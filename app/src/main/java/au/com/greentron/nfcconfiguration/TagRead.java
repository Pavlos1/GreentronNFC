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

    // See: https://stackoverflow.com/questions/13209364/convert-c-crc16-to-java-crc16
    static int crc16(final byte[] buffer) {
        int crc = 0xFFFF;

        for (int j = 0; j < buffer.length ; j++) {
            crc = ((crc  >>> 8) | (crc  << 8) )& 0xffff;
            crc ^= (buffer[j] & 0xff);//byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc;

    }

    @Override
    public void run() {
        MifareUltralight iso = MifareUltralight.get(tag);
        try {
            iso.connect();
            iso.setTimeout(5000);
            byte[] command = {0x3a, 0x00, 0x01};
            byte[] result = iso.transceive(command);
            uiHandler.obtainMessage(Constants.WORKER_EXIT_SUCCESS, bytesToHex(result)).sendToTarget();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            uiHandler.obtainMessage(Constants.WORKER_FATAL_ERROR, sw.toString()).sendToTarget();
        }
    }
}
