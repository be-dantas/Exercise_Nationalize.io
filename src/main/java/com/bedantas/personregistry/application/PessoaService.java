package com.bedantas.personregistry.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bedantas.personregistry.domain.error.ErroDeDominio;
import com.bedantas.personregistry.domain.valueobject.Documento;
import com.bedantas.personregistry.domain.valueobject.Email;
import com.bedantas.personregistry.domain.valueobject.Nome;
import com.bedantas.personregistry.domain.valueobject.Sobrenome;
import com.bedantas.personregistry.domain.entity.Pessoa;
import com.bedantas.personregistry.domain.port.PessoaRepository;

@Service
public class PessoaService {

    private final PessoaRepository repositorio;

    public PessoaService(PessoaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Pessoa registrar(Documento documento, Nome nome, Sobrenome sobrenome, Email email) {
        var pessoa = new Pessoa(documento, nome, sobrenome, email);
        if (!repositorio.salvarSeAusente(pessoa)) {
            throw new ErroDeDominio.PessoaJaCadastrada(documento.valor());
        }
        return pessoa;
    }

    public Pessoa obter(Documento documento) {
        return repositorio.buscarPor(documento)
                .orElseThrow(() -> new ErroDeDominio.PessoaNaoEncontrada(documento.valor()));
    }

    public List<Pessoa> listar() {
        return repositorio.listarTodas();
    }

    public void excluir(Documento documento) {
        if (!repositorio.remover(documento)) {
            throw new ErroDeDominio.PessoaNaoEncontrada(documento.valor());
        }
    }
}
