package com.bedantas.personregistry.domain.valueobject;

import com.bedantas.personregistry.domain.error.ErroDeDominio;

import java.util.regex.Pattern;

public record Email(String valor) {

    private static final Pattern PADRAO =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");

    public Email {
        if (valor == null || valor.isBlank()) {
            throw new ErroDeDominio.DadoInvalido("e-mail e obrigatorio");
        }
        valor = valor.trim().toLowerCase();
        if (!PADRAO.matcher(valor).matches()) {
            throw new ErroDeDominio.DadoInvalido("e-mail invalido: " + valor);
        }
    }
}
