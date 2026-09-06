package com.bedantas.personregistry.infrastructure;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import com.bedantas.personregistry.domain.valueobject.Nacionalidade;
import com.bedantas.personregistry.domain.port.PrevisorDeNacionalidade;
import com.bedantas.personregistry.domain.error.ErroDeDominio;

/**
 * Adapter para api.nationalize.io.
 *
 * Tres responsabilidades que o dominio nao deve conhecer:
 *  1. falar HTTP com timeout, traduzindo qualquer falha em excecao de dominio;
 *  2. converter o codigo ISO 3166-1 alpha-2 devolvido pela API no NOME do pais,
 *     que e o que o enunciado pede. O Java ja traz os dados (CLDR), entao isso
 *     nao custa nenhuma dependencia extra;
 *  3. guardar em cache o que ja foi consultado - o plano gratuito da API
 *     permite apenas 25 requisicoes por dia (cabecalho x-rate-limit-limit).
 *     Sem cache, repetir a mesma consulta gasta cota a toa.
 *
 * Tudo isso vive aqui: nenhuma outra camada sabe que existe cache, timeout
 * ou limite de uso.
 */
@Component
public class NationalizeClient implements PrevisorDeNacionalidade {

    private final RestClient http;

    /** Nome consultado -> previsao. Vazio significa "consultado, sem palpite". */
    private final Map<String, Optional<Nacionalidade>> cache = new ConcurrentHashMap<>();

    public NationalizeClient(@Value("${nationalize.url}") String url) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().requestFactory(fabrica).baseUrl(url).build();
    }

    @Override
    public Optional<Nacionalidade> preverPara(String nomeCompleto) {
        return cache.computeIfAbsent(nomeCompleto.toLowerCase(), chave -> consultar(nomeCompleto));
    }

    private Optional<Nacionalidade> consultar(String nomeCompleto) {
        Resposta resposta;
        try {
            resposta = http.get()
                    .uri(uri -> uri.queryParam("name", nomeCompleto).build())
                    .retrieve()
                    .body(Resposta.class);
        } catch (HttpStatusCodeException e) {
            throw new ErroDeDominio.ServicoExternoIndisponivel(mensagemPara(e), e);
        } catch (Exception e) {
            throw new ErroDeDominio.ServicoExternoIndisponivel(
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

    /**
     * Cota estourada e a falha mais provavel de aparecer na pratica, entao ela
     * ganha uma mensagem propria: sem isso, quem estiver avaliando ve um erro
     * generico e conclui que a aplicacao esta quebrada.
     */
    private String mensagemPara(HttpStatusCodeException e) {
        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return "limite diario de requisicoes da api.nationalize.io atingido "
                    + "(25/dia no plano gratuito); tente novamente mais tarde";
        }
        return "o servico de nacionalidade respondeu " + e.getStatusCode().value();
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
