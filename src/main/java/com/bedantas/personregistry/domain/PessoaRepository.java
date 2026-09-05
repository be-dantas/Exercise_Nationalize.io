package com.bedantas.personregistry.domain;

import java.util.List;
import java.util.Optional;

/**
 * Porta de persistencia. Fica no dominio (Java puro, zero framework);
 * a implementacao concreta vive em infrastructure.
 */
public interface PessoaRepository {

    /**
     * Guarda a pessoa apenas se o documento ainda nao estiver em uso.
     * Devolve false quando ja existia.
     *
     * Precisa ser atomico: separar em "existe?" e depois "salvar" abre uma
     * janela entre as duas chamadas, e dois cadastros simultaneos com o mesmo
     * documento passavam os dois, um sobrescrevendo o outro em silencio.
     */
    boolean salvarSeAusente(Pessoa pessoa);

    Optional<Pessoa> buscarPor(Documento documento);

    List<Pessoa> listarTodas();

    boolean remover(Documento documento);
}
