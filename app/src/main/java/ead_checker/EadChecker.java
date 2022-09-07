package ead_checker;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class EadChecker {
    
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
            System.out.println("Error happened.");
            e.printStackTrace();
        }
        return html;
    }

    public void printCaseStatus(Long receiptNumber, CaseRecord localCaseRecord, CaseRecord latestCaseRecord) {        
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
                .append(latestCaseRecord.getContent());
                
        System.out.println(stringBuilder.toString());          
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
                    printCaseStatus(receiptNumber, localDataMap.getOrDefault(receiptNumber, null), latestCaseRecord);
                }
            }
            receiptNumber++;
        }
        // If there are any updates, overwrite the local-case-data;
        if (hasUpdates) {
            writeLocalData(latestDataMap);
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

    public void checkCaseStatus(String baseNumber, String range) {
        int indexRange = Integer.valueOf(range);
        long startIndex = Long.valueOf(baseNumber.substring(3)) - indexRange / 2;
        long endIndex = startIndex + indexRange / 2;
        checkCaseStatus(startIndex, endIndex);
    }
}
