package au.com.greentron.nfcconfiguration;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditConfig extends AppCompatActivity {
    Toolbar actionBar;
    EditText nameEntry;
    EditText pan_idEntry;
    EditText channelEntry;
    EditText serial_numberEntry;
    Button flashButton;

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
                long pan_id = Long.parseLong(pan_idEntry.getText().toString());
                long channel = Long.parseLong(channelEntry.getText().toString());
                long serial_number = Long.parseLong(serial_numberEntry.getText().toString());
                String name = nameEntry.getText().toString();
            }
        });
    }
}
