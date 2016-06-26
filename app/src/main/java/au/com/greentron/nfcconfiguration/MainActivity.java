package au.com.greentron.nfcconfiguration;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TabHost;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    Handler uiHandler;
    NfcAdapter.ReaderCallback readerCallback;
    TextView dataSensorType;
    TextView configSensorType;
    TextView configName;
    TextView configPAN_ID;
    TextView configChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                TextView dataField = (TextView) findViewById(R.id.datatab_data);
                switch (msg.what) {
                    case Constants.WORKER_EXIT_SUCCESS:
                        Configuration config = (Configuration) msg.obj;
                        dataSensorType.setText(String.valueOf(config.sensor_type));
                        configSensorType.setText(String.valueOf(config.sensor_type));
                        configName.setText(config.name);
                        configPAN_ID.setText(String.valueOf(config.pan_id));
                        configChannel.setText(String.valueOf(config.channel));
                        break;
                    case Constants.WORKER_FATAL_ERROR:
                        dataField.setText("Got fatal error:\n");
                        dataField.append(msg.obj.toString());
                        dataField.append("\n");
                        break;
                }
            }
        };

        TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
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

        dataSensorType = (TextView) findViewById(R.id.datatab_sensor_type);
        configSensorType = (TextView) findViewById(R.id.configtab_sensor_type);
        configName = (TextView) findViewById(R.id.sensor_name);
        configPAN_ID = (TextView) findViewById(R.id.panid);
        configChannel = (TextView) findViewById(R.id.channel);

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
