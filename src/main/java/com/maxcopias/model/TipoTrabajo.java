package com.maxcopias.model;

/**
 * Tipos de trabajos disponibles en copistería.
 */
public enum TipoTrabajo {

    IMPRESION(
        "Impresion",
        "Sube documentos PDF o imagenes listos para imprimir.",
        "Acepta apuntes, documentos, presentaciones y fotos en alta calidad.",
        true,
        "IM",
        "Desde 0,06 EUR"
    ),
    FOTOCOPIAS(
        "Fotocopias",
        "Adjunta documentos o imagenes para copiar en el formato que necesites.",
        "Puedes enviar documentos academicos, oficiales, DNI o copias simples.",
        true,
        "FC",
        "Desde 0,05 EUR"
    ),
    ENCUADERNACION(
        "Encuadernacion",
        "Sube el documento final para encuadernar o una referencia clara del acabado.",
        "Ideal para trabajos universitarios, memorias, tesis y dossiers.",
        false,
        "EN",
        "Desde 3,50 EUR"
    ),
    PLASTIFICADO(
        "Plastificado",
        "Adjunta el documento a plastificar o una imagen de referencia.",
        "Pensado para carteles, fichas, menus y documentos de uso frecuente.",
        false,
        "PL",
        "Desde 1,80 EUR"
    ),
    DISENO_GRAFICO(
        "Diseno grafico",
        "Sube bocetos, logos, imagenes o textos base para preparar el diseno.",
        "Flyers, carteles, tarjetas, maquetacion y piezas visuales basicas.",
        false,
        "DG",
        "Desde 25,00 EUR"
    ),
    PUBLICIDAD_IMPRENTA(
        "Publicidad e imprenta",
        "Adjunta artes finales, logos o referencias visuales del material promocional.",
        "Flyers, carteles, dipticos, tripticos, pegatinas y vinilos.",
        true,
        "PI",
        "Desde 19,00 EUR"
    ),
    PERSONALIZACION(
        "Personalizacion",
        "Sube imagenes, frases, logos o referencias para productos personalizados.",
        "Camisetas, tazas y regalos personalizados para eventos o detalles.",
        false,
        "PS",
        "Desde 9,90 EUR"
    ),
    SERVICIOS_ADICIONALES(
        "Servicios adicionales",
        "Adjunta el documento o archivo base y explica el servicio que necesitas.",
        "Escaneado, fax, impresiones urgentes y trabajos complementarios.",
        false,
        "AD",
        "Desde 0,50 EUR"
    ),
    OTRO(
        "Otro",
        "Adjunta cualquier documento, foto o archivo util para explicar el encargo.",
        "Usa esta opcion para necesidades especiales fuera de los servicios habituales.",
        false,
        "OT",
        "Presupuesto"
    );

    private final String label;
    private final String uploadLabel;
    private final String uploadDescription;
    private final boolean requiresPrintConfiguration;
    private final String shortCode;
    private final String priceHint;

    TipoTrabajo(
        String label,
        String uploadLabel,
        String uploadDescription,
        boolean requiresPrintConfiguration,
        String shortCode,
        String priceHint
    ) {
        this.label = label;
        this.uploadLabel = uploadLabel;
        this.uploadDescription = uploadDescription;
        this.requiresPrintConfiguration = requiresPrintConfiguration;
        this.shortCode = shortCode;
        this.priceHint = priceHint;
    }

    public String getLabel() {
        return label;
    }

    public String getUploadLabel() {
        return uploadLabel;
    }

    public String getUploadDescription() {
        return uploadDescription;
    }

    public boolean isRequiresPrintConfiguration() {
        return requiresPrintConfiguration;
    }

    public boolean isPrimaryWizardOption() {
        return switch (this) {
            case IMPRESION, FOTOCOPIAS, DISENO_GRAFICO, PUBLICIDAD_IMPRENTA, OTRO -> true;
            default -> false;
        };
    }

    public boolean usesCompleteWizard() {
        return this == IMPRESION || this == FOTOCOPIAS;
    }

    public boolean allowsBindingStep() {
        return this == IMPRESION || this == FOTOCOPIAS;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getPriceHint() {
        return priceHint;
    }
}

