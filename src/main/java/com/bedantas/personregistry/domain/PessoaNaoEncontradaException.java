package com.bedantas.personregistry.domain;

/** Documento com formato valido, mas ninguem cadastrado com ele. Vira HTTP 404. */
public class PessoaNaoEncontradaException extends RuntimeException {

    public PessoaNaoEncontradaException(String documento) {
        super("nenhuma pessoa cadastrada com o documento " + documento);
    }
}
