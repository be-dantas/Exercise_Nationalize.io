package com.bedantas.prova.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bedantas.prova.domain.Documento;
import com.bedantas.prova.domain.Email;
import com.bedantas.prova.domain.Nacionalidade;
import com.bedantas.prova.domain.Nome;
import com.bedantas.prova.domain.Pessoa;
import com.bedantas.prova.domain.PessoaNaoEncontradaException;
import com.bedantas.prova.domain.PessoaRepository;
import com.bedantas.prova.domain.PrevisorDeNacionalidade;
import com.bedantas.prova.domain.ServicoExternoIndisponivelException;

/**
 * Roda sem rede e sem subir o Spring. So e possivel porque o servico externo
 * esta atras da porta PrevisorDeNacionalidade: aqui plugamos implementacoes
 * falsas no lugar da API real.
 */
class NacionalidadeServiceTest {

    private PessoaRepository repositorio;
    private PessoaService pessoas;

    /** Repositorio falso, equivalente ao de producao para efeito de teste. */
    static class RepositorioFalso implements PessoaRepository {
        private final Map<Documento, Pessoa> dados = new ConcurrentHashMap<>();
        public void salvar(Pessoa p) { dados.put(p.documento(), p); }
        public boolean existe(Documento d) { return dados.containsKey(d); }
        public Optional<Pessoa> buscarPor(Documento d) { return Optional.ofNullable(dados.get(d)); }
        public java.util.List<Pessoa> listarTodas() { return java.util.List.copyOf(dados.values()); }
        public boolean remover(Documento d) { return dados.remove(d) != null; }
    }

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioFalso();
        pessoas = new PessoaService(repositorio);
        pessoas.registrar(new Documento("12345678900"), new Nome("Nathaniel"),
                          new Nome("Silva"), new Email("nat@exemplo.com"));
    }

    @Test
    @DisplayName("consulta usa nome + sobrenome, nao so o primeiro nome")
    void consultaUsaNomeCompleto() {
        var enviado = new java.util.concurrent.atomic.AtomicReference<String>();
        PrevisorDeNacionalidade previsor = nome -> {
            enviado.set(nome.valor());
            return Optional.of(new Nacionalidade("US", "United States", 0.35));
        };

        new NacionalidadeService(pessoas, previsor).descobrir(new Documento("12345678900"));

        assertEquals("Nathaniel Silva", enviado.get());
    }

    @Test
    @DisplayName("devolve o NOME do pais, nunca o codigo ISO")
    void devolveNomeDoPais() {
        PrevisorDeNacionalidade previsor =
                nome -> Optional.of(new Nacionalidade("US", "United States", 0.35));

        var resultado = new NacionalidadeService(pessoas, previsor)
                .descobrir(new Documento("12345678900"));

        assertTrue(resultado.previsao().isPresent());
        assertEquals("United States", resultado.previsao().get().nomeDoPais());
        assertEquals("Nathaniel", resultado.pessoa().nome().valor());
    }

    @Test
    @DisplayName("sem palpite para o nome, devolve previsao vazia (nao e erro)")
    void semPalpite() {
        PrevisorDeNacionalidade previsor = nome -> Optional.empty();

        var resultado = new NacionalidadeService(pessoas, previsor)
                .descobrir(new Documento("12345678900"));

        assertTrue(resultado.previsao().isEmpty());
    }

    @Test
    @DisplayName("pessoa inexistente nao chega a consultar o servico externo")
    void pessoaInexistente() {
        PrevisorDeNacionalidade previsor = nome -> {
            throw new AssertionError("o servico externo nao deveria ser chamado");
        };

        var servico = new NacionalidadeService(pessoas, previsor);
        assertThrows(PessoaNaoEncontradaException.class,
                () -> servico.descobrir(new Documento("99988877766")));
    }

    @Test
    @DisplayName("falha do servico externo propaga como indisponibilidade")
    void servicoExternoFora() {
        PrevisorDeNacionalidade previsor = nome -> {
            throw new ServicoExternoIndisponivelException("fora do ar", new RuntimeException());
        };

        var servico = new NacionalidadeService(pessoas, previsor);
        assertThrows(ServicoExternoIndisponivelException.class,
                () -> servico.descobrir(new Documento("12345678900")));
    }
}
