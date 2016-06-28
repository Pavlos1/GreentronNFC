package au.com.greentron.nfcconfiguration;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

public class EditConfig extends AppCompatActivity {
    Toolbar actionBar;
    EditText nameEntry;
    EditText pan_idEntry;
    EditText channelEntry;
    EditText serial_numberEntry;
    Button flashButton;
    Configuration config;
    Handler uiHandler;

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

        // Mainly for creating toasts
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.WORKER_PRINT_MESSAGE:
                        Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG).show();
                }
            }
        };

        // Any tags detected will trigger an alert (and not launch another app!!)
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        NfcAdapter.ReaderCallback readerCallback = new NfcAdapter.ReaderCallback() {
            @Override
            public void onTagDiscovered(Tag tag) {
                uiHandler.obtainMessage(Constants.WORKER_PRINT_MESSAGE,
                        "Press \"FLASH\" before approaching tag").sendToTarget();
            }
        };
        int nfcflags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NFC_A;
        nfcAdapter.enableReaderMode(this, readerCallback, nfcflags, new Bundle());

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
            mainActionBar.setTitle("Edit Configuration");
        }

        nameEntry = (EditText) findViewById(R.id.edit_config_sensor_name);
        pan_idEntry = (EditText) findViewById(R.id.edit_config_panid);
        channelEntry = (EditText) findViewById(R.id.edit_config_channel);
        serial_numberEntry = (EditText) findViewById(R.id.edit_config_serial_number);
        flashButton = (Button) findViewById(R.id.flash_config);

        // Fill in fields that are given in the config object
        if (config.name != null) { nameEntry.setText(config.name); }
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
                    showError("A name is required");
                    return;
                }
                if (nameEntry.getText().toString().length() > 32) {
                    showError("Name cannot exceed 32 characters");
                }
                if (pan_idEntry.getText().toString().length() == 0) {
                    showError("A PAN ID is required");
                    return;
                }
                if (channelEntry.getText().toString().length() == 0) {
                    showError("A channel is required");
                    return;
                }
                if (serial_numberEntry.getText().toString().length() == 0) {
                    showError("A serial number is required");
                    return;
                }
                config.pan_id = Long.parseLong(pan_idEntry.getText().toString());
                if (config.pan_id > 65535) {
                    showError("PAN ID cannot exceed 65535");
                    return;
                }
                config.channel = Long.parseLong(channelEntry.getText().toString());
                if (config.channel > 65535) {
                    showError("Channel cannot exceed 65535");
                    return;
                }
                config.serial_number = Long.parseLong(serial_numberEntry.getText().toString());
                config.name = nameEntry.getText().toString();
            }
        });
    }
}
