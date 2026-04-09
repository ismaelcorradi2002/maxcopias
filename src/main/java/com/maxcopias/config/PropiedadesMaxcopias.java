package com.maxcopias.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "maxcopias")
public class PropiedadesMaxcopias {

    @NotBlank
    private String uploadDir = "uploads/copisteria";

    @Min(1)
    private int maxFiles = 5;

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(15);

    @NotEmpty
    private List<String> allowedExtensions = new ArrayList<>(List.of("pdf", "jpg", "jpeg", "png"));

    @NotNull
    private Mail mail = new Mail();

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = new ArrayList<>(allowedExtensions);
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public static class Mail {
        private boolean welcomeEnabled = false;
        private String fromAddress = "no-reply@maxcopias.local";
        private String fromName = "Maxcopias";
        private String supportEmail = "info@maxcopias.es";
        private String supportPhone = "600 111 222";
        private String appUrl = "http://localhost:8080";

        public boolean isWelcomeEnabled() {
            return welcomeEnabled;
        }

        public void setWelcomeEnabled(boolean welcomeEnabled) {
            this.welcomeEnabled = welcomeEnabled;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getSupportEmail() {
            return supportEmail;
        }

        public void setSupportEmail(String supportEmail) {
            this.supportEmail = supportEmail;
        }

        public String getSupportPhone() {
            return supportPhone;
        }

        public void setSupportPhone(String supportPhone) {
            this.supportPhone = supportPhone;
        }

        public String getAppUrl() {
            return appUrl;
        }

        public void setAppUrl(String appUrl) {
            this.appUrl = appUrl;
        }
    }
}

