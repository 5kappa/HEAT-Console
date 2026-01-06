package HEAT.model;

public class Quote {
    private int id;
    private String level;
    private String quote;

    public Quote(int id, String level, String quote) {
        this.id = id;
        this.level = level;
        this.quote = quote;
    }

    public Quote(String level, String quote) {
        this.level = level;
        this.quote = quote;
    }

    public int getId() { return id; }
    public String getLevel() { return level; }
    public String getQuote() { return quote; }
}
