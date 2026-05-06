package com.maxcopias.dto;

import com.maxcopias.model.MetodoEntregaPedidoTienda;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FormularioCheckoutTienda {

    @NotBlank(message = "Introduce tu nombre.")
    @Size(max = 160, message = "El nombre no puede superar 160 caracteres.")
    private String nombre;

    @NotBlank(message = "Introduce tu email.")
    @Email(message = "Introduce un email valido.")
    @Size(max = 140, message = "El email no puede superar 140 caracteres.")
    private String email;

    @NotBlank(message = "Introduce tu telefono.")
    @Size(max = 20, message = "El telefono no puede superar 20 caracteres.")
    private String telefono;

    @NotNull(message = "Selecciona un metodo de entrega.")
    private MetodoEntregaPedidoTienda metodoEntrega = MetodoEntregaPedidoTienda.RECOGIDA_TIENDA;

    @Size(max = 240, message = "La direccion no puede superar 240 caracteres.")
    private String direccionEntrega;

    @Pattern(regexp = "^$|^[0-9]{5}$", message = "Introduce un codigo postal valido de 5 digitos.")
    private String codigoPostalEntrega;

    @Size(max = 120, message = "La ciudad no puede superar 120 caracteres.")
    private String ciudadEntrega;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public MetodoEntregaPedidoTienda getMetodoEntrega() {
        return metodoEntrega;
    }

    public void setMetodoEntrega(MetodoEntregaPedidoTienda metodoEntrega) {
        this.metodoEntrega = metodoEntrega;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public String getCodigoPostalEntrega() {
        return codigoPostalEntrega;
    }

    public void setCodigoPostalEntrega(String codigoPostalEntrega) {
        this.codigoPostalEntrega = codigoPostalEntrega;
    }

    public String getCiudadEntrega() {
        return ciudadEntrega;
    }

    public void setCiudadEntrega(String ciudadEntrega) {
        this.ciudadEntrega = ciudadEntrega;
    }
}
