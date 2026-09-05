package com.bedantas.prova.domain;

import java.util.regex.Pattern;


/** Nome ou sobrenome. Value Object: imutavel e sempre valido. */
public record Nome(String valor) {

    private static final Pattern LETRAS = Pattern.compile("^[\\p{L} '-]+$");

    public Nome {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("nome e obrigatorio");
        }
        valor = valor.trim();
        if (valor.length() < 2 || valor.length() > 80) {
            throw new DadoInvalidoException("nome deve ter de 2 a 80 caracteres");
        }
        if (!LETRAS.matcher(valor).matches()) {
            throw new DadoInvalidoException("nome contem caracteres invalidos: " + valor);
        }
    }
}
