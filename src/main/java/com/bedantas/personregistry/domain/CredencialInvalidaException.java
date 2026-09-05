package com.bedantas.personregistry.domain;

/** Usuario ou senha nao conferem. Vira HTTP 401. */
public class CredencialInvalidaException extends RuntimeException {

    public CredencialInvalidaException() {
        super("usuario ou senha invalidos");
    }
}
