package com.bedantas.prova.domain;

import java.util.regex.Pattern;


/** Endereco de e-mail. Value Object: imutavel e sempre valido. */
public record Email(String valor) {

    private static final Pattern PADRAO =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");

    public Email {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("e-mail e obrigatorio");
        }
        valor = valor.trim().toLowerCase();
        if (!PADRAO.matcher(valor).matches()) {
            throw new DadoInvalidoException("e-mail invalido: " + valor);
        }
    }
}
