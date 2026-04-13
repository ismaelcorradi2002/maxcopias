package com.maxcopias.dto;

/**
 * Record respuesta JSON para vista previa de precio copistería.
 */
import com.maxcopias.model.EstimacionPrecioCopisteria;
import com.maxcopias.model.LineaPrecioCopisteria;
import java.util.List;

public record RespuestaVistaPreviaPrecioCopisteria(
    String formattedTotal,
    String breakdown,
    String note,
    int fileCount,
    int pageCount,
    String pageCountLabel,
    List<LineaPrecioVistaPreviaCopisteria> lines
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
            estimate.getPageCountLabel(),
            estimate.getLines().stream()
                .map(LineaPrecioVistaPreviaCopisteria::from)
                .toList()
        );
    }

    public record LineaPrecioVistaPreviaCopisteria(
        String concept,
        String detail,
        String formattedAmount
    ) {
        public static LineaPrecioVistaPreviaCopisteria from(LineaPrecioCopisteria line) {
            return new LineaPrecioVistaPreviaCopisteria(
                line.getConcept(),
                line.getDetail(),
                line.getFormattedAmount()
            );
        }
    }
}

