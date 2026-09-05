package com.bedantas.personregistry.presentation;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bedantas.personregistry.application.PessoaService;
import com.bedantas.personregistry.domain.Documento;
import com.bedantas.personregistry.domain.Email;
import com.bedantas.personregistry.domain.Nome;
import com.bedantas.personregistry.domain.Pessoa;

@RestController
public class PessoaController {

    private final PessoaService servico;

    public PessoaController(PessoaService servico) {
        this.servico = servico;
    }

    /**
     * O contrato JSON e em ingles porque e a interface externa da API.
     * A validacao acontece na construcao dos Value Objects logo abaixo:
     * qualquer campo invalido lanca ErroDeDominio.DadoInvalido, traduzida em 400
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

    /**
     * O parametro e o documento (identidade natural da pessoa). Construir o
     * Value Object ja valida o formato: nenhum "if" extra e necessario aqui.
     */
    @GetMapping("/list/{document}")
    public PessoaResponse obter(@PathVariable String document) {
        return PessoaResponse.de(servico.obter(new Documento(document)));
    }

    @GetMapping("/list")
    public List<PessoaResponse> listar() {
        return servico.listar().stream().map(PessoaResponse::de).toList();
    }

    /** 204: sucesso sem corpo. Excluir de novo devolve 404. */
    @DeleteMapping("/list/{document}")
    public ResponseEntity<Void> excluir(@PathVariable String document) {
        servico.excluir(new Documento(document));
        return ResponseEntity.noContent().build();
    }
}
