package ead_checker;
import java.io.Serializable;

public class CaseRecord implements Serializable{
    private String title;
    private String content;

    public CaseRecord(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}
