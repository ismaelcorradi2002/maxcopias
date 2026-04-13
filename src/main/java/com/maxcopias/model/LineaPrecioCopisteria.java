package com.maxcopias.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class LineaPrecioCopisteria {

    private final String concept;
    private final String detail;
    private final BigDecimal amount;

    public LineaPrecioCopisteria(String concept, String detail, BigDecimal amount) {
        this.concept = concept == null ? "" : concept;
        this.detail = detail == null ? "" : detail;
        this.amount = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    public String getConcept() {
        return concept;
    }

    public String getDetail() {
        return detail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFormattedAmount() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return currencyFormat.format(amount);
    }
}
