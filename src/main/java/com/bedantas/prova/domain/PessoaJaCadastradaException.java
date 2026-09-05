package com.bedantas.prova.domain;

/** Documento ja existe no sistema. Vira HTTP 409 (conflito, nao erro de formato). */
public class PessoaJaCadastradaException extends RuntimeException {

    public PessoaJaCadastradaException(String documento) {
        super("ja existe uma pessoa cadastrada com o documento " + documento);
    }
}
