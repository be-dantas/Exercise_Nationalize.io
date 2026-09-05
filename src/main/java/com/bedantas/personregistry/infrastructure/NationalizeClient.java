package com.bedantas.personregistry.infrastructure;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bedantas.personregistry.domain.Nacionalidade;
import com.bedantas.personregistry.domain.Nome;
import com.bedantas.personregistry.domain.PrevisorDeNacionalidade;
import com.bedantas.personregistry.domain.ServicoExternoIndisponivelException;

/**
 * Adapter para api.nationalize.io.
 *
 * Duas responsabilidades que o dominio nao deve conhecer:
 *  1. falar HTTP com timeout, traduzindo qualquer falha em excecao de dominio;
 *  2. converter o codigo ISO 3166-1 alpha-2 devolvido pela API no NOME do pais,
 *     que e o que o enunciado pede. O Java ja traz os dados (CLDR), entao isso
 *     nao custa nenhuma dependencia extra.
 */
@Component
public class NationalizeClient implements PrevisorDeNacionalidade {

    private final RestClient http;

    public NationalizeClient(@Value("${nationalize.url}") String url) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().requestFactory(fabrica).baseUrl(url).build();
    }

    @Override
    public Optional<Nacionalidade> preverPara(Nome nome) {
        Resposta resposta;
        try {
            resposta = http.get()
                    .uri(uri -> uri.queryParam("name", nome.valor()).build())
                    .retrieve()
                    .body(Resposta.class);
        } catch (Exception e) {
            throw new ServicoExternoIndisponivelException(
                    "nao foi possivel consultar o servico de nacionalidade", e);
        }

        if (resposta == null || resposta.country() == null || resposta.country().isEmpty()) {
            return Optional.empty();
        }

        // A API devolve a lista ordenada, mas nao dependemos disso.
        Pais maisProvavel = resposta.country().stream()
                .max((a, b) -> Double.compare(a.probability(), b.probability()))
                .orElseThrow();

        return Optional.of(new Nacionalidade(
                maisProvavel.country_id(),
                nomeDoPais(maisProvavel.country_id()),
                maisProvavel.probability()));
    }

    /** "US" -> "United States". Codigo desconhecido devolve ele mesmo. */
    private String nomeDoPais(String codigoIso) {
        String nome = Locale.of("", codigoIso).getDisplayCountry(Locale.ENGLISH);
        return nome.isBlank() ? codigoIso : nome;
    }

    /** Espelho do JSON da API externa; os nomes seguem o contrato deles. */
    private record Resposta(String name, List<Pais> country) {
    }

    private record Pais(String country_id, double probability) {
    }
}
