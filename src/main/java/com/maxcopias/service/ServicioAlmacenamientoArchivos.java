package com.maxcopias.service;

import com.maxcopias.config.PropiedadesMaxcopias;
import com.maxcopias.model.AnalisisArchivoSubido;
import com.maxcopias.model.DatosArchivoGuardado;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServicioAlmacenamientoArchivos {

    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ISO_DATE;

    private final PropiedadesMaxcopias properties;
    private final ServicioInspeccionArchivos fileInspectionService;

    public ServicioAlmacenamientoArchivos(PropiedadesMaxcopias properties, ServicioInspeccionArchivos fileInspectionService) {
        this.properties = properties;
        this.fileInspectionService = fileInspectionService;
    }

    public List<DatosArchivoGuardado> storeFiles(List<MultipartFile> files, String reference) {
        List<MultipartFile> safeFiles = files == null
            ? List.of()
            : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        List<AnalisisArchivoSubido> analyzedFiles = fileInspectionService.inspectOrderFiles(safeFiles);

        Path orderDirectory = null;

        try {
            Path uploadRoot = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            orderDirectory = uploadRoot
                .resolve(LocalDate.now().format(DATE_FOLDER_FORMAT))
                .resolve(reference);

            Files.createDirectories(orderDirectory);

            List<DatosArchivoGuardado> storedFiles = new ArrayList<>();

            for (int index = 0; index < safeFiles.size(); index++) {
                MultipartFile file = safeFiles.get(index);
                AnalisisArchivoSubido analysis = analyzedFiles.get(index);
                String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                    ? StringUtils.cleanPath(file.getOriginalFilename())
                    : "archivo";
                String storedFilename = buildStoredFilename(index + 1, originalFilename);
                Path targetFile = orderDirectory.resolve(storedFilename);

                file.transferTo(targetFile);

                String relativePath = uploadRoot.relativize(targetFile).toString().replace("\\", "/");
                storedFiles.add(new DatosArchivoGuardado(
                    originalFilename,
                    storedFilename,
                    relativePath,
                    file.getContentType(),
                    file.getSize(),
                    analysis.getPageCount()
                ));
            }

            return storedFiles;
        } catch (IOException exception) {
            cleanupDirectory(orderDirectory);
            throw new ExcepcionAlmacenamientoArchivos("No se han podido guardar los archivos. Intentalo de nuevo.", exception);
        }
    }

    public DatosArchivoGuardado storeOrderFile(MultipartFile file, String reference) {
        List<DatosArchivoGuardado> storedFiles = storeFiles(file == null ? List.of() : List.of(file), reference);
        return storedFiles.get(0);
    }

    public Path resolveStoredPath(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new ExcepcionAlmacenamientoArchivos("El pedido no tiene archivo asociado.");
        }

        Path uploadRoot = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        Path resolvedPath = uploadRoot.resolve(relativePath).normalize();

        if (!resolvedPath.startsWith(uploadRoot)) {
            throw new ExcepcionAlmacenamientoArchivos("La ruta del archivo no es valida.");
        }

        return resolvedPath;
    }

    private String buildStoredFilename(int index, String originalFilename) {
        String extension = extractExtension(originalFilename);
        String baseName = StringUtils.stripFilenameExtension(originalFilename);
        String sanitizedBaseName = sanitizeBaseName(baseName);

        return String.format("%02d-%s.%s", index, sanitizedBaseName, extension);
    }

    private String sanitizeBaseName(String baseName) {
        String safeBaseName = StringUtils.hasText(baseName) ? baseName : "archivo";
        String normalized = safeBaseName
            .replaceAll("[^A-Za-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "")
            .toLowerCase();

        if (!StringUtils.hasText(normalized)) {
            return "archivo";
        }

        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }

    private String extractExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase();
    }

    private void cleanupDirectory(Path orderDirectory) {
        if (orderDirectory == null || !Files.exists(orderDirectory)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(orderDirectory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}

