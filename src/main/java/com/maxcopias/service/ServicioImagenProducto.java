package com.maxcopias.service;

import com.maxcopias.config.PropiedadesMaxcopias;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServicioImagenProducto {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    private final PropiedadesMaxcopias properties;

    public ServicioImagenProducto(PropiedadesMaxcopias properties) {
        this.properties = properties;
    }

    public ImagenProductoGuardada guardar(MultipartFile imagen, String referencia) {
        validar(imagen);

        Path root = Path.of(properties.getProductImageUploadDir()).toAbsolutePath().normalize();
        String extension = obtenerExtension(imagen.getOriginalFilename());
        String nombreBase = sanitizarNombre(referencia);
        String storedFilename = nombreBase + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        Path targetDirectory = root
            .resolve(String.valueOf(LocalDate.now().getYear()))
            .resolve(String.format(Locale.ROOT, "%02d", LocalDate.now().getMonthValue()));
        Path targetFile = targetDirectory.resolve(storedFilename).normalize();

        try {
            Files.createDirectories(targetDirectory);
            imagen.transferTo(targetFile);
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se ha podido guardar la imagen del producto.");
        }

        String relativePath = root.relativize(targetFile).toString().replace("\\", "/");
        return new ImagenProductoGuardada(
            "/media/productos/" + relativePath,
            StringUtils.hasText(imagen.getOriginalFilename()) ? StringUtils.cleanPath(imagen.getOriginalFilename()) : storedFilename,
            imagen.getContentType(),
            imagen.getSize()
        );
    }

    public void eliminarSiEsLocal(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith("/media/productos/")) {
            return;
        }

        String relativePath = imageUrl.substring("/media/productos/".length());
        Path root = Path.of(properties.getProductImageUploadDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();

        if (!target.startsWith(root)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private void validar(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen valida.");
        }

        String contentType = imagen.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Formato no valido. Usa JPG, PNG o WEBP.");
        }

        if (imagen.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB.");
        }

        String extension = obtenerExtension(imagen.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Extension no valida. Usa JPG, PNG o WEBP.");
        }
    }

    private String obtenerExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String sanitizarNombre(String referencia) {
        String safeReference = StringUtils.hasText(referencia) ? referencia : "producto";
        String normalized = safeReference
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");

        if (!StringUtils.hasText(normalized)) {
            return "producto";
        }

        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    public record ImagenProductoGuardada(
        String imageUrl,
        String imageName,
        String imageType,
        long imageSize
    ) {
    }
}
