package com.bedantas.personregistry.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoTest {

    @Test
    @DisplayName("aceita CPF valido e remove a pontuacao")
    void aceitaCpfFormatado() {
        assertEquals("52998224725", new Documento("529.982.247-25").valor());
    }

    @ParameterizedTest
    @ValueSource(strings = { "52998224725", "11144477735", "39875642100", "86412250301" })
    @DisplayName("aceita CPFs validos sem pontuacao")
    void aceitaCpfsValidos(String cpf) {
        assertEquals(cpf, new Documento(cpf).valor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("rejeita nulo, vazio e espacos")
    void rejeitaVazio(String entrada) {
        assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Documento(entrada));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "<script>alert(1)</script>", "529982247AB",
                             "'; DROP TABLE pessoas--", "529 982 247 25" })
    @DisplayName("rejeita qualquer caractere que nao seja digito, ponto ou hifen")
    void rejeitaCaracteresEstranhos(String entrada) {
        assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Documento(entrada));
    }

    @ParameterizedTest
    @ValueSource(strings = { "5299822472", "529982247255" })
    @DisplayName("rejeita quantidade de digitos diferente de 11")
    void rejeitaTamanhoErrado(String entrada) {
        assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Documento(entrada));
    }

    @ParameterizedTest
    @ValueSource(strings = { "00000000000", "11111111111", "99999999999", "555.555.555-55" })
    @DisplayName("rejeita digitos repetidos, que passam no calculo mas nao sao CPF")
    void rejeitaDigitosRepetidos(String entrada) {
        assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Documento(entrada));
    }

    @ParameterizedTest
    @ValueSource(strings = { "12345678900", "529982247 26", "52998224724", "11144477736" })
    @DisplayName("rejeita CPF com digito verificador errado")
    void rejeitaDigitoVerificadorErrado(String entrada) {
        assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Documento(entrada));
    }

    @Test
    @DisplayName("igualdade por valor, para servir de chave no repositorio")
    void igualdadePorValor() {
        assertEquals(new Documento("529.982.247-25"), new Documento("52998224725"));
    }
}
