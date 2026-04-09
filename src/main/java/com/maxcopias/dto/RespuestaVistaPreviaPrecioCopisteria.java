package com.maxcopias.dto;

import com.maxcopias.model.EstimacionPrecioCopisteria;

public record RespuestaVistaPreviaPrecioCopisteria(
    String formattedTotal,
    String breakdown,
    String note,
    int fileCount,
    int pageCount,
    String pageCountLabel
) {

    public static RespuestaVistaPreviaPrecioCopisteria from(EstimacionPrecioCopisteria estimate) {
        return new RespuestaVistaPreviaPrecioCopisteria(
            estimate.getFormattedTotal(),
            estimate.getBreakdown(),
            estimate.getNote(),
            estimate.getFileCount(),
            estimate.getPageCount(),
            estimate.getPageCountLabel()
        );
    }
}

