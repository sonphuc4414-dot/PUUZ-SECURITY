package com.puuz.mapshield.money;

public enum MoneyHistoryStyle {
    TEXT("Chỉ chữ"),
    CARD("Thẻ mềm"),
    GLASS("Kính nhẹ"),
    OUTLINE("Viền thanh"),
    PILL("Viên thuốc"),
    COMPACT("Gọn gàng"),
    SOFT("Mềm dịu"),
    MINIMAL("Tối giản"),
    PANEL("Bảng nổi");

    private final String label;
    MoneyHistoryStyle(String label) { this.label = label; }
    public String label() { return label; }
    public static MoneyHistoryStyle from(String value) {
        if (value == null) return CARD;
        try { return valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return CARD; }
    }
}
