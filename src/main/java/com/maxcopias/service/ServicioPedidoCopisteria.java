package com.maxcopias.service;

/**
 * Servicio de gestión de pedidos de copistería (creación, consulta).
 */
import com.maxcopias.dto.FormularioPedidoCopisteria;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.ArchivoPedidoCopisteria;
import com.maxcopias.model.EstimacionPrecioCopisteria;
import com.maxcopias.model.EstadoPedidoCopisteria;
import com.maxcopias.model.DatosArchivoGuardado;
import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioPedidoCopisteria;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServicioPedidoCopisteria {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PICKUP_CODE_LENGTH = 6;

    private final RepositorioPedidoCopisteria orderRepository;
    private final ServicioAlmacenamientoArchivos fileStorageService;
    private final ServicioPrecioCopisteria pricingService;
    private final Random random = new Random();

    public ServicioPedidoCopisteria(
        RepositorioPedidoCopisteria orderRepository,
        ServicioAlmacenamientoArchivos fileStorageService,
        ServicioPrecioCopisteria pricingService
    ) {
        this.orderRepository = orderRepository;
        this.fileStorageService = fileStorageService;
        this.pricingService = pricingService;
    }

    @Transactional
    public PedidoCopisteria createOrder(Usuario user, FormularioPedidoCopisteria form, List<MultipartFile> files) {
        String pickupCode = generateUniquePickupCode();
        List<DatosArchivoGuardado> storedFiles = fileStorageService.storeFiles(files, pickupCode);
        int totalPages = storedFiles.stream()
            .mapToInt(DatosArchivoGuardado::getPageCount)
            .sum();
        EstimacionPrecioCopisteria priceEstimate = pricingService.calculate(form, storedFiles.size(), totalPages);

        PedidoCopisteria order = new PedidoCopisteria();
        order.setPickupCode(pickupCode);
        order.setUsuario(user);
        order.setCustomerName(normalizeValue(form.getCustomerName()));
        order.setPhone(normalizeValue(form.getPhone()));
        order.setEmail(normalizeValue(form.getEmail()));
        order.setTipoTrabajo(form.getTipoTrabajo());
        order.setCopies(form.getCopies());
        order.setModoColor(form.getModoColor());
        order.setCaraImpresion(form.getCaraImpresion());
        order.setPaperSize(form.getPaperSize());
        order.setTipoPapel(form.getTipoPapel());
        order.setTipoEncuadernacion(form.getTipoEncuadernacion());
        order.setPlastificado(Boolean.TRUE.equals(form.getPlastificado()));
        order.setUrgente(Boolean.TRUE.equals(form.getUrgente()));
        order.setEscaneado(Boolean.TRUE.equals(form.getEscaneado()));
        order.setObservations(normalizeOptionalValue(form.getObservations()));
        order.setStatus(EstadoPedidoCopisteria.RECIBIDO);
        order.setEstimatedPrice(priceEstimate.getTotal());
        order.setPriceBreakdown(priceEstimate.getBreakdown());

        storedFiles.forEach(file -> order.addFile(toOrderFile(file)));
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Optional<PedidoCopisteria> findOrderForUsuario(String pickupCode, Usuario user) {
        return orderRepository.findByPickupCodeAndUserId(pickupCode, user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<PedidoCopisteria> findOrderForAdmin(String pickupCode) {
        return orderRepository.findByPickupCode(pickupCode);
    }

    @Transactional(readOnly = true)
    public List<PedidoCopisteria> findOrdersForUsuario(Usuario user) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    private ArchivoPedidoCopisteria toOrderFile(DatosArchivoGuardado file) {
        ArchivoPedidoCopisteria orderFile = new ArchivoPedidoCopisteria();
        orderFile.setOriginalFilename(file.getOriginalFilename());
        orderFile.setStoredFilename(file.getStoredFilename());
        orderFile.setRelativePath(file.getRelativePath());
        orderFile.setContentType(file.getContentType());
        orderFile.setSizeInBytes(file.getSizeInBytes());
        orderFile.setPageCount(file.getPageCount());
        return orderFile;
    }

    private String generateUniquePickupCode() {
        for (int attempt = 0; attempt < 50; attempt++) {
            String candidate = "MC-" + randomCode();
            if (!orderRepository.existsByPickupCode(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("No se ha podido generar un codigo unico de recogida.");
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(PICKUP_CODE_LENGTH);

        for (int index = 0; index < PICKUP_CODE_LENGTH; index++) {
            builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }

        return builder.toString();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalValue(String value) {
        return value == null ? "" : value.trim();
    }
}

