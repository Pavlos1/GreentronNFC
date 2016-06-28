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

import com.google.gson.Gson;

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
        TabHost.TabSpec spec = tabHost.newTabSpec("Data");
        spec.setContent(R.id.datatab);
        spec.setIndicator("Data");
        tabHost.addTab(spec);

        //Config Tab
        spec = tabHost.newTabSpec("Config");
        spec.setContent(R.id.configtab);
        spec.setIndicator("Config");
        tabHost.addTab(spec);

        // Help Tab
        spec = tabHost.newTabSpec("Help");
        spec.setContent(R.id.helptab);
        spec.setIndicator("Help");
        tabHost.addTab(spec);

        // Set tab label size
        for(int i=0; i<tabHost.getTabWidget().getChildCount(); i++)
        {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }

        // By deafult, open help tab
        tabHost.setCurrentTab(2);

        // Handles to UI elements
        dataSensorType = (TextView) findViewById(R.id.datatab_sensor_type);
        configSensorType = (TextView) findViewById(R.id.configtab_sensor_type);
        configName = (TextView) findViewById(R.id.sensor_name);
        configPAN_ID = (TextView) findViewById(R.id.panid);
        configChannel = (TextView) findViewById(R.id.channel);
        editConfig = (Button) findViewById(R.id.enter_config_setup);

        // Receive result from TagRead output
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                TextView dataField = (TextView) findViewById(R.id.datatab_data);

                // If user is currently on help tab, direct them to data tab
                if (tabHost.getCurrentTab() == 2) { tabHost.setCurrentTab(0); }

                switch (msg.what) {
                    case Constants.WORKER_EXIT_SUCCESS:
                        config = (Configuration) msg.obj;
                        dataSensorType.setText(String.valueOf(config.sensor_type));
                        configSensorType.setText(String.valueOf(config.sensor_type));
                        configName.setText(config.name);
                        configPAN_ID.setText(String.valueOf(config.pan_id));
                        configChannel.setText(String.valueOf(config.channel));

                        dataField.setText("Got data:\n");
                        for (int i=0; i<config.data.length; i++) {
                            dataField.append("Page ");
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
                        break;
                    case Constants.WORKER_FATAL_ERROR:
                        dataField.setText("Got fatal error:\n");
                        dataField.append(msg.obj.toString());
                        dataField.append("\n");
                        break;
                }
            }
        };

        // Set up NFC callback, handled by TagRead
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        readerCallback = new NfcAdapter.ReaderCallback() {
            @Override
            public void onTagDiscovered(Tag tag) {
                (new TagRead(uiHandler, tag)).start();
            }
        };
        int nfcflags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NFC_A;
        nfcAdapter.enableReaderMode(this, readerCallback, nfcflags, new Bundle());
    }
}
