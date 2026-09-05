package com.bedantas.personregistry.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SobrenomeTest {

    @ParameterizedTest
    @ValueSource(strings = { "Dantas", "Silva Junior", "O'Brien", "Nuñez", "Saint-Exupéry" })
    @DisplayName("aceita sobrenomes validos")
    void aceitaValidos(String entrada) {
        assertEquals(entrada, new Sobrenome(entrada).valor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "D", "D@ntas", "Dant4s", "<b>x</b>" })
    @DisplayName("rejeita sobrenomes invalidos")
    void rejeitaInvalidos(String entrada) {
        assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Sobrenome(entrada));
    }

    @Test
    @DisplayName("a mensagem de erro cita o campo SOBRENOME, nao 'nome'")
    void mensagemCitaOCampoCerto() {
        var erro = assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Sobrenome("D@ntas"));
        assertTrue(erro.getMessage().startsWith("sobrenome"),
                "esperava mensagem sobre 'sobrenome', veio: " + erro.getMessage());

        var vazio = assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Sobrenome(""));
        assertEquals("sobrenome e obrigatorio", vazio.getMessage());
    }

    @Test
    @DisplayName("as regras sao as mesmas do Nome, so a mensagem muda")
    void mesmasRegrasDoNome() {
        var erroNome = assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Nome("D@ntas"));
        var erroSobrenome = assertThrows(ErroDeDominio.DadoInvalido.class, () -> new Sobrenome("D@ntas"));

        assertEquals(erroNome.getMessage().replace("nome", "sobrenome"), erroSobrenome.getMessage());
    }
}
