package com.maxcopias.service;

import java.security.SecureRandom;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class GeneradorCodigoPedido {

    private static final char[] SAFE_CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 200;

    private final SecureRandom random = new SecureRandom();

    public String generarCodigoCopisteria(Predicate<String> existeCodigo) {
        return generarCodigoUnico("C", existeCodigo);
    }

    public String generarCodigoTienda(Predicate<String> existeCodigo) {
        return generarCodigoUnico("T", existeCodigo);
    }

    private String generarCodigoUnico(String prefijo, Predicate<String> existeCodigo) {
        for (int intento = 0; intento < MAX_ATTEMPTS; intento++) {
            String codigo = prefijo + "-" + generarBloqueAleatorio();
            if (!existeCodigo.test(codigo)) {
                return codigo;
            }
        }

        throw new IllegalStateException("No se pudo generar un codigo unico de pedido.");
    }

    private String generarBloqueAleatorio() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            builder.append(SAFE_CHARS[random.nextInt(SAFE_CHARS.length)]);
        }
        return builder.toString();
    }
}
