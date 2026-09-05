package com.bedantas.prova.presentation;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bedantas.prova.application.PessoaService;
import com.bedantas.prova.domain.Documento;
import com.bedantas.prova.domain.Email;
import com.bedantas.prova.domain.Nome;
import com.bedantas.prova.domain.Pessoa;

@RestController
public class PessoaController {

    private final PessoaService servico;

    public PessoaController(PessoaService servico) {
        this.servico = servico;
    }

    /**
     * O contrato JSON e em ingles porque e a interface externa da API.
     * A validacao acontece na construcao dos Value Objects logo abaixo:
     * qualquer campo invalido lanca DadoInvalidoException, traduzida em 400
     * pelo TratadorGlobalDeErros.
     */
    public record RegistrarPessoaRequest(
            String document, String name, String lastName, String email) {
    }

    public record PessoaResponse(
            String document, String name, String lastName, String email) {

        static PessoaResponse de(Pessoa p) {
            return new PessoaResponse(
                    p.documento().valor(),
                    p.nome().valor(),
                    p.sobrenome().valor(),
                    p.email().valor());
        }
    }

    @PostMapping("/registrarName")
    public ResponseEntity<PessoaResponse> registrar(@RequestBody RegistrarPessoaRequest requisicao) {
        var pessoa = servico.registrar(
                new Documento(requisicao.document()),
                new Nome(requisicao.name()),
                new Nome(requisicao.lastName()),
                new Email(requisicao.email()));

        return ResponseEntity
                .created(URI.create("/list/" + pessoa.documento().valor()))
                .body(PessoaResponse.de(pessoa));
    }
}
