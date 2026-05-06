package com.maxcopias.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionMediaProductos implements WebMvcConfigurer {

    private final PropiedadesMaxcopias properties;

    public ConfiguracionMediaProductos(PropiedadesMaxcopias properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.getProductImageUploadDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/productos/**")
            .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
