package au.com.greentron.nfcconfiguration;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Handler;

import java.io.PrintWriter;
import java.io.StringWriter;

class TagRead extends Thread {
    Handler uiHandler;
    Tag tag;

    public TagRead(Handler uiHandler, Tag tag) {
        super();
        this.uiHandler = uiHandler;
        this.tag = tag;
    }

    // See: https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void run() {
        MifareUltralight iso = MifareUltralight.get(tag);
        try {
            iso.connect();
            iso.setTimeout(5000);

            // Check magic header at 0x80, "GRTN"
            // Java bytes are signed, so it thinks 0x80/0x8d is negative if I apply (byte).
            // This will not affect the representation in memory, however.
            byte[] command = {0x3a, (byte) 0x80, (byte) 0x8d};
            byte[] result = iso.transceive(command);
            if ((result[0] != 'G') || (result[1] != 'T') || (result[2] != 'R')
                    || (result[3] != 'N')) {
                uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR,
                        "Wrong magic header").sendToTarget();
                return;
            }

            // The checksum is simply a summation of all the bytes,
            // including the header, mod 0xffffffff
            long checksum = 0;
            int i;
            for (i=0; i<4*0x0D; i++) {
                checksum = (checksum + (result[i] & 0xFF)) % 0xffffffffL;
            }

            long reported_checksum = 0;
            for (i=0; i<4; i++) {
                // & 0xFF coerces the bytes to be signed
                // Also (3-i) because data is transmitted MSB first
                reported_checksum += ((result[i +  4*0x0D] & 0xFF) << 8*(3-i));
            }
            if (checksum != reported_checksum) {
                uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR,
                        "Checksum incorrect\nActual: "+String.valueOf(checksum)+"\nReported: "
                                +String.valueOf(reported_checksum)+"\n").sendToTarget();
                return;
            }

            long sensor_type = 0;
            long pan_id = 0;
            long channel = 0;
            for (i=0; i<4; i++) {
                sensor_type += ((result[i + 4*0x02] & 0xFF) << (3-i)*8);
                pan_id += ((result[i + 4*0x03] & 0xFF) << (3-i)*8);
                channel += ((result[i + 4*0x04] & 0xFF) << (3-i)*8);
            }
            if (sensor_type > 65536) {
                uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR,
                        "Sensor type: field too long").sendToTarget();
                return;
            }
            if (pan_id > 65536) {
                uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR,
                        "PAN ID: field too long").sendToTarget();
                return;
            }
            if (channel > 65536) {
                uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR,
                        "Channel: field too long").sendToTarget();
                return;
            }

            // Get name (32 ASCII chars)
            int name_length = 0;
            for (i=0; i<32; i++) {
                if (result[i + 4*0x05] < 0) {
                    uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR,
                            "Name: non-ASCII characters detected").sendToTarget();
                    return;
                }
                if (result[i + 4*0x05] == 0) {
                    name_length = i + 1;
                    break;
                }
            }
            if (name_length == 0) { name_length = 32; }
            byte[] name = new byte[name_length];
            for (i=0; i<name_length; i++) {
                name[i] = result[i + 4*0x05];
            }

            Configuration obj = new Configuration();
            obj.sensor_type = sensor_type;
            obj.pan_id = pan_id;
            obj.channel = channel;
            obj.name = new String(name, "ISO-8859-1");
            uiHandler.obtainMessage(Constants.WORKER_EXIT_SUCCESS, obj).sendToTarget();

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR, sw.toString()).sendToTarget();
        }
    }
}
