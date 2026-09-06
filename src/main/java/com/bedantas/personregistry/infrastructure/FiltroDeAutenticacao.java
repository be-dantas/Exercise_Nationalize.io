package com.bedantas.personregistry.infrastructure;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    /** Protege as cinco APIs do enunciado; so a pagina passa sem chave. */
    private boolean exigeAutenticacao(String caminho) {
        return caminho.equals("/registrarName")
                || caminho.equals("/list")
                || caminho.startsWith("/list/")
                || caminho.startsWith("/findNacionalityByPerson/");
    }

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

    private void recusar(HttpServletResponse resposta) throws IOException {
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setContentType("application/json;charset=UTF-8");
        resposta.getWriter().write(
                "{\"error\":\"UNAUTHORIZED\",\"message\":\"informe a chave em X-API-Key\"}");
    }
}
