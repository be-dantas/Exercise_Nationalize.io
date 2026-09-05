package com.bedantas.prova.domain;

/**
 * Entity cuja identidade e o documento.
 *
 * Modelada como record porque neste escopo ela e imutavel e nunca e comparada
 * por igualdade - o repositorio a indexa pelo documento. Isso economiza codigo
 * sem prejuizo semantico.
 *
 * Nao ha validacao aqui: cada campo ja e um Value Object que so existe valido.
 */
public record Pessoa(Documento documento, Nome nome, Nome sobrenome, Email email) {
}
