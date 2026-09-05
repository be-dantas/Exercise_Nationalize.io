package com.bedantas.personregistry.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bedantas.personregistry.domain.PessoaJaCadastradaException;
import com.bedantas.personregistry.domain.PessoaNaoEncontradaException;
import com.bedantas.personregistry.domain.Documento;
import com.bedantas.personregistry.domain.Email;
import com.bedantas.personregistry.domain.Nome;
import com.bedantas.personregistry.domain.Pessoa;
import com.bedantas.personregistry.domain.PessoaRepository;

/**
 * Casos de uso de pessoa. Recebe Value Objects ja validados: a conversao de
 * texto cru para dominio acontece na borda (controller), entao aqui nao existe
 * dado invalido possivel.
 */
@Service
public class PessoaService {

    private final PessoaRepository repositorio;

    public PessoaService(PessoaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Pessoa registrar(Documento documento, Nome nome, Nome sobrenome, Email email) {
        if (repositorio.existe(documento)) {
            throw new PessoaJaCadastradaException(documento.valor());
        }
        var pessoa = new Pessoa(documento, nome, sobrenome, email);
        repositorio.salvar(pessoa);
        return pessoa;
    }

    public Pessoa obter(Documento documento) {
        return repositorio.buscarPor(documento)
                .orElseThrow(() -> new PessoaNaoEncontradaException(documento.valor()));
    }

    public List<Pessoa> listar() {
        return repositorio.listarTodas();
    }

    public void excluir(Documento documento) {
        if (!repositorio.remover(documento)) {
            throw new PessoaNaoEncontradaException(documento.valor());
        }
    }
}
