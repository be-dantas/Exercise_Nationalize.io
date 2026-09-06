package com.bedantas.personregistry.infrastructure;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exige uma chave de API em todas as rotas de negocio.
 *
 * O enunciado permite "um sistema de autenticacao geral para todas as APIs" e
 * deixa o mecanismo a criterio de quem faz a prova. Escolhi chave de API por
 * ser o mecanismo mais simples que atende o requisito e permanece inteiramente
 * explicavel - proporcional a um cadastro com dados ficticios.
 *
 * A pagina estatica e o /health ficam fora: a tela precisa carregar para que
 * alguem possa digitar a chave, e o /health existe para verificar se a
 * aplicacao subiu, antes de qualquer credencial.
 *
 * A chave nunca aparece no codigo da pagina: quem usa a interface digita, e ela
 * vive apenas na memoria do navegador. Nao existe segredo no cliente - o que
 * chega ao navegador, o usuario le.
 */
@Component
public class FiltroDeAutenticacao extends OncePerRequestFilter {

    private static final String CABECALHO = "X-API-Key";

    private final String chaveEsperada;

    public FiltroDeAutenticacao(@Value("${auth.api-key}") String chaveEsperada) {
        this.chaveEsperada = chaveEsperada;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain corrente) throws ServletException, IOException {
        if (!exigeAutenticacao(requisicao.getRequestURI())) {
            corrente.doFilter(requisicao, resposta);
            return;
        }
        if (chaveConfere(requisicao.getHeader(CABECALHO))) {
            corrente.doFilter(requisicao, resposta);
            return;
        }
        recusar(resposta);
    }

    /** Protege as cinco APIs do enunciado; deixa passar a pagina e o /health. */
    private boolean exigeAutenticacao(String caminho) {
        return caminho.equals("/registrarName")
                || caminho.equals("/list")
                || caminho.startsWith("/list/")
                || caminho.startsWith("/findNacionalityByPerson/");
    }

    /**
     * Comparacao em tempo constante: String.equals sai no primeiro caractere
     * diferente, e essa diferenca de tempo pode ser medida para descobrir a
     * chave caractere a caractere.
     */
    private boolean chaveConfere(String recebida) {
        if (recebida == null || recebida.length() != chaveEsperada.length()) {
            return false;
        }
        int diferenca = 0;
        for (int i = 0; i < chaveEsperada.length(); i++) {
            diferenca |= recebida.charAt(i) ^ chaveEsperada.charAt(i);
        }
        return diferenca == 0;
    }

    /** O filtro roda antes dos controllers, entao escreve o erro no formato do contrato. */
    private void recusar(HttpServletResponse resposta) throws IOException {
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setContentType("application/json;charset=UTF-8");
        resposta.getWriter().write(
                "{\"error\":\"UNAUTHORIZED\",\"message\":\"informe a chave em X-API-Key\"}");
    }
}
