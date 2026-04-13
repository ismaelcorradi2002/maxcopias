package com.maxcopias.service;

/**
 * Servicio de calculo de precios orientativos para copisteria.
 */
import com.maxcopias.dto.FormularioPedidoCopisteria;
import com.maxcopias.model.CaraImpresion;
import com.maxcopias.model.EstimacionPrecioCopisteria;
import com.maxcopias.model.LineaPrecioCopisteria;
import com.maxcopias.model.ModoColor;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.TamanoPapel;
import com.maxcopias.model.TipoEncuadernacion;
import com.maxcopias.model.TipoPapel;
import com.maxcopias.model.TipoTrabajo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServicioPrecioCopisteria {

    private static final BigDecimal IMPRESION_BW = amount("0.06");
    private static final BigDecimal IMPRESION_COLOR = amount("0.45");
    private static final BigDecimal FOTOCOPIA_BW = amount("0.05");
    private static final BigDecimal FOTOCOPIA_COLOR = amount("0.18");
    private static final BigDecimal PUBLICIDAD_BASE = amount("19.00");
    private static final BigDecimal DISENO_BASE = amount("25.00");
    private static final BigDecimal OTRO_BASE = amount("12.00");
    private static final BigDecimal EXTRA_PLASTIFICADO = amount("1.80");
    private static final BigDecimal EXTRA_URGENTE = amount("2.00");
    private static final BigDecimal EXTRA_ESCANEADO = amount("0.50");
    private static final BigDecimal EXTRA_ENCUADERNACION_ESPIRAL = amount("3.50");
    private static final BigDecimal EXTRA_ENCUADERNACION_TAPA_DURA = amount("7.50");
    private static final BigDecimal EXTRA_ENCUADERNACION_GRAPADO = amount("0.60");

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
            form.getTipoPapel(),
            form.getTipoEncuadernacion(),
            Boolean.TRUE.equals(form.getPlastificado()),
            Boolean.TRUE.equals(form.getUrgente()),
            Boolean.TRUE.equals(form.getEscaneado()),
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
            order.getTipoPapel(),
            order.getTipoEncuadernacion(),
            order.isPlastificado(),
            order.isUrgente(),
            order.isEscaneado(),
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
            case DISENO_GRAFICO -> calculateQuoteStyle(input, DISENO_BASE, "diseno grafico");
            case OTRO -> calculateQuoteStyle(input, OTRO_BASE, "encargo especial");
            default -> calculateQuoteStyle(input, OTRO_BASE, "encargo especial");
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
        ModoColor colorMode = input.colorMode() != null ? input.colorMode() : ModoColor.BLACK_AND_WHITE;
        CaraImpresion printSide = input.printSide() != null ? input.printSide() : CaraImpresion.ONE_SIDED;
        TamanoPapel paperSize = input.paperSize() != null ? input.paperSize() : TamanoPapel.A4;
        TipoPapel paperType = input.paperType() != null ? input.paperType() : TipoPapel.NORMAL;
        TipoEncuadernacion bindingType = input.bindingType() != null ? input.bindingType() : TipoEncuadernacion.SIN_ENCUADERNACION;

        BigDecimal baseUnit = colorMode == ModoColor.COLOR ? colorUnitPrice : bwUnitPrice;
        BigDecimal basePrint = baseUnit.multiply(BigDecimal.valueOf((long) pages * copies));
        BigDecimal sizeExtra = surcharge(basePrint, sizeMultiplier(paperSize));
        BigDecimal withSize = basePrint.add(sizeExtra);
        BigDecimal sideExtra = surcharge(withSize, sideMultiplier(printSide));
        BigDecimal withSides = withSize.add(sideExtra);
        BigDecimal paperExtra = surcharge(withSides, paperTypeMultiplier(paperType));
        BigDecimal bindingExtra = bindingPrice(bindingType);
        BigDecimal plastificadoExtra = plastificadoPrice(input);
        BigDecimal urgenteExtra = urgentePrice(input);
        BigDecimal escaneadoExtra = escaneadoPrice(input, pages);

        List<LineaPrecioCopisteria> lines = new ArrayList<>();
        lines.add(new LineaPrecioCopisteria(
            input.jobType() == TipoTrabajo.FOTOCOPIAS
                ? "Fotocopias " + colorLabel(colorMode)
                : "Impresion " + colorLabel(colorMode),
            pages + " pagina(s) x " + copies + " copia(s)",
            basePrint
        ));
        addLineIfPositive(lines, "Formato " + paperSize.getLabel(), "Ajuste por tamano del papel", sizeExtra);
        addLineIfPositive(lines, printSide.getLabel(), "Configuracion de caras del pedido", sideExtra);
        addLineIfPositive(lines, "Papel " + paperType.getLabel(), "Acabado seleccionado", paperExtra);
        addLineIfPositive(lines, "Encuadernacion " + bindingType.getLabel(), "Acabado adicional", bindingExtra);
        addLineIfPositive(lines, "Plastificado", "Proteccion del documento", plastificadoExtra);
        addLineIfPositive(lines, "Servicio urgente", "Prioridad de preparacion", urgenteExtra);
        addLineIfPositive(lines, "Escaneado", pages + " pagina(s) a digitalizar", escaneadoExtra);

        BigDecimal total = lines.stream()
            .map(LineaPrecioCopisteria::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> breakdownItems = new ArrayList<>();
        breakdownItems.add(pageReference(input.pageCount(), input.fileCount()) + " x " + copies + " copia(s)");
        breakdownItems.add(colorMode.getLabel());
        breakdownItems.add(paperSize.getLabel());
        breakdownItems.add(printSide.getLabel());
        breakdownItems.add(paperType.getLabel());
        if (!bindingType.isNone()) {
            breakdownItems.add(bindingType.getLabel());
        }
        appendExtrasBreakdown(breakdownItems, input);

        return buildEstimate(total, String.join(" • ", breakdownItems), input.fileCount(), input.pageCount(), jobLabel, lines);
    }

    private EstimacionPrecioCopisteria calculatePublicidad(PricingInput input) {
        int copies = normalizeCopies(input.copies());
        int pages = pricedPages(input.pageCount());
        ModoColor colorMode = input.colorMode() != null ? input.colorMode() : ModoColor.COLOR;
        TamanoPapel paperSize = input.paperSize() != null ? input.paperSize() : TamanoPapel.A4;
        TipoPapel paperType = input.paperType() != null ? input.paperType() : TipoPapel.NORMAL;

        BigDecimal production = (colorMode == ModoColor.COLOR ? amount("0.22") : amount("0.12"))
            .multiply(sizeMultiplierForCampaign(paperSize))
            .multiply(paperTypeMultiplier(paperType))
            .multiply(BigDecimal.valueOf((long) pages * copies));
        BigDecimal plastificadoExtra = plastificadoPrice(input);
        BigDecimal urgenteExtra = urgentePrice(input);
        BigDecimal escaneadoExtra = escaneadoPrice(input, pages);

        List<LineaPrecioCopisteria> lines = new ArrayList<>();
        lines.add(new LineaPrecioCopisteria("Base de publicidad e imprenta", "Preparacion del encargo", PUBLICIDAD_BASE));
        lines.add(new LineaPrecioCopisteria(
            "Produccion " + colorLabel(colorMode),
            pages + " pagina(s) x " + copies + " unidad(es) • " + paperSize.getLabel() + " • " + paperType.getLabel(),
            production
        ));
        addLineIfPositive(lines, "Plastificado", "Proteccion del documento", plastificadoExtra);
        addLineIfPositive(lines, "Servicio urgente", "Prioridad de preparacion", urgenteExtra);
        addLineIfPositive(lines, "Escaneado", pages + " pagina(s) a digitalizar", escaneadoExtra);

        BigDecimal total = lines.stream()
            .map(LineaPrecioCopisteria::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> breakdownItems = new ArrayList<>();
        breakdownItems.add("Base de imprenta");
        breakdownItems.add(pageReference(input.pageCount(), input.fileCount()) + " x " + copies + " unidad(es)");
        breakdownItems.add(colorMode.getLabel());
        breakdownItems.add(paperSize.getLabel());
        breakdownItems.add(paperType.getLabel());
        appendExtrasBreakdown(breakdownItems, input);

        return buildEstimate(total, String.join(" • ", breakdownItems), input.fileCount(), input.pageCount(), "publicidad e imprenta", lines);
    }

    private EstimacionPrecioCopisteria calculateQuoteStyle(PricingInput input, BigDecimal basePrice, String jobLabel) {
        BigDecimal additionalFiles = amount("2.50").multiply(BigDecimal.valueOf(Math.max(input.fileCount(), 1) - 1L));
        BigDecimal plastificadoExtra = plastificadoPrice(input);
        BigDecimal urgenteExtra = urgentePrice(input);
        BigDecimal escaneadoExtra = escaneadoPrice(input, pricedPages(input.pageCount()));

        List<LineaPrecioCopisteria> lines = new ArrayList<>();
        lines.add(new LineaPrecioCopisteria("Base de " + jobLabel, fileReference(input.fileCount()), basePrice));
        addLineIfPositive(lines, "Archivos adicionales", Math.max(input.fileCount() - 1, 0) + " archivo(s)", additionalFiles);
        addLineIfPositive(lines, "Plastificado", "Proteccion del documento", plastificadoExtra);
        addLineIfPositive(lines, "Servicio urgente", "Prioridad de preparacion", urgenteExtra);
        addLineIfPositive(lines, "Escaneado", pricedPages(input.pageCount()) + " pagina(s) a digitalizar", escaneadoExtra);

        BigDecimal total = lines.stream()
            .map(LineaPrecioCopisteria::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> breakdownItems = new ArrayList<>();
        breakdownItems.add("Base de " + jobLabel);
        breakdownItems.add(fileReference(input.fileCount()));
        appendExtrasBreakdown(breakdownItems, input);

        return buildEstimate(total, String.join(" • ", breakdownItems), input.fileCount(), input.pageCount(), jobLabel, lines);
    }

    private EstimacionPrecioCopisteria buildEstimate(
        BigDecimal rawTotal,
        String breakdown,
        int fileCount,
        int pageCount,
        String jobLabel,
        List<LineaPrecioCopisteria> lines
    ) {
        BigDecimal total = rawTotal.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String note = fileCount > 0
            ? "Precio orientativo calculado con el servicio, las paginas detectadas y la configuracion elegida. El importe final puede ajustarse al revisar acabados especiales."
            : "Configura el pedido y sube tus archivos para afinar mejor el importe orientativo de " + jobLabel + ".";

        return new EstimacionPrecioCopisteria(total, breakdown, note, fileCount, pageCount, lines);
    }

    private EstimacionPrecioCopisteria emptyEstimate() {
        return new EstimacionPrecioCopisteria(
            BigDecimal.ZERO,
            "Selecciona un servicio para ver el precio orientativo del pedido.",
            "El importe se calcula automaticamente al combinar configuracion, archivos y extras.",
            0,
            0,
            List.of()
        );
    }

    private BigDecimal plastificadoPrice(PricingInput input) {
        if (!input.plastificado()) {
            return BigDecimal.ZERO;
        }

        return EXTRA_PLASTIFICADO.multiply(BigDecimal.valueOf(Math.max(input.fileCount(), 1L)));
    }

    private BigDecimal urgentePrice(PricingInput input) {
        return input.urgente() ? EXTRA_URGENTE : BigDecimal.ZERO;
    }

    private BigDecimal escaneadoPrice(PricingInput input, int pages) {
        if (!input.escaneado()) {
            return BigDecimal.ZERO;
        }

        return EXTRA_ESCANEADO.multiply(BigDecimal.valueOf(pages));
    }

    private BigDecimal bindingPrice(TipoEncuadernacion bindingType) {
        if (bindingType == null || bindingType.isNone()) {
            return BigDecimal.ZERO;
        }

        return switch (bindingType) {
            case ESPIRAL -> EXTRA_ENCUADERNACION_ESPIRAL;
            case TAPA_DURA -> EXTRA_ENCUADERNACION_TAPA_DURA;
            case GRAPADO -> EXTRA_ENCUADERNACION_GRAPADO;
            case SIN_ENCUADERNACION -> BigDecimal.ZERO;
        };
    }

    private void appendExtrasBreakdown(List<String> breakdownItems, PricingInput input) {
        if (input.plastificado()) {
            breakdownItems.add("Plastificado");
        }
        if (input.urgente()) {
            breakdownItems.add("Urgente");
        }
        if (input.escaneado()) {
            breakdownItems.add("Escaneado");
        }
    }

    private void addLineIfPositive(List<LineaPrecioCopisteria> lines, String concept, String detail, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        lines.add(new LineaPrecioCopisteria(concept, detail, amount));
    }

    private BigDecimal surcharge(BigDecimal base, BigDecimal multiplier) {
        return base.multiply(multiplier.subtract(BigDecimal.ONE)).max(BigDecimal.ZERO);
    }

    private int pricedPages(int pageCount) {
        return Math.max(pageCount, 1);
    }

    private int normalizeCopies(Integer copies) {
        return copies == null || copies < 1 ? 1 : copies;
    }

    private BigDecimal sizeMultiplier(TamanoPapel paperSize) {
        return switch (paperSize) {
            case A5 -> amount("0.72");
            case A3 -> amount("1.85");
            case A4 -> amount("1.00");
        };
    }

    private BigDecimal sideMultiplier(CaraImpresion printSide) {
        return printSide == CaraImpresion.DOUBLE_SIDED ? amount("1.80") : amount("1.00");
    }

    private BigDecimal paperTypeMultiplier(TipoPapel paperType) {
        return switch (paperType) {
            case SATINADO -> amount("1.35");
            case CARTULINA -> amount("1.65");
            case NORMAL -> amount("1.00");
        };
    }

    private BigDecimal sizeMultiplierForCampaign(TamanoPapel paperSize) {
        return switch (paperSize) {
            case A5 -> amount("0.78");
            case A3 -> amount("1.40");
            case A4 -> amount("1.00");
        };
    }

    private String colorLabel(ModoColor colorMode) {
        return colorMode == ModoColor.COLOR ? "color" : "blanco y negro";
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

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private record PricingInput(
        TipoTrabajo jobType,
        Integer copies,
        ModoColor colorMode,
        CaraImpresion printSide,
        TamanoPapel paperSize,
        TipoPapel paperType,
        TipoEncuadernacion bindingType,
        boolean plastificado,
        boolean urgente,
        boolean escaneado,
        int fileCount,
        int pageCount
    ) {
    }
}
