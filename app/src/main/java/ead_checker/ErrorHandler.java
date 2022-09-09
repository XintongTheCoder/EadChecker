package ead_checker;

public class ErrorHandler {
    private static ErrorHandler INSTANCE;
    
    private ErrorHandler() {}

    public synchronized static ErrorHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ErrorHandler();
        }
        return INSTANCE;
    }

    public void handleError(Exception ex) {
        ex.printStackTrace();
        MessageSender.getInstance().sendMessage("EAD CHECKER ERROR HAPPEND", ex.getMessage());         
        System.exit(1);
    }
}
