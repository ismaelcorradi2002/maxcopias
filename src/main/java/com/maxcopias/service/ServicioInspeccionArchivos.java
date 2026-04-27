package com.maxcopias.service;

import com.maxcopias.config.PropiedadesMaxcopias;
import com.maxcopias.model.AnalisisArchivoSubido;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServicioInspeccionArchivos {

    private final PropiedadesMaxcopias properties;

    public ServicioInspeccionArchivos(PropiedadesMaxcopias properties) {
        this.properties = properties;
    }

    public List<AnalisisArchivoSubido> inspectOrderFiles(List<MultipartFile> files) {
        return inspectFiles(files, true);
    }

    public List<AnalisisArchivoSubido> inspectPreviewFiles(List<MultipartFile> files) {
        return inspectFiles(files, false);
    }

    private List<AnalisisArchivoSubido> inspectFiles(List<MultipartFile> files, boolean requireAtLeastOneFile) {
        List<MultipartFile> safeFiles = files == null
            ? List.of()
            : files.stream().filter(file -> file != null && !file.isEmpty()).toList();

        validateFiles(safeFiles, requireAtLeastOneFile);

        return safeFiles.stream()
            .map(this::analyzeFile)
            .toList();
    }

    private void validateFiles(List<MultipartFile> files, boolean requireAtLeastOneFile) {
        if (requireAtLeastOneFile && files.isEmpty()) {
            throw new ExcepcionAlmacenamientoArchivos("Debes adjuntar al menos un archivo para preparar el pedido.");
        }

        if (files.size() > properties.getMaxFiles()) {
            throw new ExcepcionAlmacenamientoArchivos("Puedes subir un maximo de " + properties.getMaxFiles() + " archivos por pedido.");
        }

        List<String> allowedExtensions = properties.getAllowedExtensions().stream()
            .map(extension -> extension.toLowerCase(Locale.ROOT))
            .toList();

        for (MultipartFile file : files) {
            String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? StringUtils.cleanPath(file.getOriginalFilename())
                : "";

            if (!StringUtils.hasText(originalFilename)) {
                throw new ExcepcionAlmacenamientoArchivos("Uno de los archivos no tiene nombre valido.");
            }

            String extension = extractExtension(originalFilename);
            if (!allowedExtensions.contains(extension)) {
                throw new ExcepcionAlmacenamientoArchivos("El archivo " + originalFilename + " no tiene un formato permitido.");
            }

            if (file.getSize() > properties.getMaxFileSize().toBytes()) {
                throw new ExcepcionAlmacenamientoArchivos("El archivo " + originalFilename + " supera el tamano maximo permitido.");
            }
        }
    }

    private AnalisisArchivoSubido analyzeFile(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        int pageCount = resolvePageCount(file, extension, originalFilename);

        return new AnalisisArchivoSubido(
            originalFilename,
            file.getContentType(),
            file.getSize(),
            pageCount
        );
    }

    private int resolvePageCount(MultipartFile file, String extension, String originalFilename) {
        return switch (extension) {
            case "pdf" -> countPdfPages(file, originalFilename);
            case "jpg", "jpeg", "png" -> 1;
            default -> 1;
        };
    }

    private int countPdfPages(MultipartFile file, String originalFilename) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return Math.max(document.getNumberOfPages(), 1);
        } catch (IOException exception) {
            throw new ExcepcionAlmacenamientoArchivos(
                "No se ha podido leer el PDF " + originalFilename + ". Comprueba que el archivo no este danado e intentalo de nuevo.",
                exception
            );
        }
    }

    private String extractExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }
}

