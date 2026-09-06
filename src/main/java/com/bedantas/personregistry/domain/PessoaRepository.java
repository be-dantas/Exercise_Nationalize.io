package com.bedantas.personregistry.domain;

import java.util.List;
import java.util.Optional;

/** A implementacao concreta vive em infrastructure. */

public interface PessoaRepository {

    boolean salvarSeAusente(Pessoa pessoa);

    Optional<Pessoa> buscarPor(Documento documento);

    List<Pessoa> listarTodas();

    boolean remover(Documento documento);
}
