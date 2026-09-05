package com.bedantas.prova.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


class DocumentoTest {

    @Test
    @DisplayName("normaliza pontuacao de CPF")
    void normalizaCpf() {
        assertEquals("12345678900", new Documento("123.456.789-00").valor());
    }

    @Test
    @DisplayName("aceita documento estrangeiro (DNI argentino)")
    void aceitaDocumentoEstrangeiro() {
        assertEquals("20123456789", new Documento("20-12345678-9").valor());
    }

    @Test
    @DisplayName("aceita passaporte alfanumerico")
    void aceitaPassaporte() {
        assertEquals("AB123456", new Documento("AB123456").valor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "abc", "12345", "..--..",
                             "123456789012345678901", "<script>alert(1)</script>" })
    @DisplayName("rejeita nulo, vazio, curto demais, longo demais e lixo")
    void rejeitaInvalidos(String entrada) {
        assertThrows(DadoInvalidoException.class, () -> new Documento(entrada));
    }

    @Test
    @DisplayName("igualdade por valor, para servir de chave no repositorio")
    void igualdadePorValor() {
        assertEquals(new Documento("123.456.789-00"), new Documento("12345678900"));
    }
}
