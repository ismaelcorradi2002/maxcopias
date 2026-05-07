package com.maxcopias.controller;

import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.repository.RepositorioPedidoCopisteria;
import com.maxcopias.service.CloudinaryStorageService;
import com.maxcopias.service.ServicioAlmacenamientoArchivos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class ControladorArchivosPedido {

    private final RepositorioPedidoCopisteria repositorioPedidoCopisteria;
    private final ServicioAlmacenamientoArchivos servicioAlmacenamientoArchivos;
    private final CloudinaryStorageService cloudinaryStorageService;

    public ControladorArchivosPedido(
        RepositorioPedidoCopisteria repositorioPedidoCopisteria,
        ServicioAlmacenamientoArchivos servicioAlmacenamientoArchivos,
        CloudinaryStorageService cloudinaryStorageService
    ) {
        this.repositorioPedidoCopisteria = repositorioPedidoCopisteria;
        this.servicioAlmacenamientoArchivos = servicioAlmacenamientoArchivos;
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @GetMapping("/pedidos/copisteria/{id}/archivo")
    public ResponseEntity<Resource> descargarArchivoPedido(
        @PathVariable Long id,
        @RequestParam(name = "index", defaultValue = "0") int index,
        @RequestParam(name = "download", defaultValue = "false") boolean download,
        Authentication authentication
    ) {
        PedidoCopisteria pedido = repositorioPedidoCopisteria.findByIdWithUsuario(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado."));

        validarAcceso(authentication, pedido);

        if (!StringUtils.hasText(pedido.getRutaArchivo())) {
            throw new ResponseStatusException(NOT_FOUND, "El pedido no tiene archivo asociado.");
        }

        if (index < 0 || index >= pedido.getRutasArchivo().size()) {
            throw new ResponseStatusException(NOT_FOUND, "El archivo solicitado no existe en este pedido.");
        }

        String rutaArchivo = pedido.getRutasArchivo().get(index);
        if (cloudinaryStorageService.esUrlCloudinary(rutaArchivo)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, rutaArchivo)
                .build();
        }

        Path archivo = servicioAlmacenamientoArchivos.resolveStoredPath(rutaArchivo);
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo)) {
            throw new ResponseStatusException(NOT_FOUND, "No se ha encontrado el archivo del pedido.");
        }

        String contentType = detectarContentType(archivo);
        String nombreArchivo = pedido.getNombresArchivo().size() > index
            ? pedido.getNombresArchivo().get(index)
            : (pedido.getNombreArchivo() != null ? pedido.getNombreArchivo() : archivo.getFileName().toString());
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
            .filename(nombreArchivo)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(resolveContentLength(pedido, archivo))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(new FileSystemResource(archivo));
    }

    private void validarAcceso(Authentication authentication, PedidoCopisteria pedido) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isWorker = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_WORKER".equals(authority.getAuthority()));

        if (isAdmin || isWorker) {
            return;
        }

        String currentEmail = authentication.getName();
        if (pedido.getUsuario() == null || !currentEmail.equalsIgnoreCase(pedido.getUsuario().getEmail())) {
            throw new ResponseStatusException(FORBIDDEN, "No tienes permiso para acceder a este archivo.");
        }
    }

    private String detectarContentType(Path archivo) {
        try {
            String detected = Files.probeContentType(archivo);
            return StringUtils.hasText(detected) ? detected : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException exception) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private long resolveContentLength(PedidoCopisteria pedido, Path archivo) {
        try {
            return Files.size(archivo);
        } catch (IOException exception) {
            return -1L;
        }
    }
}
