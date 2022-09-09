package ead_checker;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;

public class SingleCaseChecker implements Runnable{
    private long receiptNumber;
    private ConcurrentMap<Long, CaseRecord> localDataMap;
    private ConcurrentMap<Long, CaseRecord> latestDataMap;
    
    public SingleCaseChecker(long receiptNumber, ConcurrentMap<Long, CaseRecord> localDataMap, ConcurrentMap<Long, CaseRecord> latestDataMap) {
        this.receiptNumber = receiptNumber;
        this.localDataMap = localDataMap;
        this.latestDataMap = latestDataMap;
    }

    public void run() {
        String html = getHtml();
        CaseRecord latestCaseRecord = getCaseRecord(html);
        // To skip non-I-765 case
        if (latestCaseRecord != null) {
            latestDataMap.put(receiptNumber, new CaseRecord(latestCaseRecord.getTitle(), latestCaseRecord.getContent()));            
        }
    }

    private String getHtml() {
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
        } catch (IOException ex) {
            ErrorHandler.getInstance().handleError(ex);       
        }
        return html;
    }

    private CaseRecord getCaseRecord(String html) {
        Document doc = Jsoup.parse(html);
        Element caseElement = doc.select("div.rows.text-center").first();

        String content = caseElement.select("p").first().text(); 
        if (!content.contains("I-765") && !localDataMap.containsKey(receiptNumber)) { // Skip non-I-765 cases (The keyword "I-765" could be exclueded from case content in some case status)
            return null;
        }
        String title = caseElement.select("h1").first().text();  
        return new CaseRecord(title, content);
    }    
}
