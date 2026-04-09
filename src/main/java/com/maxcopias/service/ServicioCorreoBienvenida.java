package com.maxcopias.service;

import com.maxcopias.config.PropiedadesMaxcopias;
import com.maxcopias.model.Usuario;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class ServicioCorreoBienvenida {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioCorreoBienvenida.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SpringTemplateEngine templateEngine;
    private final PropiedadesMaxcopias maxcopiasProperties;

    public ServicioCorreoBienvenida(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        SpringTemplateEngine templateEngine,
        PropiedadesMaxcopias maxcopiasProperties
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.templateEngine = templateEngine;
        this.maxcopiasProperties = maxcopiasProperties;
    }

    public void sendWelcomeEmail(Usuario user) {
        PropiedadesMaxcopias.Mail mailProperties = maxcopiasProperties.getMail();

        if (!mailProperties.isWelcomeEnabled()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            LOGGER.warn("El email de bienvenida esta activado pero no hay JavaMailSender disponible.");
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("firstName", user.getFirstName());
            context.setVariable("fullName", user.getFullName());
            context.setVariable("loginUrl", buildUrl(mailProperties.getAppUrl(), "/login"));
            context.setVariable("supportEmail", mailProperties.getSupportEmail());
            context.setVariable("supportPhone", mailProperties.getSupportPhone());

            String html = templateEngine.process("correo/bienvenida", context);

            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setSubject("Bienvenido a Maxcopias");
            helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception exception) {
            LOGGER.warn("No se ha podido enviar el email de bienvenida a {}.", user.getEmail(), exception);
        }
    }

    private String buildUrl(String baseUrl, String path) {
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();

        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }

        return normalizedBase + path;
    }
}

