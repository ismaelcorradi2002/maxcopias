package com.maxcopias.service;

import com.maxcopias.dto.FormularioPedidoCopisteria;
import com.maxcopias.model.ModoColor;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.EstimacionPrecioCopisteria;
import com.maxcopias.model.TipoTrabajo;
import com.maxcopias.model.TamanoPapel;
import com.maxcopias.model.CaraImpresion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ServicioPrecioCopisteria {

    private static final BigDecimal IMPRESION_BW = amount("0.06");
    private static final BigDecimal IMPRESION_COLOR = amount("0.45");
    private static final BigDecimal FOTOCOPIA_BW = amount("0.05");
    private static final BigDecimal FOTOCOPIA_COLOR = amount("0.18");
    private static final BigDecimal PUBLICIDAD_BASE = amount("19.00");
    private static final BigDecimal ENCUADERNACION_BASE = amount("3.50");
    private static final BigDecimal PLASTIFICADO_BASE = amount("1.80");
    private static final BigDecimal DISENO_BASE = amount("25.00");
    private static final BigDecimal PERSONALIZACION_BASE = amount("9.90");
    private static final BigDecimal SERVICIOS_ADICIONALES_BASE = amount("0.50");
    private static final BigDecimal OTRO_BASE = amount("12.00");
    private static final BigDecimal URGENT_SUPPLEMENT = amount("2.00");
    private static final BigDecimal NOTE_MINIMUM = amount("0.00");

    public EstimacionPrecioCopisteria calculate(FormularioPedidoCopisteria form, int uploadedFiles, int totalPages) {
        if (form == null) {
            return emptyEstimate();
        }

        return calculateInternal(new PricingInput(
            form.getTipoTrabajo(),
            form.getCopies(),
            form.getModoColor(),
            form.getCaraImpresion(),
            form.getPaperSize(),
            form.getObservations(),
            uploadedFiles,
            totalPages
        ));
    }

    public EstimacionPrecioCopisteria calculate(PedidoCopisteria order) {
        if (order == null) {
            return emptyEstimate();
        }

        return calculateInternal(new PricingInput(
            order.getTipoTrabajo(),
            order.getCopies(),
            order.getModoColor(),
            order.getCaraImpresion(),
            order.getPaperSize(),
            order.getObservations(),
            order.getFileCount(),
            order.getTotalPageCount()
        ));
    }

    private EstimacionPrecioCopisteria calculateInternal(PricingInput input) {
        if (input.jobType() == null) {
            return emptyEstimate();
        }

        return switch (input.jobType()) {
            case IMPRESION -> calculatePrintLike(input, IMPRESION_BW, IMPRESION_COLOR, "impresion");
            case FOTOCOPIAS -> calculatePrintLike(input, FOTOCOPIA_BW, FOTOCOPIA_COLOR, "fotocopias");
            case PUBLICIDAD_IMPRENTA -> calculatePublicidad(input);
            case ENCUADERNACION -> calculateSimpleBase(
                input,
                ENCUADERNACION_BASE,
                amount("1.10"),
                "Base de encuadernacion + acabado por archivo"
            );
            case PLASTIFICADO -> calculateSimpleBase(
                input,
                PLASTIFICADO_BASE,
                amount("0.90"),
                "Plastificado calculado por documento adjunto"
            );
            case DISENO_GRAFICO -> calculateSimpleBase(
                input,
                DISENO_BASE,
                amount("4.50"),
                "Base de diseno + material o referencias adjuntas"
            );
            case PERSONALIZACION -> calculateSimpleBase(
                input,
                PERSONALIZACION_BASE,
                amount("3.50"),
                "Personalizacion orientativa segun unidades o artes adjuntas"
            );
            case SERVICIOS_ADICIONALES -> calculateSimpleBase(
                input,
                SERVICIOS_ADICIONALES_BASE,
                amount("0.75"),
                "Servicio adicional calculado por documento o gestion"
            );
            case OTRO -> calculateSimpleBase(
                input,
                OTRO_BASE,
                amount("2.50"),
                "Referencia base para encargos especiales"
            );
        };
    }

    private EstimacionPrecioCopisteria calculatePrintLike(
        PricingInput input,
        BigDecimal bwUnitPrice,
        BigDecimal colorUnitPrice,
        String jobLabel
    ) {
        int copies = normalizeCopies(input.copies());
        int pages = pricedPages(input.pageCount());
        int units = pages * copies;

        ModoColor colorMode = input.colorMode() != null ? input.colorMode() : ModoColor.BLACK_AND_WHITE;
        CaraImpresion printSide = input.printSide() != null ? input.printSide() : CaraImpresion.ONE_SIDED;
        TamanoPapel paperSize = input.paperSize() != null ? input.paperSize() : TamanoPapel.A4;

        BigDecimal unitPrice = colorMode == ModoColor.COLOR ? colorUnitPrice : bwUnitPrice;
        unitPrice = unitPrice.multiply(sizeMultiplier(paperSize)).multiply(sideMultiplier(printSide));

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(units));
        String breakdown = pageReference(input.pageCount(), input.fileCount())
            + " x "
            + copies
            + " copia(s) • "
            + colorMode.getLabel()
            + " • "
            + paperSize.getLabel()
            + " • "
            + printSide.getLabel();

        return buildEstimate(total, breakdown, input, jobLabel);
    }

    private EstimacionPrecioCopisteria calculatePublicidad(PricingInput input) {
        int copies = normalizeCopies(input.copies());
        int pages = pricedPages(input.pageCount());
        int units = pages * copies;

        ModoColor colorMode = input.colorMode() != null ? input.colorMode() : ModoColor.COLOR;
        CaraImpresion printSide = input.printSide() != null ? input.printSide() : CaraImpresion.ONE_SIDED;
        TamanoPapel paperSize = input.paperSize() != null ? input.paperSize() : TamanoPapel.A4;

        BigDecimal variableUnit = colorMode == ModoColor.COLOR ? amount("0.22") : amount("0.12");
        variableUnit = variableUnit.multiply(sizeMultiplierForCampaign(paperSize)).multiply(sideMultiplierForCampaign(printSide));

        BigDecimal total = PUBLICIDAD_BASE.add(variableUnit.multiply(BigDecimal.valueOf(units)));
        String breakdown = "Base de imprenta + tirada estimada de "
            + pageReference(input.pageCount(), input.fileCount())
            + " x "
            + copies
            + " unidad(es) • "
            + colorMode.getLabel()
            + " • "
            + paperSize.getLabel();

        return buildEstimate(total, breakdown, input, "publicidad e imprenta");
    }

    private EstimacionPrecioCopisteria calculateSimpleBase(
        PricingInput input,
        BigDecimal basePrice,
        BigDecimal extraPerFile,
        String breakdownPrefix
    ) {
        int pricedFiles = pricedFiles(input.fileCount());
        BigDecimal total = basePrice.add(extraPerFile.multiply(BigDecimal.valueOf(pricedFiles - 1L)));
        String breakdown = breakdownPrefix + " • " + fileReference(input.fileCount());
        return buildEstimate(total, breakdown, input, input.jobType().getLabel().toLowerCase(Locale.ROOT));
    }

    private EstimacionPrecioCopisteria buildEstimate(
        BigDecimal rawTotal,
        String breakdown,
        PricingInput input,
        String jobLabel
    ) {
        BigDecimal total = rawTotal.max(NOTE_MINIMUM);

        if (hasUrgentRequest(input.observations())) {
            total = total.add(URGENT_SUPPLEMENT);
            breakdown = breakdown + " + suplemento urgente";
        }

        String note = input.fileCount() > 0
            ? "Precio orientativo calculado con el servicio, las paginas detectadas y la configuracion elegida. El importe final puede ajustarse al revisar acabados especiales."
            : "Precio orientativo base para " + jobLabel + ". Sube tus archivos en PDF, JPG o PNG para afinar mejor el importe antes de guardar el pedido.";

        return new EstimacionPrecioCopisteria(
            total.setScale(2, RoundingMode.HALF_UP),
            breakdown,
            note,
            input.fileCount(),
            input.pageCount()
        );
    }

    private EstimacionPrecioCopisteria emptyEstimate() {
        return new EstimacionPrecioCopisteria(
            BigDecimal.ZERO,
            "Selecciona un servicio para ver el precio orientativo del pedido.",
            "El importe se calcula automaticamente al combinar tipo de trabajo, archivos y configuracion.",
            0,
            0
        );
    }

    private int pricedFiles(int fileCount) {
        return Math.max(fileCount, 1);
    }

    private int pricedPages(int pageCount) {
        return Math.max(pageCount, 1);
    }

    private int normalizeCopies(Integer copies) {
        return copies == null || copies < 1 ? 1 : copies;
    }

    private BigDecimal sizeMultiplier(TamanoPapel paperSize) {
        return paperSize == TamanoPapel.A3 ? amount("1.85") : amount("1.00");
    }

    private BigDecimal sideMultiplier(CaraImpresion printSide) {
        return printSide == CaraImpresion.DOUBLE_SIDED ? amount("1.80") : amount("1.00");
    }

    private BigDecimal sizeMultiplierForCampaign(TamanoPapel paperSize) {
        return paperSize == TamanoPapel.A3 ? amount("1.40") : amount("1.00");
    }

    private BigDecimal sideMultiplierForCampaign(CaraImpresion printSide) {
        return printSide == CaraImpresion.DOUBLE_SIDED ? amount("1.30") : amount("1.00");
    }

    private String fileReference(int fileCount) {
        return fileCount > 0 ? fileCount + " archivo(s)" : "1 archivo de referencia";
    }

    private String pageReference(int pageCount, int fileCount) {
        if (pageCount > 0) {
            return pageCount == 1
                ? "1 pagina detectada en " + Math.max(fileCount, 1) + " archivo"
                : pageCount + " paginas detectadas en " + Math.max(fileCount, 1) + " archivo(s)";
        }

        return "1 pagina estimada base";
    }

    private boolean hasUrgentRequest(String observations) {
        if (observations == null || observations.isBlank()) {
            return false;
        }

        String normalized = observations.toLowerCase(Locale.ROOT);
        return normalized.contains("urgente") || normalized.contains("express");
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private record PricingInput(
        TipoTrabajo jobType,
        Integer copies,
        ModoColor colorMode,
        CaraImpresion printSide,
        TamanoPapel paperSize,
        String observations,
        int fileCount,
        int pageCount
    ) {
    }
}

