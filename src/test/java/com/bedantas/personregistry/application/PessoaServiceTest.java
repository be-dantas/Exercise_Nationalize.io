package com.bedantas.personregistry.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bedantas.personregistry.domain.model.Documento;
import com.bedantas.personregistry.domain.model.Email;
import com.bedantas.personregistry.domain.error.ErroDeDominio;
import com.bedantas.personregistry.domain.model.Nome;
import com.bedantas.personregistry.domain.model.Sobrenome;
import com.bedantas.personregistry.infrastructure.PessoaRepositoryEmMemoria;

class PessoaServiceTest {

    private static final String[] NOMES =
            { "Ana", "Bruno", "Carla", "Diego", "Elisa", "Felipe", "Gabi", "Hugo" };

    private PessoaService servicoNovo() {
        return new PessoaService(new PessoaRepositoryEmMemoria());
    }

    @Test
    @DisplayName("o mesmo documento nao pode ser cadastrado duas vezes")
    void recusaDocumentoDuplicado() {
        var servico = servicoNovo();
        var cpf = new Documento("529.982.247-25");

        servico.registrar(cpf, new Nome("Beatriz"), new Sobrenome("Dantas"),
                new Email("be@exemplo.com"));

        assertThrows(ErroDeDominio.PessoaJaCadastrada.class,
                () -> servico.registrar(cpf, new Nome("Carlos"), new Sobrenome("Souza"),
                        new Email("carlos@exemplo.com")));

        assertEquals(1, servico.listar().size());
        assertEquals("Beatriz", servico.obter(cpf).nome().valor(),
                "o primeiro cadastro nao pode ser sobrescrito");
    }

    @Test
    @DisplayName("exclusoes simultaneas do mesmo documento: exatamente uma vence")
    void exclusaoSimultaneaNaoDuplica() throws Exception {
        for (int rodada = 0; rodada < 100; rodada++) {
            var servico = servicoNovo();
            var cpf = new Documento("529.982.247-25");
            servico.registrar(cpf, new Nome("Beatriz"), new Sobrenome("Dantas"),
                    new Email("be@exemplo.com"));

            var largada = new CountDownLatch(1);
            var sucessos = new AtomicInteger();
            ExecutorService pool = Executors.newFixedThreadPool(NOMES.length);

            for (int i = 0; i < NOMES.length; i++) {
                pool.submit(() -> {
                    try {
                        largada.await();
                        servico.excluir(cpf);
                        sucessos.incrementAndGet();
                    } catch (ErroDeDominio.PessoaNaoEncontrada esperado) {
                        // quem perdeu a corrida recebe 404, e esta correto
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            largada.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "as threads travaram");

            assertEquals(1, sucessos.get(), "rodada " + rodada + ": mais de uma exclusao relatou sucesso");
            assertTrue(servico.listar().isEmpty(), "rodada " + rodada + ": sobrou registro");
        }
    }

    @Test
    @DisplayName("cadastros simultaneos com o mesmo documento: exatamente um vence")
    void cadastroSimultaneoNaoDuplica() throws Exception {
        // Regressao: quando registrar perguntava "existe?" e so depois salvava,
        // varias threads passavam pela verificacao antes de qualquer uma gravar,
        // e uma sobrescrevia a outra em silencio. Medido antes da correcao:
        // 162 de 500 rodadas terminavam com numero errado de cadastros.
        for (int rodada = 0; rodada < 100; rodada++) {
            var servico = servicoNovo();
            var cpf = new Documento("529.982.247-25");
            var largada = new CountDownLatch(1);
            var sucessos = new AtomicInteger();
            ExecutorService pool = Executors.newFixedThreadPool(NOMES.length);

            for (int i = 0; i < NOMES.length; i++) {
                final int indice = i;
                pool.submit(() -> {
                    try {
                        largada.await();
                        servico.registrar(cpf, new Nome(NOMES[indice]),
                                new Sobrenome("Souza"), new Email("p" + indice + "@ex.com"));
                        sucessos.incrementAndGet();
                    } catch (ErroDeDominio.PessoaJaCadastrada esperado) {
                        // o comportamento correto para quem perdeu a corrida
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            largada.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "as threads travaram");

            assertEquals(1, sucessos.get(), "rodada " + rodada + ": mais de um cadastro passou");
            assertEquals(1, servico.listar().size(), "rodada " + rodada + ": sobrou registro duplicado");
        }
    }
}
