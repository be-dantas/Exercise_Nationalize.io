package com.bedantas.personregistry.domain.valueobject;

import com.bedantas.personregistry.domain.error.ErroDeDominio;

import java.util.regex.Pattern;

public record Nome(String valor) {

    private static final Pattern LETRAS = Pattern.compile("^[\\p{L} '-]+$");
    private static final int MINIMO = 2;
    /** Limite de uma PARTE do nome. Publico porque o teste da entidade o consulta. */
    public static final int MAXIMO = 50;

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
