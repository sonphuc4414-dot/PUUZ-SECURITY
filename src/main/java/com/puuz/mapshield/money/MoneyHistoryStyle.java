package com.puuz.mapshield.money;

public enum MoneyHistoryStyle {
    TEXT("Chỉ text"), CARD("Card mềm"), GLASS("Glass nhẹ"), OUTLINE("Viền thanh"), PILL("Pill gọn");

    private final String label;
    MoneyHistoryStyle(String label) { this.label = label; }
    public String label() { return label; }
    public static MoneyHistoryStyle from(String value) {
        if (value == null) return CARD;
        try { return valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return CARD; }
    }
}
