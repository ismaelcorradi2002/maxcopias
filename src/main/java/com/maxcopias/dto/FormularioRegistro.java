package com.maxcopias.dto;

/**
 * DTO para registro de nuevo usuario.
 */
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FormularioRegistro {

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

    @NotBlank(message = "Introduce un email.")
    @Email(message = "Introduce un email valido.")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres.")
    private String email;

    @NotBlank(message = "Introduce un telefono.")
    @Pattern(regexp = "^\\d{9}$", message = "Introduce un telefono valido de 9 digitos.")
    private String phone;

    @NotBlank(message = "Introduce una contrasena.")
    @Size(min = 6, max = 72, message = "La contrasena debe tener entre 6 y 72 caracteres.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*(?:\\d|[^A-Za-z\\d])).{6,72}$",
        message = "La contrasena debe tener al menos 6 caracteres, una mayuscula, una minuscula y un numero o simbolo."
    )
    private String password;

    @NotBlank(message = "Confirma la contrasena.")
    private String confirmPassword;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}

