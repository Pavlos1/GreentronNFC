package au.com.greentron.nfcconfiguration;

public class Constants {
    // Status codes for communicating with background process
    public static final int WORKER_FATAL_ERROR = 1;
    public static final int WORKER_DISABLE_CANCEL = 2;
    public static final int WORKER_PRINT_MESSAGE = 3;
    public static final int WORKER_EXIT_SUCCESS = 4;
    public static final int WORKER_READ_BACK_ERROR = 5;

    public static final int DIALOG_CANCEL = 6;
    public static final int DIALOG_START = 7;
}
