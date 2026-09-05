package com.bedantas.personregistry.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


class EmailTest {

    @Test
    @DisplayName("normaliza para minusculo e remove espacos")
    void normaliza() {
        assertEquals("be@exemplo.com", new Email("  BE@Exemplo.COM  ").valor());
    }

    @ParameterizedTest
    @ValueSource(strings = { "be@exemplo.com", "be.dantas+tag@sub.exemplo.com.br", "a_b-c@x-y.io" })
    @DisplayName("aceita enderecos validos")
    void aceitaValidos(String entrada) {
        assertEquals(entrada.toLowerCase(), new Email(entrada).valor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "sem-arroba", "@semlocal.com", "sem@dominio",
                             "espaco no@meio.com", "dois@@arrobas.com" })
    @DisplayName("rejeita enderecos invalidos")
    void rejeitaInvalidos(String entrada) {
        assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Email(entrada));
    }
}
