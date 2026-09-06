package com.bedantas.personregistry.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Sobe a aplicacao inteira numa porta real e exercita a API por HTTP.
 *
 * Nao toca no servico externo de nacionalidade: aquele caminho e coberto por
 * NacionalidadeServiceTest com implementacoes falsas, entao esta suite roda
 * sem rede.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiEndToEndTest {

    private static final String CABECALHO = "X-API-Key";

    @LocalServerPort
    int porta;

    @Value("${auth.api-key}")
    String chave;

    private RestClient http;

    @BeforeEach
    void preparar() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + porta)
                // Nao lancar excecao em 4xx/5xx: aqui o status E o resultado sob teste.
                .defaultStatusHandler(status -> true, (requisicao, resposta) -> { })
                .build();
    }

    private String pessoaJson(String documento, String email) {
        return """
               {"document":"%s","name":"Beatriz","lastName":"Dantas","email":"%s"}
               """.formatted(documento, email);
    }

    private ResponseEntity<String> cadastrar(String documento, String email, String chaveUsada) {
        var pedido = http.post().uri("/registrarName").contentType(MediaType.APPLICATION_JSON);
        if (chaveUsada != null) {
            pedido = pedido.header(CABECALHO, chaveUsada);
        }
        return pedido.body(pessoaJson(documento, email)).retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> get(String caminho, String chaveUsada) {
        var pedido = http.get().uri(caminho);
        if (chaveUsada != null) {
            pedido = pedido.header(CABECALHO, chaveUsada);
        }
        return pedido.retrieve().toEntity(String.class);
    }

    // ---------- disponibilidade ----------

    @Test
    @DisplayName("a pagina carrega sem chave: e nela que a chave e digitada")
    void paginaNaoExigeChave() {
        var r = http.get().uri("/").retrieve().toEntity(String.class);
        assertEquals(200, r.getStatusCode().value());
    }

    // ---------- a chave protege as cinco APIs ----------

    @Test
    @DisplayName("sem chave, todas as cinco APIs devolvem 401")
    void semChaveTudoRecusa() {
        assertEquals(401, cadastrar("10433218100", "a@ex.com", null).getStatusCode().value());
        assertEquals(401, get("/list", null).getStatusCode().value());
        assertEquals(401, get("/list/10433218100", null).getStatusCode().value());
        assertEquals(401, get("/findNacionalityByPerson/10433218100", null).getStatusCode().value());
        assertEquals(401, http.delete().uri("/list/10433218100")
                .retrieve().toEntity(String.class).getStatusCode().value());
    }

    @Test
    @DisplayName("chave errada tambem devolve 401, com o formato de erro do contrato")
    void chaveErradaRecusa() {
        var r = get("/list", "chave-errada");
        assertEquals(401, r.getStatusCode().value());
        assertTrue(r.getBody().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("chave do tamanho certo mas com um caractere trocado tambem recusa")
    void chaveQuaseCertaRecusa() {
        String quase = chave.substring(0, chave.length() - 1) + "X";
        assertEquals(401, get("/list", quase).getStatusCode().value());
    }

    // ---------- com a chave, tudo funciona ----------

    @Test
    @DisplayName("cadastrar devolve 201 e o header Location")
    void cadastrarComChave() {
        var r = cadastrar("96001338914", "b@ex.com", chave);
        assertEquals(201, r.getStatusCode().value());
        assertEquals("/list/96001338914", r.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    @DisplayName("listar e obter funcionam com a chave")
    void leituraComChave() {
        cadastrar("08386379499", "c@ex.com", chave);

        var lista = get("/list", chave);
        assertEquals(200, lista.getStatusCode().value());
        assertTrue(lista.getBody().contains("08386379499"));

        assertEquals(200, get("/list/08386379499", chave).getStatusCode().value());
    }

    @Test
    @DisplayName("excluir devolve 204 e a pessoa some")
    void excluirComChave() {
        cadastrar("02654235114", "d@ex.com", chave);

        var exclusao = http.delete().uri("/list/02654235114").header(CABECALHO, chave)
                .retrieve().toEntity(String.class);
        assertEquals(204, exclusao.getStatusCode().value());
        assertEquals(404, get("/list/02654235114", chave).getStatusCode().value());
    }

    // ---------- erros de dominio ----------

    @Test
    @DisplayName("documento inexistente devolve 404 e formato invalido devolve 400")
    void erros404e400() {
        var naoExiste = get("/list/16155940789", chave);
        assertEquals(404, naoExiste.getStatusCode().value());
        assertTrue(naoExiste.getBody().contains("NOT_FOUND"));

        var formatoRuim = get("/list/abc", chave);
        assertEquals(400, formatoRuim.getStatusCode().value());
        assertTrue(formatoRuim.getBody().contains("INVALID_DATA"));
    }

    @Test
    @DisplayName("e-mail invalido devolve 400 mesmo com a chave certa")
    void emailInvalido() {
        var r = cadastrar("81618495950", "nao-e-email", chave);
        assertEquals(400, r.getStatusCode().value());
        assertTrue(r.getBody().contains("INVALID_DATA"));
    }

    @Test
    @DisplayName("CPF com digito verificador errado devolve 400")
    void cpfInvalido() {
        var r = cadastrar("12345678900", "e@ex.com", chave);
        assertEquals(400, r.getStatusCode().value());
        assertTrue(r.getBody().contains("digito verificador"));
    }

    @Test
    @DisplayName("documento duplicado devolve 409")
    void documentoDuplicado() {
        cadastrar("31034131656", "f@ex.com", chave);
        var segunda = cadastrar("31034131656", "g@ex.com", chave);
        assertEquals(409, segunda.getStatusCode().value());
        assertTrue(segunda.getBody().contains("ALREADY_EXISTS"));
    }
}
