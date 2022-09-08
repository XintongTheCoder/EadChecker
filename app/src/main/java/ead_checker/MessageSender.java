package ead_checker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.PasswordAuthentication;

public class MessageSender {
   private static MessageSender INSTANCE;
   
   private MessageSender() {}

   public synchronized static MessageSender getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MessageSender();
        }
        return INSTANCE;
   }
    
    public void sendMessage(String subject, String message) {
        Args ARGS = Args.getInstance();
        String to = ARGS.getRecipientsEmail();
        String from = ARGS.getSendersEmail();
        String host = "smtp.gmail.com";
        Properties properties = System.getProperties();
        // Setup mail server
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        // Get the Session object.// and pass username and password
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(from, ARGS.getSendersPassword());

            }

        });

        // Used to debug SMTP issues
        // session.setDebug(true);

        try {
            // Create a default MimeMessage object
            MimeMessage mimeMessage = new MimeMessage(session);
            // Set From: header field of the header
            mimeMessage.setFrom(new InternetAddress(from));
            // Set To: header field of the header
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            // Set Subject: header field
            mimeMessage.setSubject(subject);
            // Set the email content
            mimeMessage.setText(message);
            // Send message
            Transport.send(mimeMessage);
            System.out.println("Sent message on: " + getCurrentDateAndTime());
        } catch (MessagingException mex) {
            mex.printStackTrace();
            sendMessage("EAD CHECKER ERROR HAPPEND", mex.getMessage());         
            System.exit(1);        
        }
    }

    private String getCurrentDateAndTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }
}
