package com.bedantas.personregistry.domain;

import java.util.regex.Pattern;

/** Primeiro nome da pessoa. Value Object: imutavel e sempre valido. */
public record Nome(String valor) {

    private static final Pattern LETRAS = Pattern.compile("^[\\p{L} '-]+$");
    /** Visiveis ao pacote para que os testes acompanhem o limite escolhido. */
    static final int MINIMO = 2;
    static final int MAXIMO = 20;

    public Nome {
        valor = validar(valor, "nome");
    }

    /**
     * Regras compartilhadas com Sobrenome: sao identicas, muda apenas o nome do
     * campo citado na mensagem. Sem isso, um erro no sobrenome respondia
     * "nome contem caracteres invalidos" e o cliente nao sabia o que corrigir.
     */
    static String validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ErroDeDominio.DadoInvalido(campo + " e obrigatorio");
        }
        valor = valor.trim();
        if (valor.length() < MINIMO || valor.length() > MAXIMO) {
            throw new ErroDeDominio.DadoInvalido(
                    campo + " deve ter de " + MINIMO + " a " + MAXIMO + " caracteres");
        }
        if (!LETRAS.matcher(valor).matches()) {
            throw new ErroDeDominio.DadoInvalido(
                    campo + " contem caracteres invalidos: " + valor);
        }
        return valor;
    }
}
