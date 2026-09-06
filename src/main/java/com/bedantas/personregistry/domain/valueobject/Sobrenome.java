package com.bedantas.personregistry.domain.valueobject;

public record Sobrenome(String valor) {

    public Sobrenome {
        valor = Nome.validar(valor, "sobrenome");
    }
}
