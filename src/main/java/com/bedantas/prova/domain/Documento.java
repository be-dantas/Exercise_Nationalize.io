package com.bedantas.prova.domain;

import java.util.regex.Pattern;


/**
 * Identificador da pessoa. Value Object: imutavel e sempre valido.
 *
 * A validacao e generica de proposito (6 a 20 alfanumericos apos normalizar).
 * O enunciado pede "documento", nao "CPF" - aceitar CPF, DNI, RUT ou passaporte
 * evita rejeitar documento estrangeiro legitimo. Um validador por pais plugaria
 * aqui sem mudar mais nada no sistema.
 *
 * A forma bruta e validada ANTES da normalizacao, de proposito: se a limpeza
 * viesse primeiro, uma entrada como "<script>alert(1)</script>" seria reduzida
 * a "scriptalert1script" e passaria como documento valido. So separadores que
 * documentos realmente usam (. - / espaco) sao aceitos e removidos.
 */
public record Documento(String valor) {

    private static final Pattern BRUTO = Pattern.compile("^[A-Za-z0-9.\\-/ ]{6,30}$");
    private static final Pattern NORMALIZADO = Pattern.compile("^[A-Za-z0-9]{6,20}$");

    public Documento {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("documento e obrigatorio");
        }
        valor = valor.trim();
        if (!BRUTO.matcher(valor).matches()) {
            throw new DadoInvalidoException("documento contem caracteres invalidos");
        }
        valor = valor.replaceAll("[.\\-/ ]", "");
        if (!NORMALIZADO.matcher(valor).matches()) {
            throw new DadoInvalidoException(
                    "documento deve ter de 6 a 20 caracteres alfanumericos");
        }
    }
}
