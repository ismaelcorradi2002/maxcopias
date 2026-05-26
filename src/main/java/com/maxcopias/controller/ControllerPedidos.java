package com.maxcopias.controller;

import com.maxcopias.dto.FormularioPedidoCopisteria;
import com.maxcopias.model.CaraImpresion;
import com.maxcopias.model.ModoColor;
import com.maxcopias.model.TamanoPapel;
import com.maxcopias.model.TipoEncuadernacion;
import com.maxcopias.model.TipoPapel;
import com.maxcopias.model.TipoTrabajo;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.Usuario;
import com.maxcopias.model.DatosArchivoGuardado;
import com.maxcopias.repository.RepositorioPedidoCopisteria;
import com.maxcopias.service.ExcepcionAlmacenamientoArchivos;
import com.maxcopias.service.GeneradorCodigoPedido;
import com.maxcopias.service.ServicioAlmacenamientoArchivos;
import com.maxcopias.service.ServicioUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import com.maxcopias.dto.RespuestaVistaPreviaPrecioCopisteria;
import com.maxcopias.model.EstimacionPrecioCopisteria;
import com.maxcopias.model.LineaPrecioCopisteria;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/copisteria")
public class ControllerPedidos {

    private static final String ORDER_SUBMISSION_TOKEN_ATTR = "copisteriaOrderSubmissionToken";
    private static final String ORDER_SUBMISSION_RESULTS_ATTR = "copisteriaOrderSubmissionResults";
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerPedidos.class);

    @Autowired
    private RepositorioPedidoCopisteria pedidoRepository;

    @Autowired
    private ServicioAlmacenamientoArchivos storageService;

    @Autowired
    private ServicioUsuario userService;

    @Autowired
    private GeneradorCodigoPedido generadorCodigoPedido;

    @InitBinder("orderForm")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("files"); // Prevent binding issues with file uploads
    }

@GetMapping({"", "/pedido", "/formulario"})
    public String formulario(@ModelAttribute("orderForm") FormularioPedidoCopisteria orderForm, Model model, HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            Usuario user = userService.findRequiredByEmail(username);
            orderForm.setCustomerName(user.getFirstName() + " " + user.getLastName());
            orderForm.setPhone(user.getPhone());
            orderForm.setEmail(user.getEmail());
        }
        // Pre-select defaults for smooth JS wizard progression
        if (orderForm.getJobType() == null) {
            orderForm.setJobType(TipoTrabajo.IMPRESION);
        }
        if (orderForm.getColorMode() == null) {
            orderForm.setColorMode(ModoColor.BLACK_AND_WHITE);
        }
        if (orderForm.getPaperSize() == null) {
            orderForm.setPaperSize(TamanoPapel.A4);
        }
        if (orderForm.getPrintSide() == null) {
            orderForm.setPrintSide(CaraImpresion.ONE_SIDED);
        }
        if (orderForm.getPaperType() == null) {
            orderForm.setPaperType(TipoPapel.NORMAL);
        }
        if (orderForm.getBindingType() == null) {
            orderForm.setBindingType(TipoEncuadernacion.SIN_ENCUADERNACION);
        }
        if (orderForm.getCopies() == null || orderForm.getCopies() < 1) {
            orderForm.setCopies(1);
        }
        if (orderForm.getDeliveryMethod() == null || orderForm.getDeliveryMethod().isBlank()) {
            orderForm.setDeliveryMethod("STORE_PICKUP");
        }
        // Populate enum lists for template dropdowns
        model.addAttribute("primaryJobTypes", TipoTrabajo.values());
        model.addAttribute("colorModes", ModoColor.values());
        model.addAttribute("paperSizes", TamanoPapel.values());
        model.addAttribute("printSides", CaraImpresion.values());
        model.addAttribute("paperTypes", TipoPapel.values());
        model.addAttribute("bindingTypes", TipoEncuadernacion.values());
        
        // Static preview and config
        model.addAttribute("acceptedFormats", "PDF, DOC, DOCX, JPG, JPEG, PNG, WEBP");
        model.addAttribute("maxFileSizeLabel", "20 MB");
        model.addAttribute("maxCopiesPerOrder", FormularioPedidoCopisteria.MAX_COPIES);
        
        // Empty price preview (will be populated by JS/service)
        model.addAttribute("pricePreview", new com.maxcopias.model.EstimacionPrecioCopisteria(
            java.math.BigDecimal.ZERO, 
            "Selecciona un servicio para ver el precio orientativo del pedido.", 
            "El importe se calcula automáticamente según la configuración y los archivos.", 
            0, 0
        ));
        model.addAttribute("submissionToken", ensureSubmissionToken(session));
        
        return "copisteria/formulario";
    }

    private String formulario(FormularioPedidoCopisteria orderForm, Model model) {
        jakarta.servlet.http.HttpServletRequest request =
            ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return formulario(orderForm, model, request.getSession());
    }

    @PostMapping
    public String procesarPedido(
            @Valid @ModelAttribute("orderForm") FormularioPedidoCopisteria orderForm,
            BindingResult result,
            @RequestParam(value = "archivo", required = false) MultipartFile[] archivos,
            @RequestParam("submissionToken") String submissionToken,
            Model model,
            HttpSession session) {
        
        Long existingOrderId = findOrderIdBySubmissionToken(session, submissionToken);
        if (existingOrderId != null) {
            LOGGER.warn("POST duplicado de copisteria detectado. token={}, pedidoExistente={}", submissionToken, existingOrderId);
            return "redirect:/copisteria/resumen?id=" + existingOrderId;
        }

        if (result.hasErrors()) {
            // Re-populate model attributes on validation error
            formulario(orderForm, model, session);
            return "copisteria/formulario";
        }

        if ("HOME_DELIVERY".equals(orderForm.getDeliveryMethod())
            && !validarDireccionEntrega(orderForm, result)) {
            formulario(orderForm, model, session);
            return "copisteria/formulario";
        }
        
        PedidoCopisteria pedido = new PedidoCopisteria();
        pedido.setCustomerName(orderForm.getCustomerName());
        pedido.setPhone(orderForm.getPhone());
        pedido.setEmail(orderForm.getEmail());
        pedido.setTrabajo(orderForm.getJobType());
        pedido.setCopias(orderForm.getCopies());
        pedido.setColor(orderForm.getColorMode());
        pedido.setTamano(orderForm.getPaperSize());
        pedido.setCaras(orderForm.getPrintSide());
        pedido.setPapel(orderForm.getPaperType());
        // Only set print-specific fields for IMPRESION or FOTOCOPIAS
        TipoTrabajo trabajo = orderForm.getJobType();
        if (trabajo == TipoTrabajo.IMPRESION || trabajo == TipoTrabajo.FOTOCOPIAS) {
            pedido.setEncuadernacion(orderForm.getBindingType());
        } else {
            pedido.setEncuadernacion(null);
            pedido.setCaras(null);
            pedido.setColor(null);
            pedido.setPapel(null);
            pedido.setTamano(null);
        }
        String extrasStr = construirExtrasPedido(orderForm);
        pedido.setExtras(extrasStr);
        // Use the price calculated by the frontend wizard
        String estimatedPrice = orderForm.getEstimatedPrice();
        BigDecimal precioCalculado = BigDecimal.ZERO;
        if (estimatedPrice != null && !estimatedPrice.isBlank()) {
            try {
                precioCalculado = new BigDecimal(estimatedPrice.replace(",", ".")).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored) {
                // fallback to zero if parsing fails
            }
        }
        pedido.setPrecio(precioCalculado);
        pedido.setRutaArchivo(null);

        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            model.addAttribute("error", "Debes iniciar sesión para realizar un pedido.");
            formulario(orderForm, model);
            return "copisteria/formulario";
        }
        String username = auth.getName();
        Usuario usuario = userService.findRequiredByEmail(username);
        pedido.setUsuario(usuario);

        boolean requiereArchivo = true;
        List<MultipartFile> archivosValidos = archivos == null
            ? List.of()
            : java.util.Arrays.stream(archivos)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (requiereArchivo && archivosValidos.isEmpty()) {
            result.reject("archivo.required", "Debes adjuntar un archivo valido para guardar el pedido.");
            formulario(orderForm, model);
            return "copisteria/formulario";
        }

        PedidoCopisteria savedPedido = null;
        try {
            LOGGER.info("Guardando pedido de copisteria. token={}, archivos={}", submissionToken, archivosValidos.size());
            pedido.setCodigoRecoger(generarCodigoRecogerUnico());
            savedPedido = pedidoRepository.saveAndFlush(pedido);
            if (!archivosValidos.isEmpty()) {
                List<DatosArchivoGuardado> archivosGuardados = storageService.storeFiles(archivosValidos, savedPedido.getCodigoRecoger());
                savedPedido.setRutasArchivo(
                    archivosGuardados.stream()
                        .map(DatosArchivoGuardado::getRelativePath)
                        .collect(Collectors.toList())
                );
                savedPedido.setNombresArchivo(
                    archivosGuardados.stream()
                        .map(DatosArchivoGuardado::getOriginalFilename)
                        .collect(Collectors.toList())
                );
                savedPedido.setTiposArchivo(
                    archivosGuardados.stream()
                        .map(DatosArchivoGuardado::getContentType)
                        .collect(Collectors.toList())
                );
                savedPedido.setTamanosArchivoLista(
                    archivosGuardados.stream()
                        .map(DatosArchivoGuardado::getSizeInBytes)
                        .collect(Collectors.toList())
                );
                savedPedido.setPaginasArchivoLista(
                    archivosGuardados.stream()
                        .map(DatosArchivoGuardado::getPageCount)
                        .collect(Collectors.toList())
                );
                pedidoRepository.saveAndFlush(savedPedido);
            }
            registerOrderIdForSubmissionToken(session, submissionToken, savedPedido.getId());
            rotateSubmissionToken(session);
        } catch (ExcepcionAlmacenamientoArchivos exception) {
            if (savedPedido != null && savedPedido.getId() != null) {
                pedidoRepository.deleteById(savedPedido.getId());
            }
            result.reject("archivo.invalid", exception.getMessage());
            formulario(orderForm, model);
            return "copisteria/formulario";
        }

        return "redirect:/copisteria/resumen?id=" + savedPedido.getId();
    }

    @GetMapping("/resumen")
    public String resumen(@RequestParam Long id, Model model) {
        PedidoCopisteria pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        model.addAttribute("pedidoId", pedido.getId());
        model.addAttribute("pickupCode", pedido.getPickupCode());
        model.addAttribute("formattedEstimatedPrice", pedido.getFormattedEstimatedPrice());
        model.addAttribute("formattedCreatedAt", pedido.getFormattedCreatedAt());
        model.addAttribute("fileCount", pedido.getFileCount());
        model.addAttribute("totalPageCount", resolverPaginasTotales(pedido));
        model.addAttribute("estadoLabel", pedido.getStatus() != null ? pedido.getStatus().getLabel() : "Sin estado");
        model.addAttribute("trabajoLabel", pedido.getJobType() != null ? pedido.getJobType().getLabel() : "Sin tipo");
        model.addAttribute("customerName", pedido.getCustomerName());
        model.addAttribute("phone", pedido.getPhone());
        model.addAttribute("email", pedido.getEmail());
        model.addAttribute("colorLabel", pedido.getColorMode() != null ? pedido.getColorMode().getLabel() : null);
        model.addAttribute("paperSizeLabel", pedido.getPaperSize() != null ? pedido.getPaperSize().getLabel() : null);
        model.addAttribute("copies", pedido.getCopias());
        model.addAttribute("printSideLabel", pedido.getPrintSide() != null ? pedido.getPrintSide().getLabel() : null);
        model.addAttribute("paperTypeLabel", pedido.getPaperType() != null ? pedido.getPaperType().getLabel() : null);
        model.addAttribute("bindingLabel", pedido.getBindingType() != null && !pedido.getBindingType().isNone() ? pedido.getBindingType().getLabel() : null);
        model.addAttribute("extrasSummary", resolverResumenExtras(pedido));
        model.addAttribute("observations", resolverObservaciones(pedido));
        model.addAttribute("deliveryMethodLabel", resolverMetodoEntrega(pedido));
        model.addAttribute("deliveryAddress", resolverDireccionEntrega(pedido));
        model.addAttribute("deliveryEtaLabel", resolverVentanaUrgente(pedido));
        model.addAttribute("priceBreakdown", pedido.getPriceBreakdownOrDefault());
        model.addAttribute("rutaArchivo", pedido.getRutaArchivo());
        model.addAttribute("nombreArchivo", pedido.getNombreArchivo());
        model.addAttribute("tamanoArchivoFormateado", pedido.getTamanoArchivoFormateado());
        model.addAttribute("archivoVerUrl", "/pedidos/copisteria/" + pedido.getId() + "/archivo");
        model.addAttribute("archivoDescargaUrl", "/pedidos/copisteria/" + pedido.getId() + "/archivo?download=true");
        model.addAttribute("archivosPedido", construirArchivosPedido(pedido));
        return "copisteria/resumen-simple";
    }

    private String generarCodigoRecogerUnico() {
        return generadorCodigoPedido.generarCodigoCopisteria(pedidoRepository::existsByCodigoRecoger);
    }

    private String ensureSubmissionToken(HttpSession session) {
        Object current = session.getAttribute(ORDER_SUBMISSION_TOKEN_ATTR);
        if (current instanceof String token && !token.isBlank()) {
            return token;
        }

        String newToken = UUID.randomUUID().toString();
        session.setAttribute(ORDER_SUBMISSION_TOKEN_ATTR, newToken);
        return newToken;
    }

    private void rotateSubmissionToken(HttpSession session) {
        session.setAttribute(ORDER_SUBMISSION_TOKEN_ATTR, UUID.randomUUID().toString());
    }

    @SuppressWarnings("unchecked")
    private Long findOrderIdBySubmissionToken(HttpSession session, String submissionToken) {
        if (submissionToken == null || submissionToken.isBlank()) {
            return null;
        }

        Object attribute = session.getAttribute(ORDER_SUBMISSION_RESULTS_ATTR);
        if (!(attribute instanceof Map<?, ?> storedMap)) {
            return null;
        }

        Object orderId = storedMap.get(submissionToken);
        return orderId instanceof Long value ? value : null;
    }

    @SuppressWarnings("unchecked")
    private void registerOrderIdForSubmissionToken(HttpSession session, String submissionToken, Long orderId) {
        if (submissionToken == null || submissionToken.isBlank() || orderId == null) {
            return;
        }

        Object attribute = session.getAttribute(ORDER_SUBMISSION_RESULTS_ATTR);
        Map<String, Long> results = attribute instanceof Map<?, ?> existingMap
            ? (Map<String, Long>) existingMap
            : new HashMap<>();
        results.put(submissionToken, orderId);
        session.setAttribute(ORDER_SUBMISSION_RESULTS_ATTR, results);
    }

    private String construirExtrasPedido(FormularioPedidoCopisteria orderForm) {
        StringBuilder extras = new StringBuilder();
        extras.append("plastificado=").append(Boolean.TRUE.equals(orderForm.getPlastificado()));
        extras.append(",urgente=").append(Boolean.TRUE.equals(orderForm.getUrgente()));
        extras.append(",escaneado=").append(Boolean.TRUE.equals(orderForm.getEscaneado()));
        extras.append(",deliveryMethod='").append(sanitizarTexto(orderForm.getDeliveryMethod())).append("'");
        extras.append(",deliveryAddress='").append(sanitizarTexto(orderForm.getDeliveryAddress())).append("'");
        extras.append(",observaciones='").append(sanitizarTexto(orderForm.getObservations())).append("'");

        return extras.toString();
    }

    private String sanitizarTexto(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", " ").trim();
    }

    private List<Map<String, String>> construirArchivosPedido(PedidoCopisteria pedido) {
        List<Map<String, String>> archivos = new ArrayList<>();
        List<String> rutas = pedido.getRutasArchivo();

        for (int index = 0; index < rutas.size(); index += 1) {
            String ruta = rutas.get(index);
            Map<String, String> item = new LinkedHashMap<>();
            item.put("nombre", resolverNombreArchivo(pedido, index, ruta));
            item.put("ruta", ruta);
            item.put("tamano", resolverTamanoArchivo(pedido, index, ruta));
            item.put("verUrl", "/pedidos/copisteria/" + pedido.getId() + "/archivo?index=" + index);
            item.put("descargaUrl", "/pedidos/copisteria/" + pedido.getId() + "/archivo?download=true&index=" + index);
            archivos.add(item);
        }

        return archivos;
    }

    private String resolverTamanoArchivo(PedidoCopisteria pedido, int index, String rutaRelativa) {
        List<Long> tamanos = pedido.getTamanosArchivoLista();
        if (index >= 0 && index < tamanos.size() && tamanos.get(index) != null) {
            return formatearTamanoArchivo(tamanos.get(index));
        }

        try {
            Path path = storageService.resolveStoredPath(rutaRelativa);
            long size = Files.exists(path) ? Files.size(path) : 0L;
            return formatearTamanoArchivo(size);
        } catch (Exception exception) {
            return null;
        }
    }

    private String resolverNombreArchivo(PedidoCopisteria pedido, int index, String ruta) {
        List<String> nombres = pedido.getNombresArchivo();
        if (index >= 0 && index < nombres.size() && nombres.get(index) != null && !nombres.get(index).isBlank()) {
            return nombres.get(index);
        }

        try {
            if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
                return java.net.URLDecoder.decode(
                    Path.of(java.net.URI.create(ruta).getPath()).getFileName().toString(),
                    java.nio.charset.StandardCharsets.UTF_8
                );
            }
            return Path.of(ruta).getFileName().toString();
        } catch (Exception exception) {
            return ruta;
        }
    }

    private String formatearTamanoArchivo(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return null;
        }
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        }
        double sizeInKb = sizeInBytes / 1024.0;
        if (sizeInKb < 1024) {
            return String.format("%.1f KB", sizeInKb);
        }
        return String.format("%.2f MB", sizeInKb / 1024.0);
    }

    private int resolverPaginasTotales(PedidoCopisteria pedido) {
        int totalDesdeMetadatos = pedido.getTotalPageCount();
        if (totalDesdeMetadatos > 0) {
            return totalDesdeMetadatos;
        }

        int total = 0;

        for (String ruta : pedido.getRutasArchivo()) {
            try {
                Path path = storageService.resolveStoredPath(ruta);
                String filename = path.getFileName().toString().toLowerCase();

                if (filename.endsWith(".pdf")) {
                    try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(path.toFile())) {
                        total += Math.max(document.getNumberOfPages(), 1);
                    }
                } else {
                    total += 1;
                }
            } catch (Exception ignored) {
            }
        }

        return total;
    }

    private String resolverMetodoEntrega(PedidoCopisteria pedido) {
        String value = extraValue(pedido.getExtras(), "deliveryMethod");
        return "HOME_DELIVERY".equalsIgnoreCase(value) ? "Envio a domicilio" : "Recogida en tienda";
    }

    private String resolverDireccionEntrega(PedidoCopisteria pedido) {
        String value = extraValue(pedido.getExtras(), "deliveryAddress");
        return value == null || value.isBlank() ? null : value;
    }

    private String resolverVentanaUrgente(PedidoCopisteria pedido) {
        boolean urgente = "true".equalsIgnoreCase(extraValue(pedido.getExtras(), "urgente"));
        if (!urgente) {
            return null;
        }
        return "HOME_DELIVERY".equalsIgnoreCase(extraValue(pedido.getExtras(), "deliveryMethod"))
            ? "Entrega urgente estimada en 20 minutos"
            : "Recogida urgente estimada en 10 minutos";
    }

    private String extraValue(String extras, String key) {
        if (extras == null || extras.isBlank()) {
            return null;
        }

        String quotedPrefix = key + "='";
        int quotedStart = extras.indexOf(quotedPrefix);
        if (quotedStart >= 0) {
            int valueStart = quotedStart + quotedPrefix.length();
            int valueEnd = extras.indexOf("'", valueStart);
            return valueEnd >= valueStart ? extras.substring(valueStart, valueEnd) : null;
        }

        String plainPrefix = key + "=";
        int plainStart = extras.indexOf(plainPrefix);
        if (plainStart >= 0) {
            int valueStart = plainStart + plainPrefix.length();
            int valueEnd = extras.indexOf(",", valueStart);
            return valueEnd >= valueStart ? extras.substring(valueStart, valueEnd) : extras.substring(valueStart);
        }

        return null;
    }

    private String resolverResumenExtras(PedidoCopisteria pedido) {
        List<String> extras = new ArrayList<>();
        if ("true".equalsIgnoreCase(extraValue(pedido.getExtras(), "plastificado"))) {
            extras.add("Plastificado");
        }
        if ("true".equalsIgnoreCase(extraValue(pedido.getExtras(), "urgente"))) {
            extras.add("Urgente");
        }
        if ("true".equalsIgnoreCase(extraValue(pedido.getExtras(), "escaneado"))) {
            extras.add("Escaneado");
        }
        return extras.isEmpty() ? "Sin extras" : String.join(", ", extras);
    }

    private String resolverObservaciones(PedidoCopisteria pedido) {
        String value = extraValue(pedido.getExtras(), "observaciones");
        return value == null || value.isBlank() ? "Sin observaciones." : value;
    }

    private boolean validarDireccionEntrega(FormularioPedidoCopisteria orderForm, BindingResult result) {
        if (orderForm.getDeliveryAddress() == null || orderForm.getDeliveryAddress().isBlank()) {
            result.rejectValue("deliveryAddress", "deliveryAddress.required", "Debes indicar la dirección si eliges envío a domicilio.");
            return false;
        }

        String street = extraerLineaDireccion(orderForm.getDeliveryAddress(), "Calle:");
        String number = extraerLineaDireccion(orderForm.getDeliveryAddress(), "Número:");
        String postalCode = extraerLineaDireccion(orderForm.getDeliveryAddress(), "Código postal:");
        String city = extraerLineaDireccion(orderForm.getDeliveryAddress(), "Ciudad:");
        String province = extraerLineaDireccion(orderForm.getDeliveryAddress(), "Provincia:");
        String contactPhone = extraerLineaDireccion(orderForm.getDeliveryAddress(), "Teléfono de contacto:");

        boolean valid = true;

        if (street.isBlank()) {
            result.rejectValue("deliveryAddress", "deliveryStreet.required", "Introduce la calle de entrega.");
            valid = false;
        }
        if (number.isBlank()) {
            result.rejectValue("deliveryAddress", "deliveryNumber.required", "Introduce el número de la dirección.");
            valid = false;
        }
        if (!postalCode.matches("\\d{5}")) {
            result.rejectValue("deliveryAddress", "deliveryPostalCode.invalid", "Introduce un código postal válido de 5 dígitos.");
            valid = false;
        }
        if (city.isBlank()) {
            result.rejectValue("deliveryAddress", "deliveryCity.required", "Introduce la ciudad de entrega.");
            valid = false;
        } else {
            String normalizedCity = normalizeCity(city);
            if (!"torrejon de ardoz".equals(normalizedCity) && !"torrejon".equals(normalizedCity)) {
                result.rejectValue("deliveryAddress", "deliveryCity.unsupported", "Solo repartimos en Torrejón de Ardoz.");
                valid = false;
            }
        }
        if (province.isBlank()) {
            result.rejectValue("deliveryAddress", "deliveryProvince.required", "Introduce la provincia.");
            valid = false;
        }
        if (!contactPhone.matches("\\d{9}")) {
            result.rejectValue("deliveryAddress", "deliveryContactPhone.invalid", "Introduce un teléfono de contacto válido de 9 dígitos.");
            valid = false;
        }

        return valid;
    }

    private String extraerLineaDireccion(String address, String prefix) {
        if (address == null || address.isBlank()) {
            return "";
        }

        return address.lines()
            .filter(line -> line.startsWith(prefix))
            .findFirst()
            .map(line -> line.substring(prefix.length()).trim())
            .orElse("");
    }

    private String normalizeCity(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT);
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos() {
        return "area-personal/pedidos";
    }


@PostMapping("/precio-orientativo")
    @ResponseBody
    public RespuestaVistaPreviaPrecioCopisteria precioOrientativo(
            HttpServletRequest request,
            @RequestParam(value = "archivo", required = false) MultipartFile[] files) {
        
        try {
            // Parse form fields
            TipoTrabajo jobType = TipoTrabajo.valueOf(request.getParameter("jobType"));
            ModoColor colorMode = request.getParameter("colorMode") != null ? ModoColor.valueOf(request.getParameter("colorMode")) : ModoColor.BLACK_AND_WHITE;
            int copies = Integer.parseInt(request.getParameter("copies") != null ? request.getParameter("copies") : "1");
            copies = Math.max(1, Math.min(FormularioPedidoCopisteria.MAX_COPIES, copies));
            int fileCount = files != null ? files.length : 0;
            int pageCount = fileCount; // Simple preview

            // Calc total (mirror JS basic print)
            double basePrice = colorMode == ModoColor.COLOR ? 0.45 : 0.06;
            BigDecimal total = BigDecimal.valueOf(basePrice * pageCount * copies).setScale(2, RoundingMode.HALF_UP);
            
            List<LineaPrecioCopisteria> lines = List.of(
                new LineaPrecioCopisteria(
                    jobType.name() + " " + colorMode.name(), 
                    pageCount + " páginas x " + copies + " copias", 
                    total
                )
            );

            String note = "Precio orientativo calculado. Sube archivos para páginas exactas.";
            String breakdown = jobType.name() + " · " + colorMode + " · " + copies + " copias";

            EstimacionPrecioCopisteria estimate = new EstimacionPrecioCopisteria(
                total, breakdown, note, fileCount, pageCount, lines
            );

            // Use record from() if available, else manual
            return RespuestaVistaPreviaPrecioCopisteria.from(estimate);
        } catch (Exception e) {
            // Fallback empty
            return new RespuestaVistaPreviaPrecioCopisteria(
                "0,00 EUR", "Error en configuración", "Completa el formulario", 0, 0, "0 páginas", List.of()
            );
        }
    }
}
