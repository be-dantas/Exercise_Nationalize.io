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
import com.bedantas.personregistry.domain.valueobject.Documento;
import com.bedantas.personregistry.domain.valueobject.Email;
import com.bedantas.personregistry.domain.valueobject.Nome;
import com.bedantas.personregistry.domain.valueobject.Sobrenome;
import com.bedantas.personregistry.domain.entity.Pessoa;

@RestController
public class PessoaController {

    private final PessoaService servico;

    public PessoaController(PessoaService servico) {
        this.servico = servico;
    }

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
                new Sobrenome(requisicao.lastName()),
                new Email(requisicao.email()));

        return ResponseEntity
                .created(URI.create("/list/" + pessoa.documento().valor()))
                .body(PessoaResponse.de(pessoa));
    }

    @GetMapping("/list/{document}")
    public PessoaResponse obter(@PathVariable String document) {
        return PessoaResponse.de(servico.obter(new Documento(document)));
    }

    @GetMapping("/list")
    public List<PessoaResponse> listar() {
        return servico.listar().stream().map(PessoaResponse::de).toList();
    }

    @DeleteMapping("/list/{document}")
    public ResponseEntity<Void> excluir(@PathVariable String document) {
        servico.excluir(new Documento(document));
        return ResponseEntity.noContent().build();
    }
}
