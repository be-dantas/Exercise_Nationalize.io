package com.bedantas.personregistry.domain.valueobject;

/**
 * Sobrenome da pessoa. Value Object: imutavel e sempre valido.
 *
 * Tipo proprio, e nao mais um Nome, por dois motivos:
 *  1. a mensagem de erro cita o campo certo;
 *  2. o compilador impede trocar nome e sobrenome de lugar por engano, o que
 *     antes passava despercebido porque os dois eram do mesmo tipo.
 */
public record Sobrenome(String valor) {

    public Sobrenome {
        valor = Nome.validar(valor, "sobrenome");
    }
}
