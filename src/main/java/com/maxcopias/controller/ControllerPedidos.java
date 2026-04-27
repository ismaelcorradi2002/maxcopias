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
import java.util.UUID;

@Controller
@RequestMapping("/copisteria")
public class ControllerPedidos {

    @Autowired
    private RepositorioPedidoCopisteria pedidoRepository;

    @Autowired
    private ServicioAlmacenamientoArchivos storageService;

    @Autowired
    private ServicioUsuario userService;

    @InitBinder("orderForm")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("files"); // Prevent binding issues with file uploads
    }

@GetMapping({"", "/pedido", "/formulario"})
    public String formulario(@ModelAttribute("orderForm") FormularioPedidoCopisteria orderForm, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            Usuario user = userService.findRequiredByEmail(username);
            orderForm.setCustomerName(user.getFirstName() + " " + user.getLastName());
            orderForm.setPhone(user.getPhone());
            orderForm.setEmail(user.getEmail());
        }
        // Pre-select defaults for smooth JS wizard progression
        orderForm.setJobType(TipoTrabajo.IMPRESION);
        orderForm.setColorMode(ModoColor.BLACK_AND_WHITE);
        orderForm.setPaperSize(TamanoPapel.A4);
        orderForm.setPrintSide(CaraImpresion.ONE_SIDED);
        orderForm.setPaperType(TipoPapel.NORMAL);
        orderForm.setBindingType(TipoEncuadernacion.SIN_ENCUADERNACION);
        orderForm.setCopies(1);
        
        // Populate enum lists for template dropdowns
        model.addAttribute("primaryJobTypes", TipoTrabajo.values());
        model.addAttribute("colorModes", ModoColor.values());
        model.addAttribute("paperSizes", TamanoPapel.values());
        model.addAttribute("printSides", CaraImpresion.values());
        model.addAttribute("paperTypes", TipoPapel.values());
        model.addAttribute("bindingTypes", TipoEncuadernacion.values());
        
        // Static preview and config
        model.addAttribute("acceptedFormats", "PDF, DOC, DOCX, JPG, JPEG, PNG");
        model.addAttribute("maxFileSizeLabel", "20 MB");
        
        // Empty price preview (will be populated by JS/service)
        model.addAttribute("pricePreview", new com.maxcopias.model.EstimacionPrecioCopisteria(
            java.math.BigDecimal.ZERO, 
            "Selecciona un servicio para ver el precio orientativo del pedido.", 
            "El importe se calcula automaticamente segun la configuracion y los archivos.", 
            0, 0
        ));
        
        return "copisteria/formulario";
    }

    @PostMapping
    public String procesarPedido(
            @Valid @ModelAttribute("orderForm") FormularioPedidoCopisteria orderForm,
            BindingResult result,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            Model model) {
        
        if (result.hasErrors()) {
            // Re-populate model attributes on validation error
            formulario(orderForm, model);
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
        String extrasStr = String.format("plastificado=%b,urgente=%b,escaneado=%b,observaciones='%s'",
            orderForm.getPlastificado(), orderForm.getUrgente(), orderForm.getEscaneado(), orderForm.getObservations());
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

        if (archivo == null || archivo.isEmpty()) {
            result.reject("archivo.required", "Debes adjuntar un archivo valido para guardar el pedido.");
            formulario(orderForm, model);
            return "copisteria/formulario";
        }

        PedidoCopisteria savedPedido = null;
        try {
            pedido.setCodigoRecoger(generarCodigoRecogerUnico());
            savedPedido = pedidoRepository.saveAndFlush(pedido);
            DatosArchivoGuardado archivoGuardado = storageService.storeOrderFile(archivo, savedPedido.getCodigoRecoger());
            savedPedido.setRutaArchivo(archivoGuardado.getRelativePath());
            pedidoRepository.saveAndFlush(savedPedido);
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
        model.addAttribute("totalPageCount", pedido.getTotalPageCount());
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
        model.addAttribute("extrasSummary", pedido.getExtrasSummary());
        model.addAttribute("observations", pedido.getObservationsOrDefault());
        model.addAttribute("priceBreakdown", pedido.getPriceBreakdownOrDefault());
        model.addAttribute("rutaArchivo", pedido.getRutaArchivo());
        model.addAttribute("nombreArchivo", pedido.getNombreArchivo());
        model.addAttribute("tamanoArchivoFormateado", pedido.getTamanoArchivoFormateado());
        model.addAttribute("archivoVerUrl", "/pedidos/copisteria/" + pedido.getId() + "/archivo");
        model.addAttribute("archivoDescargaUrl", "/pedidos/copisteria/" + pedido.getId() + "/archivo?download=true");
        return "copisteria/resumen-simple";
    }

    private String generarCodigoRecogerUnico() {
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (pedidoRepository.existsByCodigoRecoger(codigo));
        return codigo;
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
            int fileCount = files != null ? files.length : 0;
            int pageCount = fileCount; // Simple preview

            // Calc total (mirror JS basic print)
            double basePrice = colorMode == ModoColor.COLOR ? 0.45 : 0.06;
            BigDecimal total = BigDecimal.valueOf(basePrice * pageCount * copies).setScale(2, RoundingMode.HALF_UP);
            
            List<LineaPrecioCopisteria> lines = List.of(
                new LineaPrecioCopisteria(
                    jobType.name() + " " + colorMode.name(), 
                    pageCount + " paginas x " + copies + " copias", 
                    total
                )
            );

            String note = "Precio orientativo calculado. Sube archivos para paginas exactas.";
            String breakdown = jobType.name() + " • " + colorMode + " • " + copies + " copias";

            EstimacionPrecioCopisteria estimate = new EstimacionPrecioCopisteria(
                total, breakdown, note, fileCount, pageCount, lines
            );

            // Use record from() if available, else manual
            return RespuestaVistaPreviaPrecioCopisteria.from(estimate);
        } catch (Exception e) {
            // Fallback empty
            return new RespuestaVistaPreviaPrecioCopisteria(
                "0,00 EUR", "Error en config", "Completa el formulario", 0, 0, "0 paginas", List.of()
            );
        }
    }
}
