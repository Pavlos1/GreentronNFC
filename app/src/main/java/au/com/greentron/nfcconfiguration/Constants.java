package au.com.greentron.nfcconfiguration;

public class Constants {
    // Status codes for communicating with background process
    public static final int WORKER_FATAL_ERROR = 1;
    public static final int WORKER_DISABLE_CANCEL = 2;
    public static final int WORKER_PRINT_MESSAGE = 3;
    public static final int WORKER_EXIT_SUCCESS = 4;

    // Colours
    public static final int COLOR_CANCEL = 0xffff0000;
    public static final int COLOR_FLASH = 0xff00ff00;
    public static final int COLOR_DISABLED = 0xff454545;
}
