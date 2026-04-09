package com.maxcopias.model;

public enum TipoTrabajo {

    IMPRESION(
        "Impresion",
        "Sube documentos PDF o imagenes listos para imprimir.",
        "Acepta apuntes, documentos, presentaciones y fotos en alta calidad.",
        true
    ),
    FOTOCOPIAS(
        "Fotocopias",
        "Adjunta documentos o imagenes para copiar en el formato que necesites.",
        "Puedes enviar documentos academicos, oficiales, DNI o copias simples.",
        true
    ),
    ENCUADERNACION(
        "Encuadernacion",
        "Sube el documento final para encuadernar o una referencia clara del acabado.",
        "Ideal para trabajos universitarios, memorias, tesis y dossiers.",
        false
    ),
    PLASTIFICADO(
        "Plastificado",
        "Adjunta el documento a plastificar o una imagen de referencia.",
        "Pensado para carteles, fichas, menus y documentos de uso frecuente.",
        false
    ),
    DISENO_GRAFICO(
        "Diseno grafico",
        "Sube bocetos, logos, imagenes o textos base para preparar el diseno.",
        "Flyers, carteles, tarjetas, maquetacion y piezas visuales basicas.",
        false
    ),
    PUBLICIDAD_IMPRENTA(
        "Publicidad e imprenta",
        "Adjunta artes finales, logos o referencias visuales del material promocional.",
        "Flyers, carteles, dipticos, tripticos, pegatinas y vinilos.",
        true
    ),
    PERSONALIZACION(
        "Personalizacion",
        "Sube imagenes, frases, logos o referencias para productos personalizados.",
        "Camisetas, tazas y regalos personalizados para eventos o detalles.",
        false
    ),
    SERVICIOS_ADICIONALES(
        "Servicios adicionales",
        "Adjunta el documento o archivo base y explica el servicio que necesitas.",
        "Escaneado, fax, impresiones urgentes y trabajos complementarios.",
        false
    ),
    OTRO(
        "Otro",
        "Adjunta cualquier documento, foto o archivo util para explicar el encargo.",
        "Usa esta opcion para necesidades especiales fuera de los servicios habituales.",
        false
    );

    private final String label;
    private final String uploadLabel;
    private final String uploadDescription;
    private final boolean requiresPrintConfiguration;

    TipoTrabajo(String label, String uploadLabel, String uploadDescription, boolean requiresPrintConfiguration) {
        this.label = label;
        this.uploadLabel = uploadLabel;
        this.uploadDescription = uploadDescription;
        this.requiresPrintConfiguration = requiresPrintConfiguration;
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
}

