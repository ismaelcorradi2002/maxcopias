package com.maxcopias.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class EstimacionPrecioCopisteria {

    private final BigDecimal total;
    private final String breakdown;
    private final String note;
    private final int fileCount;
    private final int pageCount;
    private final List<LineaPrecioCopisteria> lines;

    public EstimacionPrecioCopisteria(BigDecimal total, String breakdown, String note, int fileCount, int pageCount) {
        this(total, breakdown, note, fileCount, pageCount, List.of());
    }

    public EstimacionPrecioCopisteria(
        BigDecimal total,
        String breakdown,
        String note,
        int fileCount,
        int pageCount,
        List<LineaPrecioCopisteria> lines
    ) {
        this.total = total == null ? BigDecimal.ZERO : total.setScale(2, RoundingMode.HALF_UP);
        this.breakdown = breakdown == null ? "" : breakdown;
        this.note = note == null ? "" : note;
        this.fileCount = Math.max(fileCount, 0);
        this.pageCount = Math.max(pageCount, 0);
        this.lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getBreakdown() {
        return breakdown;
    }

    public String getNote() {
        return note;
    }

    public int getFileCount() {
        return fileCount;
    }

    public int getPageCount() {
        return pageCount;
    }

    public List<LineaPrecioCopisteria> getLines() {
        return lines;
    }

    public String getPageCountLabel() {
        if (pageCount == 0) {
            return "0 paginas detectadas";
        }

        return pageCount == 1 ? "1 pagina detectada" : pageCount + " paginas detectadas";
    }

    public String getFormattedTotal() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return currencyFormat.format(total);
    }
}

