package io.github.nahomgh.portfolio.entity;

public enum TransactionType {
    BUY("BUY"),
    SELL("SELL");

    private String value;

    TransactionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TransactionType{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }
}

