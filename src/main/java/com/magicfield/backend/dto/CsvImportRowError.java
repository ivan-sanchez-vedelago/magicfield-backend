package com.magicfield.backend.dto;

public class CsvImportRowError {

    private int row;
    private String cardName;
    private String reason;

    public CsvImportRowError() {
    }

    public CsvImportRowError(int row, String cardName, String reason) {
        this.row = row;
        this.cardName = cardName;
        this.reason = reason;
    }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
