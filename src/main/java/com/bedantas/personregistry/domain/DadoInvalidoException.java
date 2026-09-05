package com.bedantas.personregistry.domain;

/** Dado que nao satisfaz as regras de formato do dominio. Vira HTTP 400. */
public class DadoInvalidoException extends RuntimeException {

    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
