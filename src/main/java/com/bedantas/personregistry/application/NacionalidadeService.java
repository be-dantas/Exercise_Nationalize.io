package com.bedantas.personregistry.application;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bedantas.personregistry.domain.valueobject.Documento;
import com.bedantas.personregistry.domain.valueobject.Nacionalidade;
import com.bedantas.personregistry.domain.entity.Pessoa;
import com.bedantas.personregistry.domain.port.PrevisorDeNacionalidade;

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
