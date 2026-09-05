package com.bedantas.personregistry.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


class NomeTest {

    @Test
    @DisplayName("remove espacos das pontas")
    void removeEspacos() {
        assertEquals("Beatriz", new Nome("  Beatriz  ").valor());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Beatriz", "Jean-Pierre", "O'Brien", "Maria da Silva", "Nuñez", "Ana Lúcia" })
    @DisplayName("aceita acento, hifen, apostrofo e espaco")
    void aceitaValidos(String entrada) {
        assertEquals(entrada, new Nome(entrada).valor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "A", "Bea3triz", "Bea@triz", "<b>x</b>" })
    @DisplayName("rejeita nulo, vazio, curto demais, numero e simbolo")
    void rejeitaInvalidos(String entrada) {
        assertThrows(DadoInvalidoException.class, () -> new Nome(entrada));
    }

    @Test
    @DisplayName("rejeita nome longo demais")
    void rejeitaLongoDemais() {
        assertThrows(DadoInvalidoException.class, () -> new Nome("a".repeat(81)));
    }
}
