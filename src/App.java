import org.apache.hc.client5.http.fluent.Request;

import java.io.IOException;

import org.apache.hc.client5.http.fluent.Form;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.printCaseStatus(2190088100L, -500, 100);
    }

    public String getContent(long receiptNumber) {
        String content = "";
        String appReceiptNum = "WAC" + receiptNumber;
        try {
            content = Request
                    .post("https://egov.uscis.gov/casestatus/mycasestatus.do").bodyForm(Form.form()
                            .add("appReceiptNum", appReceiptNum).add("caseStatusSearchBtn", "CHECK STATUS").build())
                    .execute().returnContent().asString();
        } catch (IOException e) {
            System.out.println("Error happened.");
            e.printStackTrace();
        }
        return content;
    }

    public void printCaseStatus(long startNumber, long endNumber) {
        long receiptNumber = startNumber;
        while (receiptNumber < endNumber) {
            String content = getContent(receiptNumber);
            if (content.contains("I-765")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("WAC").append(receiptNumber).append(": ").append(getCaseStatus(content))
                        .append(": ").append(getCaseDate(content));

                System.out.println(stringBuilder.toString());
            }
            receiptNumber++;
        }
    }

    public void printCaseStatus(long baseNumber, int shiftLower, int range) {
        long startIndex = baseNumber - shiftLower;
        long endIndex = startIndex + range;
        printCaseStatus(startIndex, endIndex);
    }

    public String getCaseStatus(String html) {
        int startIndex = html.indexOf("<h1>");
        int endIndex = html.indexOf("</h1>", startIndex);
        int h1TagLength = 4;
        return html.substring(startIndex + h1TagLength, endIndex);
    }

    public String getCaseDate(String html) {
        int h1EndIndex = html.indexOf("</h1>");
        int startIndex = html.indexOf("<p>", h1EndIndex);
        int endIndex = html.indexOf(", 202", startIndex) + 6;
        int pTagLength = 3;

        return html.substring(startIndex + pTagLength, endIndex);
    }
}
