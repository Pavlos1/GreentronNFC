package au.com.greentron.nfcconfiguration;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EditConfig extends AppCompatActivity {

    Button flashButton;
    Button clearButton;
    TextView scriptArea;
    Handler uiHandler;
    boolean readyToFlash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_config);

        readyToFlash = true;
        flashButton = (Button) findViewById(R.id.Flash);
        clearButton = (Button) findViewById(R.id.Clear);
        scriptArea = (TextView) findViewById(R.id.output);

        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                scriptArea.append(msg.obj.toString());
                switch (msg.what) {
                    case Constants.WORKER_FATAL_ERROR:
                        readyToFlash = true;
                        flashButton.setText("Flash");
                        flashButton.setBackgroundColor(Constants.COLOR_FLASH);
                        flashButton.setEnabled(true);
                        break;
                    case Constants.WORKER_DISABLE_CANCEL:
                        flashButton.setEnabled(false);
                        flashButton.setBackgroundColor(Constants.COLOR_DISABLED);
                        flashButton.setText("Please wait...");
                        break;
                    case Constants.WORKER_EXIT_SUCCESS:
                        readyToFlash = true;
                        flashButton.setText("Flash");
                        flashButton.setBackgroundColor(Constants.COLOR_FLASH);
                        flashButton.setEnabled(true);
                        break;
                    case Constants.WORKER_PRINT_MESSAGE:
                        break;
                }
            }
        };

        flashButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readyToFlash) {
                    readyToFlash = false;
                    // TODO: disable editable fields
                    flashButton.setText("Cancel");
                    flashButton.setBackgroundColor(Constants.COLOR_CANCEL);
                    //(new TagRead(uiHandler)).start();
                } else {
                    readyToFlash = true;
                    // TODO: disable editable fields
                    flashButton.setText("Flash");
                    flashButton.setBackgroundColor(Constants.COLOR_FLASH);
                }
            }
        });

        clearButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                scriptArea.setText("");
            }
        });
    }
}
