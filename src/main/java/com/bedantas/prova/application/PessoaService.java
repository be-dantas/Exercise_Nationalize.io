package com.bedantas.prova.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bedantas.prova.domain.PessoaJaCadastradaException;
import com.bedantas.prova.domain.PessoaNaoEncontradaException;
import com.bedantas.prova.domain.Documento;
import com.bedantas.prova.domain.Email;
import com.bedantas.prova.domain.Nome;
import com.bedantas.prova.domain.Pessoa;
import com.bedantas.prova.domain.PessoaRepository;

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
