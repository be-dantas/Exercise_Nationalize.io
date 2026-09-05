package com.bedantas.personregistry.presentation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bedantas.personregistry.infrastructure.ServicoDeToken;

@RestController
public class AuthController {

    private final ServicoDeToken tokens;

    public AuthController(ServicoDeToken tokens) {
        this.tokens = tokens;
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, long expiresInSeconds) {
    }

    @PostMapping("/auth/login")
    public LoginResponse entrar(@RequestBody LoginRequest requisicao) {
        var sessao = tokens.autenticar(requisicao.username(), requisicao.password());
        return new LoginResponse(sessao.token(), sessao.validadeEmSegundos());
    }
}
