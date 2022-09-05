package ead_checker;

import java.io.IOException;
import java.util.Set;

import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class EadChecker {
    private static final Set<String> MONTHS = Set.of("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
    
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

    public void printCaseStatus(long startNumber, long endNumber) {
        long receiptNumber = startNumber;
        while (receiptNumber <= endNumber) {
            String html = getHtml(receiptNumber);
            String caseInfo = getCaseInfo(html, receiptNumber);
            if (caseInfo.length() > 0) {
                System.out.println(caseInfo.toString());
            }            
            receiptNumber++;
        }        
    }

    public String getCaseInfo(String html, long receiptNumber) {
        Document doc = Jsoup.parse(html);
        Element content = doc.select("div.rows.text-center").first();
        String caseInfo = content.select("p").first().text(); 
        if (!caseInfo.contains("I-765")) { // Skip non-I-765 cases
            return "";
        }
        String caseStatus = content.select("h1").first().text();  
        String caseDate = "";
        // If the caseInfo begins with a date, then the caseDate is this date;
        for (String month : MONTHS) {
            if (caseInfo.contains(month)) {
                int caseDateEndIndex = caseInfo.indexOf(", 202") + 6; // ", 2022"
                caseDate = caseInfo.substring(caseInfo.indexOf(month), caseDateEndIndex);
                break;
            }
        }
        
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("WAC")
                .append(receiptNumber)
                .append(": ")
                .append(caseStatus)
                .append(": ")
                .append(caseDate);
        return stringBuilder.toString();
    }

    public void printCaseStatus(String baseNumber, String range) {
        int indexRange = Integer.valueOf(range);
        long startIndex = Long.valueOf(baseNumber.substring(3)) - indexRange / 2;
        long endIndex = startIndex + indexRange / 2;
        printCaseStatus(startIndex, endIndex);
    }
}
