package au.com.greentron.nfcconfiguration;

import android.content.DialogInterface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;

public class EditConfig extends AppCompatActivity {
    Toolbar actionBar;
    EditText nameEntry;
    EditText pan_idEntry;
    EditText channelEntry;
    EditText serial_numberEntry;
    Button flashButton;
    Configuration config;
    Handler uiHandler;
    AlertDialog dialog;
    EditConfig me;

    NfcAdapter nfcAdapter;
    NfcAdapter.ReaderCallback inactiveCallback;
    NfcAdapter.ReaderCallback activeCallback;

    public final int nfcflags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NFC_A;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {;
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else { return false; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_config);

        // I cannot believe I have to do this.
        me = this;

        // Handler for both the TagRead thread and NfcAdapter callbacks
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.WORKER_EXIT_SUCCESS:
                        // TODO: remove the old dialog, display the OK dialog
                        Toast.makeText(getApplicationContext(), getResources()
                                .getString(R.string.tag_write_success), Toast.LENGTH_SHORT).show();
                        nfcAdapter.enableReaderMode(me, inactiveCallback, nfcflags, new Bundle());
                        dialog.dismiss();
                        finish();
                        break;
                    case Constants.WORKER_FATAL_ERROR:
                        // TODO: remove old dialog, display failure dialog
                        Toast.makeText(getApplicationContext(), msg.obj.toString(),
                                Toast.LENGTH_LONG).show();
                        nfcAdapter.enableReaderMode(me, inactiveCallback, nfcflags, new Bundle());
                        dialog.dismiss();
                        break;
                    case Constants.WORKER_READ_BACK_ERROR:
                        // TODO: remove old dialog, display failure dialog
                        Toast.makeText(getApplicationContext(), msg.obj.toString(),
                                Toast.LENGTH_LONG).show();
                        nfcAdapter.enableReaderMode(me, inactiveCallback, nfcflags, new Bundle());
                        dialog.dismiss();
                        break;
                    // To tell the user something, via toast, before the tag is scanned
                    case Constants.WORKER_PRINT_MESSAGE:
                        Toast.makeText(getApplicationContext(), msg.obj.toString(),
                                Toast.LENGTH_LONG).show();
                        break;
                    case Constants.DIALOG_CANCEL:
                        nfcAdapter.enableReaderMode(me, inactiveCallback, nfcflags, new Bundle());
                        break;
                    case Constants.DIALOG_START:
                        nfcAdapter.enableReaderMode(me, activeCallback, nfcflags, new Bundle());
                        break;
                }
            }
        };

        // set up dialog that gets shown when the "FLASH" button is pressed
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                uiHandler.obtainMessage(Constants.DIALOG_CANCEL).sendToTarget();
            }
        });
        builder.setTitle(R.string.write_configuration);
        builder.setMessage(R.string.approach_tag_now);
        dialog = builder.create();

        // Any tags detected will trigger an alert (and not launch another app!!)
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        inactiveCallback = new NfcAdapter.ReaderCallback() {
            @Override
            public void onTagDiscovered(Tag tag) {
                uiHandler.obtainMessage(Constants.WORKER_PRINT_MESSAGE, getResources().
                        getString(R.string.press_flash_before_approaching_device)).sendToTarget();
            }
        };

        // Actually write the tag
        activeCallback = new NfcAdapter.ReaderCallback() {
            @Override
            public void onTagDiscovered(Tag tag) {
                (new TagWrite(getApplicationContext(), uiHandler, tag, config)).start();
            }
        };

        // by default, don't flash the card (this'll change when the dialog comes up)
        nfcAdapter.enableReaderMode(this, inactiveCallback, nfcflags, new Bundle());

        // Get configuration object passed from MainActivity
        String configStr = getIntent().getExtras().getString("config");
        Gson gson = new Gson();
        config = gson.fromJson(configStr, Configuration.class);

        // Enable action bar and add back button
        actionBar = (Toolbar) findViewById(R.id.edit_config_action_toolbar);
        setSupportActionBar(actionBar);
        ActionBar mainActionBar = getSupportActionBar();
        if (mainActionBar != null) {
            mainActionBar.setDisplayHomeAsUpEnabled(true);
            mainActionBar.setTitle(R.string.edit_configuration);
        }

        nameEntry = (EditText) findViewById(R.id.edit_config_sensor_name);
        pan_idEntry = (EditText) findViewById(R.id.edit_config_panid);
        channelEntry = (EditText) findViewById(R.id.edit_config_channel);
        serial_numberEntry = (EditText) findViewById(R.id.edit_config_serial_number);
        flashButton = (Button) findViewById(R.id.flash_config);

        // Fill in fields that are given in the config object
        if (config.name != null) {
            try {
                nameEntry.setText(new String(config.name, "ISO-8859-1"));
            } catch (UnsupportedEncodingException e) {
                // this should never happen
                nameEntry.setText(getResources().getString(R.string.name_parse_error));
            }
        }
        pan_idEntry.setText(String.valueOf(config.pan_id));
        channelEntry.setText(String.valueOf(config.channel));

        flashButton.setOnClickListener(new View.OnClickListener() {
            public void showError(String err) {
                Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onClick(View view) {
                // Verify input data
                if (nameEntry.getText().toString().length() == 0) {
                    showError(getResources().getString(R.string.name_required));
                    return;
                }
                if (nameEntry.getText().toString().length() > 32) {
                    showError(getResources().getString(R.string.name_too_long));
                    return;
                }
                if (pan_idEntry.getText().toString().length() == 0) {
                    showError(getResources().getString(R.string.pan_id_required));
                    return;
                }
                if (channelEntry.getText().toString().length() == 0) {
                    showError(getResources().getString(R.string.channel_required));
                    return;
                }
                if (serial_numberEntry.getText().toString().length() == 0) {
                    showError(getResources().getString(R.string.serial_number_required));
                    return;
                }
                config.pan_id = Long.parseLong(pan_idEntry.getText().toString());
                if (config.pan_id > 65535) {
                    showError(getResources().getString(R.string.pan_id_too_long));
                    return;
                }
                config.channel = Long.parseLong(channelEntry.getText().toString());
                if (config.channel > 65535) {
                    showError(getResources().getString(R.string.channel_too_long));
                    return;
                }
                config.serial_number = Long.parseLong(serial_numberEntry.getText().toString());

                try {
                    config.name = nameEntry.getText().toString().getBytes("ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                    showError(getResources().getString(R.string.name_must_be_ascii));
                    return;
                }

                // Set callback to write the tag
                uiHandler.obtainMessage(Constants.DIALOG_START).sendToTarget();
                // Throw up the Approach Tag dialog
                dialog.show();
            }
        });
    }
}
