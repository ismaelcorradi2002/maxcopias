package com.maxcopias.dto;

/**
 * Record respuesta JSON para vista previa de precio copistería.
 */
import com.maxcopias.model.EstimacionPrecioCopisteria;

public record RespuestaVistaPreviaPrecioCopisteria(
    String formattedTotal,
    String breakdown,
    String note,
    int fileCount,
    int pageCount,
    String pageCountLabel
) {

    /**
     * Convierte estimación de precio a record JSON.
     */
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

