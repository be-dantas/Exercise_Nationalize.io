package com.bedantas.personregistry.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bedantas.personregistry.domain.Documento;
import com.bedantas.personregistry.domain.Email;
import com.bedantas.personregistry.domain.Nacionalidade;
import com.bedantas.personregistry.domain.Nome;
import com.bedantas.personregistry.domain.Pessoa;
import com.bedantas.personregistry.domain.Sobrenome;
import com.bedantas.personregistry.domain.ErroDeDominio;
import com.bedantas.personregistry.domain.PessoaRepository;
import com.bedantas.personregistry.domain.PrevisorDeNacionalidade;

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
        pessoas.registrar(new Documento("10433218100"), new Nome("Nathaniel"),
                          new Sobrenome("Silva"), new Email("nat@exemplo.com"));
    }

    @Test
    @DisplayName("consulta usa nome + sobrenome, nao so o primeiro nome")
    void consultaUsaNomeCompleto() {
        var enviado = new java.util.concurrent.atomic.AtomicReference<String>();
        PrevisorDeNacionalidade previsor = nome -> {
            enviado.set(nome);
            return Optional.of(new Nacionalidade("US", "United States", 0.35));
        };

        new NacionalidadeService(pessoas, previsor).descobrir(new Documento("10433218100"));

        assertEquals("Nathaniel Silva", enviado.get());
    }

    @Test
    @DisplayName("devolve o NOME do pais, nunca o codigo ISO")
    void devolveNomeDoPais() {
        PrevisorDeNacionalidade previsor =
                nome -> Optional.of(new Nacionalidade("US", "United States", 0.35));

        var resultado = new NacionalidadeService(pessoas, previsor)
                .descobrir(new Documento("10433218100"));

        assertTrue(resultado.previsao().isPresent());
        assertEquals("United States", resultado.previsao().get().nomeDoPais());
        assertEquals("Nathaniel", resultado.pessoa().nome().valor());
    }

    @Test
    @DisplayName("sem palpite para o nome, devolve previsao vazia (nao e erro)")
    void semPalpite() {
        PrevisorDeNacionalidade previsor = nome -> Optional.empty();

        var resultado = new NacionalidadeService(pessoas, previsor)
                .descobrir(new Documento("10433218100"));

        assertTrue(resultado.previsao().isEmpty());
    }

    @Test
    @DisplayName("pessoa inexistente nao chega a consultar o servico externo")
    void pessoaInexistente() {
        PrevisorDeNacionalidade previsor = nome -> {
            throw new AssertionError("o servico externo nao deveria ser chamado");
        };

        var servico = new NacionalidadeService(pessoas, previsor);
        assertThrows(ErroDeDominio.PessoaNaoEncontrada.class,
                () -> servico.descobrir(new Documento("96001338914")));
    }

    @Test
    @DisplayName("falha do servico externo propaga como indisponibilidade")
    void servicoExternoFora() {
        PrevisorDeNacionalidade previsor = nome -> {
            throw new ErroDeDominio.ServicoExternoIndisponivel("fora do ar", new RuntimeException());
        };

        var servico = new NacionalidadeService(pessoas, previsor);
        assertThrows(ErroDeDominio.ServicoExternoIndisponivel.class,
                () -> servico.descobrir(new Documento("10433218100")));
    }
}
