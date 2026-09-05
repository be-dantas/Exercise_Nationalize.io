package com.bedantas.prova.domain;

import java.util.Optional;

/**
 * Porta de persistencia. Fica no dominio (Java puro, zero framework);
 * a implementacao concreta vive em infrastructure.
 */
public interface PessoaRepository {

    void salvar(Pessoa pessoa);

    boolean existe(Documento documento);

    Optional<Pessoa> buscarPor(Documento documento);
}
