package com.maxcopias.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudinaryStorageService.class);

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "application/octet-stream",
        "application/msword",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
        "doc",
        "docx",
        "jpeg",
        "jpg",
        "pdf",
        "png",
        "webp"
    );

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryStorageService(
        Cloudinary cloudinary,
        @Value("${cloudinary.cloud-name:}") String cloudName,
        @Value("${cloudinary.api-key:}") String apiKey,
        @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public ResultadoSubidaCloudinary subirArchivo(MultipartFile file, String carpeta) {
        validarArchivo(file);
        String originalFilename = limpiarNombreOriginal(file.getOriginalFilename());
        LOGGER.info("Cloudinary upload start. folder={}, file={}, size={} bytes", sanitizarCarpeta(carpeta), originalFilename, file.getSize());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", sanitizarCarpeta(carpeta),
                    "resource_type", "auto",
                    "use_filename", true,
                    "unique_filename", true,
                    "filename_override", originalFilename,
                    "overwrite", false
                )
            );

            String secureUrl = (String) resultado.get("secure_url");
            if (!StringUtils.hasText(secureUrl)) {
                throw new ExcepcionAlmacenamientoArchivos("Cloudinary no ha devuelto una URL segura para el archivo subido.");
            }

            LOGGER.info("Cloudinary upload done. folder={}, file={}, secureUrl={}", sanitizarCarpeta(carpeta), originalFilename, secureUrl);

            return new ResultadoSubidaCloudinary(
                secureUrl,
                originalFilename,
                file.getContentType(),
                file.getSize()
            );
        } catch (IOException exception) {
            throw new ExcepcionAlmacenamientoArchivos("No se ha podido leer el archivo para subirlo a Cloudinary.", exception);
        } catch (RuntimeException exception) {
            throw new ExcepcionAlmacenamientoArchivos("No se ha podido subir el archivo a Cloudinary.", exception);
        }
    }

    public boolean estaConfigurado() {
        return StringUtils.hasText(cloudName)
            && StringUtils.hasText(apiKey)
            && StringUtils.hasText(apiSecret);
    }

    public boolean esUrlCloudinary(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        try {
            URI uri = URI.create(value);
            return uri.getHost() != null && uri.getHost().toLowerCase(Locale.ROOT).contains("cloudinary.com");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcepcionAlmacenamientoArchivos("Debes seleccionar un archivo valido.");
        }

        String originalFilename = limpiarNombreOriginal(file.getOriginalFilename());
        if (!StringUtils.hasText(originalFilename)) {
            throw new ExcepcionAlmacenamientoArchivos("El archivo no tiene un nombre valido.");
        }

        String extension = obtenerExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ExcepcionAlmacenamientoArchivos("El archivo " + originalFilename + " no tiene un formato permitido.");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ExcepcionAlmacenamientoArchivos("El archivo " + originalFilename + " no tiene un tipo permitido.");
        }
    }

    private String sanitizarCarpeta(String carpeta) {
        String safeFolder = StringUtils.hasText(carpeta) ? carpeta.trim() : "maxcopias/general";
        return safeFolder
            .replace('\\', '/')
            .replaceAll("/+", "/")
            .replaceAll("(^/+|/+$)", "");
    }

    private String limpiarNombreOriginal(String originalFilename) {
        return StringUtils.hasText(originalFilename) ? StringUtils.cleanPath(originalFilename) : null;
    }

    private String obtenerExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    public record ResultadoSubidaCloudinary(
        String secureUrl,
        String originalFilename,
        String contentType,
        long sizeInBytes
    ) {
    }
}
