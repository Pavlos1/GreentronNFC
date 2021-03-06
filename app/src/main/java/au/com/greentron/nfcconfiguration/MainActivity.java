package au.com.greentron.nfcconfiguration;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    Handler uiHandler;
    NfcAdapter.ReaderCallback readerCallback;
    TextView dataSensorType;
    TextView configSensorType;
    TextView configName;
    TextView configPAN_ID;
    TextView configChannel;
    Button editConfig;
    Toolbar actionBar;
    TabHost tabHost;
    Configuration config;
    TextView dataField;

    private boolean updateUIElements() {
        if (config == null) {
            return false;
        }
        dataSensorType.setText(String.valueOf(config.sensor_type));
        configSensorType.setText(String.valueOf(config.sensor_type));
        try {
            configName.setText(new String(config.name, "ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            // this should never happen
            config = null;
            Toast.makeText(getApplicationContext(), getResources()
                    .getString(R.string.something_went_wrong), Toast.LENGTH_LONG).show();
            return false;
        }
        configPAN_ID.setText(String.valueOf(config.pan_id));
        configChannel.setText(String.valueOf(config.channel));

        dataField.setText(getResources().getString(R.string.got_data));
        for (int i=0; i<config.data.length; i++) {
            dataField.append(getResources().getString(R.string.page));
            dataField.append(String.valueOf(i));
            dataField.append(": ");
            dataField.append(String.valueOf(config.data[i]));
            dataField.append("\n");
        }

        editConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), EditConfig.class);
                Gson gson = new Gson();
                intent.putExtra("config", gson.toJson(config));
                startActivity(intent);
            }
        });

        editConfig.setEnabled(true);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // The action bar was replaced to allow editing, so this is necessary
        actionBar = (Toolbar) findViewById(R.id.action_toolbar);
        setSupportActionBar(actionBar);

        // Setup tabs
        tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();

        // Data Tab
        TabHost.TabSpec spec = tabHost.newTabSpec(getResources().getString(R.string.data_tab_label));
        spec.setContent(R.id.datatab);
        spec.setIndicator(getResources().getString(R.string.data_tab_label));
        tabHost.addTab(spec);

        //Config Tab
        spec = tabHost.newTabSpec(getResources().getString(R.string.config_tab_label));
        spec.setContent(R.id.configtab);
        spec.setIndicator(getResources().getString(R.string.config_tab_label));
        tabHost.addTab(spec);

        // Help Tab
        spec = tabHost.newTabSpec(getResources().getString(R.string.help_tab_label));
        spec.setContent(R.id.helptab);
        spec.setIndicator(getResources().getString(R.string.help_tab_label));
        tabHost.addTab(spec);

        // Set tab label size
        for(int i=0; i<tabHost.getTabWidget().getChildCount(); i++)
        {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i)
                    .findViewById(android.R.id.title);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }

        // Handles to UI elements
        dataSensorType = (TextView) findViewById(R.id.datatab_sensor_type);
        configSensorType = (TextView) findViewById(R.id.configtab_sensor_type);
        configName = (TextView) findViewById(R.id.sensor_name);
        configPAN_ID = (TextView) findViewById(R.id.panid);
        configChannel = (TextView) findViewById(R.id.channel);
        editConfig = (Button) findViewById(R.id.enter_config_setup);
        dataField = (TextView) findViewById(R.id.datatab_data);

        // Get config object if it's been saved for us, and update UI elements
        tabHost.setCurrentTab(2);
        if (savedInstanceState != null) {
            Gson gson = new Gson();
            config = gson.fromJson(savedInstanceState.getString("config"), Configuration.class);
            updateUIElements();
            tabHost.setCurrentTab(savedInstanceState.getInt("current_tab"));
        }

        // Receive result from TagRead output
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // If user is currently on help tab, direct them to data tab
                switch (msg.what) {
                    case Constants.WORKER_EXIT_SUCCESS:
                        if (tabHost.getCurrentTab() == 2) { tabHost.setCurrentTab(0); }
                        config = (Configuration) msg.obj;
                        if(updateUIElements()) {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.tag_read_success),
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constants.WORKER_FATAL_ERROR:
                        Toast.makeText(getApplicationContext(), msg.obj.toString(),
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };

        // Set up NFC callback, handled by TagRead
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        readerCallback = new NfcAdapter.ReaderCallback() {
            @Override
            public void onTagDiscovered(Tag tag) {
                (new TagRead(getApplicationContext(), uiHandler, tag)).start();
            }
        };
        int nfcflags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NFC_A;
        nfcAdapter.enableReaderMode(this, readerCallback, nfcflags, new Bundle());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save config object
        Gson gson = new Gson();
        savedInstanceState.putString("config", gson.toJson(config));
        savedInstanceState.putInt("current_tab", tabHost.getCurrentTab());
        super.onSaveInstanceState(savedInstanceState);
    }
}
