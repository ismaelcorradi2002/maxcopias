package com.maxcopias.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionMediaOfertas implements WebMvcConfigurer {

    private final PropiedadesMaxcopias properties;

    public ConfiguracionMediaOfertas(PropiedadesMaxcopias properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.getOfferImageUploadDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/ofertas/**")
            .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
