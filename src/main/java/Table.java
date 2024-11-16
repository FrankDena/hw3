public class Table {

    private String caption;
    private String table;
    private String references;
    private String footnotes;


    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getReferences() {
        return references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public String getFootnotes() {
        return footnotes;
    }

    public void setFootnotes(String footnotes) {
        this.footnotes = footnotes;
    }

    @Override
    public String toString() {
        return "Paper{" +
                "caption='" + caption + '\'' +
                ", table='" + table + '\'' +
                ", references='" + references + '\'' +
                ", footnotes='" + footnotes + '\'' +
                '}';
    }
}
