package com.bedantas.prova.application;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bedantas.prova.domain.Documento;
import com.bedantas.prova.domain.Nacionalidade;
import com.bedantas.prova.domain.Pessoa;
import com.bedantas.prova.domain.PrevisorDeNacionalidade;

/**
 * Descobre a nacionalidade provavel de uma pessoa ja cadastrada.
 * Nao sabe que existe HTTP: fala apenas com a porta PrevisorDeNacionalidade.
 */
@Service
public class NacionalidadeService {

    private final PessoaService pessoas;
    private final PrevisorDeNacionalidade previsor;

    public NacionalidadeService(PessoaService pessoas, PrevisorDeNacionalidade previsor) {
        this.pessoas = pessoas;
        this.previsor = previsor;
    }

    public Resultado descobrir(Documento documento) {
        Pessoa pessoa = pessoas.obter(documento);
        Optional<Nacionalidade> previsao = previsor.preverPara(pessoa.nomeParaPrevisao());
        return new Resultado(pessoa, previsao);
    }

    public record Resultado(Pessoa pessoa, Optional<Nacionalidade> previsao) {
    }
}
