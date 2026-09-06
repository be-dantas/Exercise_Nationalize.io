package com.bedantas.personregistry.infrastructure;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.bedantas.personregistry.domain.valueobject.Documento;
import com.bedantas.personregistry.domain.entity.Pessoa;
import com.bedantas.personregistry.domain.port.PessoaRepository;

@Repository
public class PessoaRepositoryEmMemoria implements PessoaRepository {

    private final Map<Documento, Pessoa> dados = new ConcurrentHashMap<>();

    @Override
    public boolean salvarSeAusente(Pessoa pessoa) {
        return dados.putIfAbsent(pessoa.documento(), pessoa) == null;
    }

    @Override
    public Optional<Pessoa> buscarPor(Documento documento) {
        return Optional.ofNullable(dados.get(documento));
    }

    @Override
    public List<Pessoa> listarTodas() {
        return dados.values().stream()
                .sorted(Comparator.comparing((Pessoa p) -> p.nome().valor())
                        .thenComparing(p -> p.sobrenome().valor()))
                .toList();
    }

    @Override
    public boolean remover(Documento documento) {
        return dados.remove(documento) != null;
    }
}
