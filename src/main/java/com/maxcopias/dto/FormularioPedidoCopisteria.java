package com.maxcopias.dto;

/**
 * DTO para formulario de pedido de copistería.
 */
import com.maxcopias.model.ModoColor;
import com.maxcopias.model.TipoTrabajo;
import com.maxcopias.model.TamanoPapel;
import com.maxcopias.model.CaraImpresion;
import com.maxcopias.model.TipoEncuadernacion;
import com.maxcopias.model.TipoPapel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FormularioPedidoCopisteria {

    @NotBlank(message = "Introduce tu nombre.")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres.")
    private String customerName;

    @NotBlank(message = "Introduce un telefono de contacto.")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Introduce un telefono valido.")
    private String phone;

    @NotBlank(message = "Introduce un email.")
    @Email(message = "Introduce un email valido.")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres.")
    private String email;

    @NotNull(message = "Selecciona el tipo de trabajo.")
    private TipoTrabajo jobType;

    @Min(value = 1, message = "El numero de copias debe ser al menos 1.")
    @Max(value = 5000, message = "El numero de copias no puede superar 5000.")
    private Integer copies;

    private ModoColor colorMode;

    private CaraImpresion printSide;

    private TamanoPapel paperSize;

    private TipoPapel paperType;

    private TipoEncuadernacion bindingType;

    private Boolean plastificado = Boolean.FALSE;

    private Boolean urgente = Boolean.FALSE;

    private Boolean escaneado = Boolean.FALSE;

    private String deliveryMethod = "STORE_PICKUP";

    @Size(max = 240, message = "La direccion no puede superar los 240 caracteres.")
    private String deliveryAddress;

    @Size(max = 600, message = "Las observaciones no pueden superar los 600 caracteres.")
    private String observations;

    private String estimatedPrice;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TipoTrabajo getTipoTrabajo() {
        return jobType;
    }

    public void setTipoTrabajo(TipoTrabajo jobType) {
        this.jobType = jobType;
    }

    public TipoTrabajo getJobType() {
        return jobType;
    }

    public void setJobType(TipoTrabajo jobType) {
        this.jobType = jobType;
    }

    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
    }

    public ModoColor getModoColor() {
        return colorMode;
    }

    public void setModoColor(ModoColor colorMode) {
        this.colorMode = colorMode;
    }

    public ModoColor getColorMode() {
        return colorMode;
    }

    public void setColorMode(ModoColor colorMode) {
        this.colorMode = colorMode;
    }

    public CaraImpresion getCaraImpresion() {
        return printSide;
    }

    public void setCaraImpresion(CaraImpresion printSide) {
        this.printSide = printSide;
    }

    public CaraImpresion getPrintSide() {
        return printSide;
    }

    public void setPrintSide(CaraImpresion printSide) {
        this.printSide = printSide;
    }

    public TamanoPapel getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(TamanoPapel paperSize) {
        this.paperSize = paperSize;
    }

    public TipoPapel getTipoPapel() {
        return paperType;
    }

    public void setTipoPapel(TipoPapel paperType) {
        this.paperType = paperType;
    }

    public TipoPapel getPaperType() {
        return paperType;
    }

    public void setPaperType(TipoPapel paperType) {
        this.paperType = paperType;
    }

    public TipoEncuadernacion getTipoEncuadernacion() {
        return bindingType;
    }

    public void setTipoEncuadernacion(TipoEncuadernacion bindingType) {
        this.bindingType = bindingType;
    }

    public TipoEncuadernacion getBindingType() {
        return bindingType;
    }

    public void setBindingType(TipoEncuadernacion bindingType) {
        this.bindingType = bindingType;
    }

    public Boolean getPlastificado() {
        return plastificado;
    }

    public void setPlastificado(Boolean plastificado) {
        this.plastificado = plastificado;
    }

    public Boolean getUrgente() {
        return urgente;
    }

    public void setUrgente(Boolean urgente) {
        this.urgente = urgente;
    }

    public Boolean getEscaneado() {
        return escaneado;
    }

    public void setEscaneado(Boolean escaneado) {
        this.escaneado = escaneado;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(String estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }
}

