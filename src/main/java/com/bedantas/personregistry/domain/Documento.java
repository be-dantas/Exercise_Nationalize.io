package com.bedantas.personregistry.domain;

import java.util.regex.Pattern;

/**
 * CPF da pessoa. Value Object: imutavel e sempre valido.
 *
 * A forma bruta e validada ANTES da normalizacao, de proposito: se a limpeza
 * viesse primeiro, uma entrada como "<script>alert(1)</script>" seria reduzida
 * a digitos e poderia passar. So os separadores que o CPF realmente usa
 * (ponto e hifen) sao aceitos e removidos.
 *
 * A validacao e especifica do Brasil por decisao de escopo. Trocar por outro
 * pais significa mudar apenas este arquivo - nenhum caso de uso, controller ou
 * teste de outra camada e afetado.
 */
public record Documento(String valor) {

    private static final int TAMANHO_MAXIMO_ACEITO = 20;
    private static final Pattern SEPARADORES_DO_CPF = Pattern.compile("^[0-9.\\-]+$");
    private static final Pattern ONZE_DIGITOS = Pattern.compile("^\\d{11}$");

    public Documento {
        if (valor == null || valor.isBlank()) {
            throw new ErroDeDominio.DadoInvalido("documento e obrigatorio");
        }
        valor = valor.trim();

        if (valor.length() > TAMANHO_MAXIMO_ACEITO) {
            throw new ErroDeDominio.DadoInvalido("documento longo demais");
        }
        if (!SEPARADORES_DO_CPF.matcher(valor).matches()) {
            throw new ErroDeDominio.DadoInvalido(
                    "documento deve conter apenas digitos, ponto e hifen");
        }
        valor = valor.replaceAll("[.\\-]", "");

        if (!ONZE_DIGITOS.matcher(valor).matches()) {
            throw new ErroDeDominio.DadoInvalido("CPF deve ter 11 digitos");
        }
        // 00000000000, 11111111111 e afins satisfazem o calculo do digito
        // verificador, mas nao sao CPFs validos. Precisam de rejeicao propria.
        if (valor.chars().distinct().count() == 1) {
            throw new ErroDeDominio.DadoInvalido("CPF invalido: digitos repetidos");
        }
        if (!digitosVerificadoresConferem(valor)) {
            throw new ErroDeDominio.DadoInvalido(
                    "CPF invalido: digito verificador nao confere");
        }
    }

    /** Algoritmo oficial do CPF: dois digitos verificadores por modulo 11. */
    private static boolean digitosVerificadoresConferem(String cpf) {
        int primeiro = digitoVerificador(cpf, 9, 10);
        int segundo = digitoVerificador(cpf, 10, 11);
        return primeiro == cpf.charAt(9) - '0' && segundo == cpf.charAt(10) - '0';
    }

    private static int digitoVerificador(String cpf, int quantosDigitos, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < quantosDigitos; i++) {
            soma += (cpf.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
