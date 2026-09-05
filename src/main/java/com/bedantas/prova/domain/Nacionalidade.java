package com.bedantas.prova.domain;

/**
 * Previsao de nacionalidade. Value Object.
 *
 * A API externa devolve codigo ISO ("US"); o enunciado pede o NOME do pais,
 * entao a conversao e responsabilidade nossa e acontece no adapter.
 */
public record Nacionalidade(String codigoIso, String nomeDoPais, double probabilidade) {
}
