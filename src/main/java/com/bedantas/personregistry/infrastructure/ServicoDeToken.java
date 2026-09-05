package com.bedantas.personregistry.infrastructure;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.bedantas.personregistry.domain.ErroDeDominio;

/**
 * Autenticacao por token opaco.
 *
 * A aplicacao roda em instancia unica, entao a vantagem do JWT (validar sem
 * estado compartilhado) nao se aplicaria aqui. Em troca, token opaco da
 * revogacao imediata: basta remover do mapa. Se fosse escalar horizontalmente,
 * este mapa iria para um Redis ou o token viraria JWT.
 *
 * A senha nunca e guardada em texto: o arquivo de configuracao tem apenas o
 * hash BCrypt.
 */
@Component
public class ServicoDeToken {

    private static final int BYTES_DO_TOKEN = 32;

    private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();
    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    private final String usuario;
    private final String senhaHash;
    private final Duration validade;

    public ServicoDeToken(
            @Value("${auth.usuario}") String usuario,
            @Value("${auth.senha-hash}") String senhaHash,
            @Value("${auth.validade-minutos}") long validadeMinutos) {
        this.usuario = usuario;
        this.senhaHash = senhaHash;
        this.validade = Duration.ofMinutes(validadeMinutos);
    }

    public Sessao autenticar(String usuario, String senha) {
        boolean usuarioConfere = this.usuario.equals(usuario);
        // Verifica a senha mesmo com usuario errado: se saissemos antes, o tempo
        // de resposta revelaria quais usuarios existem.
        boolean senhaConfere = cifrador.matches(senha == null ? "" : senha, senhaHash);

        if (!usuarioConfere || !senhaConfere) {
            throw new ErroDeDominio.CredencialInvalida();
        }

        byte[] bruto = new byte[BYTES_DO_TOKEN];
        aleatorio.nextBytes(bruto);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bruto);

        tokens.put(token, Instant.now().plus(validade));
        return new Sessao(token, validade.toSeconds());
    }

    public boolean tokenValido(String token) {
        Instant expiraEm = tokens.get(token);
        if (expiraEm == null) {
            return false;
        }
        if (Instant.now().isAfter(expiraEm)) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public record Sessao(String token, long validadeEmSegundos) {
    }
}
