package ead_checker;

import java.io.IOException;
import org.json.JSONObject;
import java.nio.file.Path;
import java.nio.file.Files;

public class Args {
    private static Args INSTANCE;
    private static String receiptNumber;
    private static String range;
    private static String recipientsEmail;
    private static String sendersEmail;
    private static String sendersPassword;

    private Args() {
        parseJson("resources/args.json");        
    }

    public synchronized static Args getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Args();
        }
        return INSTANCE;
    }
    
    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getRange() {
        return range;
    }

    public String getRecipientsEmail() {
        return recipientsEmail;
    }

    public String getSendersEmail() {
        return sendersEmail;
    }

    public String getSendersPassword() {
        return sendersPassword;
    }

    private void parseJson(String path) {
        Path filePath = Path.of(path);
        try {
            String jsonString = Files.readString(filePath);
            JSONObject jsonObj = new JSONObject(jsonString);
            receiptNumber = jsonObj.getString("receiptNumber");
            range = jsonObj.getString("range"); 
            recipientsEmail = jsonObj.getString("recipientsEmail");
            sendersEmail = jsonObj.getString("sendersEmail");
            sendersPassword = jsonObj.getString("sendersPassword");
        } catch (IOException ex) {
            ex.printStackTrace();         
            MessageSender.getInstance().sendMessage("EAD CHECKER ERROR HAPPEND", ex.getMessage()); 
            System.exit(1);        
        }        
    }
}
