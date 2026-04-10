package com.maxcopias.controller;

/**
 * Controlador principal de servicios de copistería (formularios, precios, pedidos).
 */
import com.maxcopias.config.PropiedadesMaxcopias;
import com.maxcopias.dto.FormularioPedidoCopisteria;
import com.maxcopias.dto.RespuestaVistaPreviaPrecioCopisteria;
import com.maxcopias.model.ModoColor;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.AnalisisArchivoSubido;
import com.maxcopias.model.TipoTrabajo;
import com.maxcopias.model.TamanoPapel;
import com.maxcopias.model.CaraImpresion;
import com.maxcopias.model.Rol;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioPedidoCopisteria;
import com.maxcopias.service.ServicioPrecioCopisteria;
import com.maxcopias.service.ServicioInspeccionArchivos;
import com.maxcopias.service.ExcepcionAlmacenamientoArchivos;
import com.maxcopias.service.ServicioUsuario;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/copisteria")
public class ControladorCopisteria {

    private final ServicioPedidoCopisteria orderService;
    private final ServicioPrecioCopisteria pricingService;
    private final ServicioInspeccionArchivos fileInspectionService;
    private final PropiedadesMaxcopias properties;
    private final ServicioUsuario userService;

    /**
     * Inyecta servicios de copistería, precios, archivos, propiedades y usuarios.
     */
    public ControladorCopisteria(
        ServicioPedidoCopisteria orderService,
        ServicioPrecioCopisteria pricingService,
        ServicioInspeccionArchivos fileInspectionService,
        PropiedadesMaxcopias properties,
        ServicioUsuario userService
    ) {
        this.orderService = orderService;
        this.pricingService = pricingService;
        this.fileInspectionService = fileInspectionService;
        this.properties = properties;
        this.userService = userService;
    }

    @ModelAttribute("jobTypes")
    public TipoTrabajo[] jobTypes() {
        return TipoTrabajo.values();
    }

    @ModelAttribute("colorModes")
    public ModoColor[] colorModes() {
        return ModoColor.values();
    }

    @ModelAttribute("printSides")
    public CaraImpresion[] printSides() {
        return CaraImpresion.values();
    }

    @ModelAttribute("paperSizes")
    public TamanoPapel[] paperSizes() {
        return TamanoPapel.values();
    }

    @ModelAttribute("acceptedFormats")
    public String acceptedFormats() {
        return properties.getAllowedExtensions().stream()
            .map(extension -> extension.toUpperCase(Locale.ROOT))
            .collect(Collectors.joining(", "));
    }

    @ModelAttribute("maxFiles")
    public int maxFiles() {
        return properties.getMaxFiles();
    }

    @ModelAttribute("maxFileSizeLabel")
    public String maxFileSizeLabel() {
        return properties.getMaxFileSize().toMegabytes() + " MB";
    }

    @GetMapping
    public String showCopisteriaLanding() {
        return "copisteria/servicios";
    }

    @GetMapping("/pedido")
    public String showOrderForm(
        Authentication authentication,
        Model model,
        @RequestParam(name = "jobType", required = false) TipoTrabajo selectedTipoTrabajo
    ) {
        FormularioPedidoCopisteria orderForm;

        if (!model.containsAttribute("orderForm")) {
            Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
            orderForm = buildDefaultForm(currentUsuario, selectedTipoTrabajo);
            model.addAttribute("orderForm", orderForm);
        } else {
            orderForm = (FormularioPedidoCopisteria) model.asMap().get("orderForm");
        }

        model.addAttribute("pricePreview", pricingService.calculate(orderForm, 0, 0));
        return "copisteria/formulario";
    }

    @PostMapping("/pedido")
    public String submitOrder(
        Authentication authentication,
        @Valid @ModelAttribute("orderForm") FormularioPedidoCopisteria orderForm,
        BindingResult bindingResult,
        Model model,
        @RequestParam(name = "files", required = false) List<MultipartFile> files
    ) {
        validateConditionalFields(orderForm, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("pricePreview", pricingService.calculate(orderForm, countProvidedFiles(files), 0));
            return "copisteria/formulario";
        }

        try {
            Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
            PedidoCopisteria order = orderService.createOrder(currentUsuario, orderForm, files);
            return "redirect:/copisteria/resumen/" + order.getPickupCode();
        } catch (ExcepcionAlmacenamientoArchivos exception) {
            bindingResult.reject("files", exception.getMessage());
            model.addAttribute("pricePreview", pricingService.calculate(orderForm, countProvidedFiles(files), 0));
            return "copisteria/formulario";
        }
    }

    @PostMapping(value = "/precio-orientativo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> previewPrice(
        @ModelAttribute FormularioPedidoCopisteria orderForm,
        @RequestParam(name = "files", required = false) List<MultipartFile> files
    ) {
        try {
            List<AnalisisArchivoSubido> analyzedFiles = fileInspectionService.inspectPreviewFiles(files);
            int fileCount = analyzedFiles.size();
            int totalPages = analyzedFiles.stream()
                .mapToInt(AnalisisArchivoSubido::getPageCount)
                .sum();

            return ResponseEntity.ok(
                RespuestaVistaPreviaPrecioCopisteria.from(pricingService.calculate(orderForm, fileCount, totalPages))
            );
        } catch (ExcepcionAlmacenamientoArchivos exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/resumen/{reference}")
    public String showSummary(@PathVariable String reference, Authentication authentication, Model model) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(Rol.ROLE_ADMIN.name()));

        PedidoCopisteria order = (isAdmin
            ? orderService.findOrderForAdmin(reference)
            : orderService.findOrderForUsuario(reference, currentUsuario))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("order", order);
        return "copisteria/resumen";
    }

    private FormularioPedidoCopisteria buildDefaultForm(Usuario currentUsuario, TipoTrabajo selectedTipoTrabajo) {
        FormularioPedidoCopisteria form = new FormularioPedidoCopisteria();
        form.setCustomerName(currentUsuario.getFullName());
        form.setPhone(currentUsuario.getPhone());
        form.setEmail(currentUsuario.getEmail());
        form.setTipoTrabajo(selectedTipoTrabajo != null ? selectedTipoTrabajo : TipoTrabajo.IMPRESION);

        if (form.getTipoTrabajo().isRequiresPrintConfiguration()) {
            form.setCopies(1);
            form.setModoColor(ModoColor.BLACK_AND_WHITE);
            form.setCaraImpresion(CaraImpresion.ONE_SIDED);
            form.setPaperSize(TamanoPapel.A4);
        }

        return form;
    }

    private void validateConditionalFields(FormularioPedidoCopisteria form, BindingResult bindingResult) {
        if (form.getTipoTrabajo() == null) {
            return;
        }

        if (form.getTipoTrabajo().isRequiresPrintConfiguration()) {
            if (form.getCopies() == null) {
                bindingResult.rejectValue("copies", "copies.required", "Indica el numero de copias.");
            }
            if (form.getModoColor() == null) {
                bindingResult.rejectValue("colorMode", "colorMode.required", "Selecciona si lo quieres en color o en blanco y negro.");
            }
            if (form.getCaraImpresion() == null) {
                bindingResult.rejectValue("printSide", "printSide.required", "Selecciona si lo quieres a una cara o doble cara.");
            }
            if (form.getPaperSize() == null) {
                bindingResult.rejectValue("paperSize", "paperSize.required", "Selecciona el tamano del papel.");
            }
        } else {
            form.setCopies(null);
            form.setModoColor(null);
            form.setCaraImpresion(null);
            form.setPaperSize(null);
        }
    }

    private int countProvidedFiles(List<MultipartFile> files) {
        if (files == null) {
            return 0;
        }

        return (int) files.stream()
            .filter(file -> file != null && !file.isEmpty())
            .count();
    }
}

