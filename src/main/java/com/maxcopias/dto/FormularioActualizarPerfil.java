package com.maxcopias.dto;

/**
 * DTO para actualizar perfil de usuario (nombre, apellidos, teléfono).
 */
import com.maxcopias.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FormularioActualizarPerfil {

    @NotBlank(message = "Introduce tu nombre.")
    @Pattern(
        regexp = "^[\\p{L}]+(?:[ -][\\p{L}]+)*$",
        message = "El nombre solo puede contener letras, espacios y guiones."
    )
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres.")
    private String firstName;

    @NotBlank(message = "Introduce tus apellidos.")
    @Pattern(
        regexp = "^[\\p{L}]+(?:[ -][\\p{L}]+)*$",
        message = "Los apellidos solo pueden contener letras, espacios y guiones."
    )
    @Size(max = 120, message = "Los apellidos no pueden superar los 120 caracteres.")
    private String lastName;

    @NotBlank(message = "Introduce un telefono.")
    @Pattern(regexp = "^\\d{9}$", message = "Introduce un telefono valido de 9 digitos.")
    private String phone;

    public static FormularioActualizarPerfil fromUsuario(Usuario user) {
        FormularioActualizarPerfil form = new FormularioActualizarPerfil();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setPhone(user.getPhone());
        return form;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

