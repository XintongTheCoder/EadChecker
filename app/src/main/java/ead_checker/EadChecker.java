package ead_checker;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.PasswordAuthentication;

public class EadChecker {
    private String recipientsEmail;
    private String sendersEmail;
    private String sendersPassword;
    public StringBuilder messageStringBuilder = new StringBuilder();
    public String getHtml(long receiptNumber) {
        String html = "";
        String appReceiptNum = "WAC" + receiptNumber;
        try {
            html = Request
                    .post("https://egov.uscis.gov/casestatus/mycasestatus.do")
                    .bodyForm(Form.form()
                            .add("appReceiptNum", appReceiptNum)
                            .add("caseStatusSearchBtn", "CHECK STATUS")
                            .build())
                    .execute()
                    .returnContent()
                    .asString();
        } catch (IOException e) {
            sendMessage("Error happened.", e.getMessage());         
        }
        return html;
    }

    public void getCaseStatus(Long receiptNumber, CaseRecord localCaseRecord, CaseRecord latestCaseRecord) {    
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("WAC")
                .append(receiptNumber)
                .append(": ")
                .append("\n")
                .append("WAS: ");
        if (localCaseRecord == null) {
            stringBuilder
                .append("N/A");
        } else {
            stringBuilder
                .append(localCaseRecord.getTitle())
                .append(": ")
                .append(localCaseRecord.getContent());
        }
        stringBuilder
                .append("\n")
                .append("IS: ")
                .append(latestCaseRecord.getTitle())
                .append(" - ")
                .append(latestCaseRecord.getContent())
                .append("\n")
                .append("\n");
                
        messageStringBuilder.append(stringBuilder);          
    }

    public void checkCaseStatus(long startNumber, long endNumber) {
        Map<Long, CaseRecord> localDataMap = readLocalData();
        Map<Long, CaseRecord> latestDataMap = new HashMap<>();
        boolean hasUpdates = false;
        long receiptNumber = startNumber;
        while (receiptNumber <= endNumber) {
            String html = getHtml(receiptNumber);
            CaseRecord latestCaseRecord = getCaseRecord(html, receiptNumber, localDataMap);
            // To skip non-I-765 case
            if (latestCaseRecord != null) {
                latestDataMap.put(receiptNumber, new CaseRecord(latestCaseRecord.getTitle(), latestCaseRecord.getContent()));
                // To print the updated cases
                if (!localDataMap.containsKey(receiptNumber) || !localDataMap.get(receiptNumber).getTitle().equals(latestCaseRecord.getTitle())) {
                    hasUpdates = true;
                    getCaseStatus(receiptNumber, localDataMap.getOrDefault(receiptNumber, null), latestCaseRecord);
                }
            }
            receiptNumber++;
        }
        // If there are any updates, overwrite the local-case-data;
        if (hasUpdates) {
            writeLocalData(latestDataMap);
            // Send message
            String subject = "UPDATES ON EAD STATUS!"; 
            sendMessage(subject, messageStringBuilder.toString());
        }
    }

    public Map<Long, CaseRecord> readLocalData() {
        Map<Long, CaseRecord> localDataMap = null;
        String filename = "local-case-data.ser";
        // Deserialization
        try {
            // Reading the Case record from local file
            FileInputStream file = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(file);
            localDataMap = (Map<Long, CaseRecord>) in.readObject();
            in.close();
            file.close();
        } catch(IOException ex) {
            return new HashMap<>();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return localDataMap;
    }

    public void writeLocalData(Map<Long, CaseRecord> latestDataMap) {
        String filename = "local-case-data.ser";
        // Serialization
        try {
            FileOutputStream file = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(latestDataMap);
            out.close();
            file.close();
        } catch (IOException ex) {
            System.out.println("IOException is caught");
        }
    }

    public CaseRecord getCaseRecord(String html, long receiptNumber, Map<Long, CaseRecord> localDataMap) {
        Document doc = Jsoup.parse(html);
        Element caseElement = doc.select("div.rows.text-center").first();

        String content = caseElement.select("p").first().text(); 
        if (!content.contains("I-765") && !localDataMap.containsKey(receiptNumber)) { // Skip non-I-765 cases (The keyword "I-765" could be exclueded from case content in some case status)
            return null;
        }
        String title = caseElement.select("h1").first().text();  
        return new CaseRecord(title, content);
    }

    public void checkCaseStatus(String baseNumber, String range, String recipientsEmail, String sendersEmail, String sendersPassword) {
        this.recipientsEmail = recipientsEmail;
        this.sendersEmail = sendersEmail;
        this.sendersPassword = sendersPassword;
        int indexRange = Integer.valueOf(range);
        long startIndex = Long.valueOf(baseNumber.substring(3)) - indexRange / 2;
        long endIndex = startIndex + indexRange / 2;
        checkCaseStatus(startIndex, endIndex);
    }

    public void sendMessage(String subject, String message) {
        String to = recipientsEmail;
        String from = sendersEmail;
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

                return new PasswordAuthentication(sendersEmail, sendersPassword);

            }

        });

        // Used to debug SMTP issues
        session.setDebug(true);

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
        }
    }

    public String getCurrentDateAndTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }
}
