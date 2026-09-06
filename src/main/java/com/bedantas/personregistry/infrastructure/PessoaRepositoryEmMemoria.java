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

    /** putIfAbsent do ConcurrentHashMap: consulta e escrita num passo atomico. */
    @Override
    public boolean salvarSeAusente(Pessoa pessoa) {
        return dados.putIfAbsent(pessoa.documento(), pessoa) == null;
    }

    @Override
    public Optional<Pessoa> buscarPor(Documento documento) {
        return Optional.ofNullable(dados.get(documento));
    }

    /** Ordenado por nome para a saida ser estavel entre chamadas. */
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
