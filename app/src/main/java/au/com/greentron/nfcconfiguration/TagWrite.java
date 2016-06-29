package au.com.greentron.nfcconfiguration;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareUltralight;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.Arrays;

public class TagWrite extends Thread {
    Handler uiHandler;
    Handler writeWorkerHandler;
    Tag tag;
    Configuration config;
    Context context;
    boolean readThreadReturned;

    public TagWrite(Context context, Handler uiHandler, Tag tag, Configuration config) {
        super();
        this.uiHandler = uiHandler;
        this.tag = tag;
        this.config = config;
        this.context = context;
    }

    public void safeSleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // I don't care
        }
    }

    @Override
    public void run() {
        readThreadReturned = false;

        // Set up handler to check the read thread's result
        Looper.prepare();
        writeWorkerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.WORKER_EXIT_SUCCESS:
                        Configuration newConfig = (Configuration) msg.obj;
                        if (Arrays.equals(config.name, newConfig.name)
                                && (config.pan_id == newConfig.pan_id)
                                && (config.channel == newConfig.channel)) {
                            uiHandler.obtainMessage(Constants.WORKER_EXIT_SUCCESS).sendToTarget();
                        } else {
                            uiHandler.obtainMessage(Constants.WORKER_READ_BACK_ERROR, context
                                    .getResources().getString(R.string.fields_dont_match))
                                    .sendToTarget();
                        }
                        readThreadReturned = true;
                        break;
                    case Constants.WORKER_FATAL_ERROR:
                        uiHandler.obtainMessage(Constants.WORKER_READ_BACK_ERROR, msg.obj.toString())
                                .sendToTarget();
                }
            }
        };

        // Set up data to write, in the form data[page][byte]
        int i, j, offset;
        byte[][] data = new byte[0x12][4];

        // magic header
        data[0][0] = 'G';
        data[0][1] = 'T';
        data[0][2] = 'R';
        data[0][3] = 'N';

        // write 4-byte config parameters
        for (i=0; i<4; i++) {
            offset = (3 - i)*8;
            // serial number
            data[0x01][i] = (byte) ((config.serial_number & (0xffL << offset)) >> offset);
            // sensor info; not being read
            data[0x02][i] = 0;
            // pan id
            data[0x03][i] = (byte) ((config.pan_id & (0xffL << offset)) >> offset);
            //channel
            data[0x04][i] = (byte) ((config.channel & (0xffL << offset)) >> offset);
        }

        // write name
        for (i=0; i<config.name.length; i++) {
            data[0x05 + (i / 4)][i % 4] = config.name[i];
        }
        for (i=config.name.length; i<32; i++) {
            data[0x05 + (i / 4)][i % 4] = (byte) 0;
        }

        // write 4 data blocks
        for (i=0; i<config.data.length; i++) {
            for (j=0; j<4; j++) {
                offset = (3 - i)*8;
                data[0x0D + i][j] = (byte) ((config.data[i] & (0xffL << offset)) >> offset);
            }
        }

        // calculate and write checksum
        long checksum = 0;
        for (i=0; i<0x11; i++) {
            for (j=0; j<4; j++) {
                checksum = (checksum + (data[i][j] & 0xFF)) % 0xffffffffL;
            }
        }

        for (i=0; i<4; i++) {
            offset = (3 - i)*8;
            data[0x11][i] = (byte) ((checksum & (0xffL << offset)) >> offset);
        }

        // finally, interface with the tag
        MifareUltralight iso = MifareUltralight.get(tag);
        iso.setTimeout(5000);

        try {
            iso.connect();

            byte[] command = new byte[6];
            for (i=0; i<0x12; i++) {
                command[0] = (byte) 0xa2; // write
                command[1] = (byte) (0x40 + i); // page address
                // the four bytes to write
                for (j=0; j<4; j++) {
                    command[2 + j] = data[i][j];
                }
                // send the command
                byte[] result = iso.transceive(command);
                if (result[0] != (byte) 0x0A) {
                    uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR, context.getResources()
                            .getString(R.string.tag_write_failed)).sendToTarget();
                    return;
                }
            }

            // close the connection to release NFC_LOCK
            iso.close();
        } catch (NullPointerException e) {
            String error = "Wrong type of card";
            uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR, error).sendToTarget();
            return;
        } catch (TagLostException e) {
            String error="Tag was lost, try again";
            uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR, error).sendToTarget();
            return;
        } catch (Exception e) {
            uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR, e.getMessage()).sendToTarget();
            return;
        }

        // wait for the microcontroller to update the config
        safeSleep(100);

        // start read thread, and loop until it finishes
        (new TagRead(context, writeWorkerHandler, tag)).start();
        Looper.loop();
    }
}
