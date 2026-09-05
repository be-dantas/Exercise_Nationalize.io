package com.bedantas.prova.domain;

/** A API de terceiro falhou, expirou ou recusou. Vira HTTP 503. */
public class ServicoExternoIndisponivelException extends RuntimeException {

    public ServicoExternoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
