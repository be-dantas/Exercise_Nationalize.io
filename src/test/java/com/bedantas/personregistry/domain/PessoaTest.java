package com.bedantas.personregistry.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PessoaTest {

    private static Pessoa comNome(String nome, String sobrenome) {
        return new Pessoa(new Documento("529.982.247-25"),
                new Nome(nome), new Nome(sobrenome), new Email("be@exemplo.com"));
    }

    @Test
    @DisplayName("a consulta de nacionalidade usa nome + sobrenome")
    void juntaNomeESobrenome() {
        assertEquals("Beatriz Dantas", comNome("Beatriz", "Dantas").nomeParaPrevisao());
    }

    @Test
    @DisplayName("nome e sobrenome longos, ambos validos, nao quebram a consulta")
    void nomeCompletoLongoNaoQuebra() {
        // Regressao: nomeParaPrevisao devolvia Nome, que valida uma PARTE do
        // nome (ate 80 caracteres). Somados, dois nomes validos passavam desse
        // limite e a consulta de nacionalidade estourava, embora a pessoa
        // tivesse sido cadastrada sem problema.
        String nome = "a".repeat(50);
        String sobrenome = "b".repeat(45);

        Pessoa pessoa = comNome(nome, sobrenome);

        assertDoesNotThrow(pessoa::nomeParaPrevisao);
        assertEquals(96, pessoa.nomeParaPrevisao().length());
        assertEquals(nome + " " + sobrenome, pessoa.nomeParaPrevisao());
    }

    @Test
    @DisplayName("a identidade da pessoa e o documento")
    void identidadeEODocumento() {
        assertEquals(new Documento("52998224725"), comNome("Beatriz", "Dantas").documento());
    }
}
