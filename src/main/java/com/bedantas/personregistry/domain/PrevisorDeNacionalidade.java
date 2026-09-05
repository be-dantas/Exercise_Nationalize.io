package com.bedantas.personregistry.domain;

import java.util.Optional;

/**
 * Porta para o servico externo de previsao de nacionalidade.
 *
 * Existir como interface permite testar sem rede e isola num unico lugar o
 * tratamento de falha, timeout e limite de uso da API de terceiro.
 */
public interface PrevisorDeNacionalidade {

    /** Vazio quando o servico nao tem palpite para o nome. */
    Optional<Nacionalidade> preverPara(Nome nome);
}
