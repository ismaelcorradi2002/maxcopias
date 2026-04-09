package com.maxcopias.dto;

import com.maxcopias.model.ModoColor;
import com.maxcopias.model.TipoTrabajo;
import com.maxcopias.model.TamanoPapel;
import com.maxcopias.model.CaraImpresion;
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

    @Size(max = 600, message = "Las observaciones no pueden superar los 600 caracteres.")
    private String observations;

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

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}

