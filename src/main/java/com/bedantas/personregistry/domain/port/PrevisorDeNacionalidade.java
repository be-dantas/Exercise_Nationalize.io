package com.bedantas.personregistry.domain.port;

import com.bedantas.personregistry.domain.valueobject.Nacionalidade;

import java.util.Optional;

/** Porta para o servico externo de previsao de nacionalidade. */
public interface PrevisorDeNacionalidade {

    Optional<Nacionalidade> preverPara(String nomeCompleto);
}
