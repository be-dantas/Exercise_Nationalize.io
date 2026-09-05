package com.bedantas.prova.infrastructure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.bedantas.prova.domain.Documento;
import com.bedantas.prova.domain.Pessoa;
import com.bedantas.prova.domain.PessoaRepository;

/**
 * Adapter de persistencia em memoria - o enunciado permite explicitamente
 * "armazenamento em memoria".
 *
 * Documento e um record, entao equals/hashCode por valor funcionam como chave.
 * Trocar por JPA/PostgreSQL significa criar outro adapter: nenhum caso de uso muda.
 */
@Repository
public class PessoaRepositoryEmMemoria implements PessoaRepository {

    private final Map<Documento, Pessoa> dados = new ConcurrentHashMap<>();

    @Override
    public void salvar(Pessoa pessoa) {
        dados.put(pessoa.documento(), pessoa);
    }

    @Override
    public boolean existe(Documento documento) {
        return dados.containsKey(documento);
    }
}
