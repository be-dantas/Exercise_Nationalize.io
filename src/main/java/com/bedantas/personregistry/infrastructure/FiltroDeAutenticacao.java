package com.bedantas.personregistry.infrastructure;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protege apenas as operacoes que alteram estado.
 *
 * DELETE e destrutivo e irreversivel, POST cria registro; leitura nao muda
 * nada e fica aberta, o que tambem permite avaliar a API sem atrito.
 *
 * Escrito a mao em vez de usar spring-boot-starter-security: o requisito e
 * simples, e assim cada linha do controle de acesso e visivel e explicavel.
 */
@Component
public class FiltroDeAutenticacao extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final ServicoDeToken tokens;

    public FiltroDeAutenticacao(ServicoDeToken tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain corrente) throws ServletException, IOException {
        if (!exigeAutenticacao(requisicao)) {
            corrente.doFilter(requisicao, resposta);
            return;
        }

        String cabecalho = requisicao.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)
                && tokens.tokenValido(cabecalho.substring(PREFIXO.length()))) {
            corrente.doFilter(requisicao, resposta);
            return;
        }

        recusar(resposta);
    }

    private boolean exigeAutenticacao(HttpServletRequest requisicao) {
        String caminho = requisicao.getRequestURI();
        return switch (requisicao.getMethod()) {
            case "POST" -> "/registrarName".equals(caminho);
            case "DELETE" -> caminho.startsWith("/list/");
            default -> false;
        };
    }

    /** O filtro roda antes dos controllers, entao escreve o erro no formato do contrato. */
    private void recusar(HttpServletResponse resposta) throws IOException {
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setContentType("application/json;charset=UTF-8");
        resposta.getWriter().write(
                "{\"error\":\"UNAUTHORIZED\",\"message\":\"informe um token valido em Authorization: Bearer <token>\"}");
    }
}
