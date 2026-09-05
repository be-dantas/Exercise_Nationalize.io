package com.bedantas.prova.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bedantas.prova.domain.DadoInvalidoException;
import com.bedantas.prova.domain.PessoaJaCadastradaException;

/**
 * Unico lugar que traduz excecao de dominio em status HTTP.
 * Por isso nenhum controller precisa de try/catch.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    public record RespostaDeErro(String error, String message) {
    }

    @ExceptionHandler(DadoInvalidoException.class)
    public ResponseEntity<RespostaDeErro> dadoInvalido(DadoInvalidoException e) {
        return ResponseEntity.badRequest()
                .body(new RespostaDeErro("INVALID_DATA", e.getMessage()));
    }

    @ExceptionHandler(PessoaJaCadastradaException.class)
    public ResponseEntity<RespostaDeErro> jaCadastrada(PessoaJaCadastradaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RespostaDeErro("ALREADY_EXISTS", e.getMessage()));
    }

    /** Corpo ausente ou JSON malformado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaDeErro> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new RespostaDeErro("MALFORMED_REQUEST", "corpo da requisicao ausente ou invalido"));
    }
}
