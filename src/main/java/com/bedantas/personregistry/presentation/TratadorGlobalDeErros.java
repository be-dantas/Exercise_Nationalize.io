package com.bedantas.personregistry.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bedantas.personregistry.domain.error.ErroDeDominio;

/**
 * Unico lugar que traduz excecao de dominio em status HTTP.
 * Por isso nenhum controller precisa de try/catch.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    public record RespostaDeErro(String error, String message) {
    }

    @ExceptionHandler(ErroDeDominio.DadoInvalido.class)
    public ResponseEntity<RespostaDeErro> dadoInvalido(ErroDeDominio.DadoInvalido e) {
        return ResponseEntity.badRequest()
                .body(new RespostaDeErro("INVALID_DATA", e.getMessage()));
    }

    @ExceptionHandler(ErroDeDominio.PessoaJaCadastrada.class)
    public ResponseEntity<RespostaDeErro> jaCadastrada(ErroDeDominio.PessoaJaCadastrada e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RespostaDeErro("ALREADY_EXISTS", e.getMessage()));
    }

    @ExceptionHandler(ErroDeDominio.PessoaNaoEncontrada.class)
    public ResponseEntity<RespostaDeErro> naoEncontrada(ErroDeDominio.PessoaNaoEncontrada e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RespostaDeErro("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(ErroDeDominio.ServicoExternoIndisponivel.class)
    public ResponseEntity<RespostaDeErro> servicoExterno(ErroDeDominio.ServicoExternoIndisponivel e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RespostaDeErro("EXTERNAL_SERVICE_UNAVAILABLE", e.getMessage()));
    }

    @ExceptionHandler(ErroDeDominio.CredencialInvalida.class)
    public ResponseEntity<RespostaDeErro> credencialInvalida(ErroDeDominio.CredencialInvalida e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new RespostaDeErro("UNAUTHORIZED", e.getMessage()));
    }

    /** Corpo ausente ou JSON malformado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaDeErro> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new RespostaDeErro("MALFORMED_REQUEST", "corpo da requisicao ausente ou invalido"));
    }
}
