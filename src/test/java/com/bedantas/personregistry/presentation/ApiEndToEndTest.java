package com.bedantas.personregistry.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @LocalServerPort
    int porta;

    private RestClient http;

    @BeforeEach
    void preparar() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + porta)
                // Nao lancar excecao em 4xx/5xx: aqui o status E o resultado sob teste.
                .defaultStatusHandler(status -> true, (requisicao, resposta) -> { })
                .build();
    }

    private record TokenResposta(String token, long expiresInSeconds) {
    }

    private String pessoaJson(String documento, String email) {
        return """
               {"document":"%s","name":"Beatriz","lastName":"Dantas","email":"%s"}
               """.formatted(documento, email);
    }

    private String autenticar() {
        TokenResposta t = http.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
                .retrieve().body(TokenResposta.class);
        assertNotNull(t);
        return t.token();
    }

    private ResponseEntity<String> cadastrar(String documento, String email, String token) {
        var pedido = http.post().uri("/registrarName")
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            pedido = pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return pedido.body(pessoaJson(documento, email)).retrieve().toEntity(String.class);
    }

    // ---------- disponibilidade ----------

    @Test
    @DisplayName("a aplicacao sobe e responde")
    void aplicacaoSobe() {
        var r = http.get().uri("/health").retrieve().toEntity(String.class);
        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getBody().contains("UP"));
    }

    // ---------- autenticacao ----------

    @Test
    @DisplayName("login com senha errada devolve 401")
    void loginComSenhaErrada() {
        var r = http.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"admin\",\"password\":\"errada\"}")
                .retrieve().toEntity(String.class);
        assertEquals(401, r.getStatusCode().value());
        assertTrue(r.getBody().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("login correto devolve um token")
    void loginCorreto() {
        assertTrue(autenticar().length() > 20);
    }

    // ---------- escrita protegida ----------

    @Test
    @DisplayName("cadastrar sem token devolve 401")
    void cadastrarSemToken() {
        var r = cadastrar("70000000001", "a@ex.com", null);
        assertEquals(401, r.getStatusCode().value());
        assertTrue(r.getBody().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("cadastrar com token devolve 201 e o header Location")
    void cadastrarComToken() {
        var r = cadastrar("70000000002", "b@ex.com", autenticar());
        assertEquals(201, r.getStatusCode().value());
        assertEquals("/list/70000000002", r.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    @DisplayName("excluir sem token devolve 401; com token devolve 204")
    void excluirExigeToken() {
        String token = autenticar();
        cadastrar("70000000003", "c@ex.com", token);

        var semToken = http.delete().uri("/list/70000000003").retrieve().toEntity(String.class);
        assertEquals(401, semToken.getStatusCode().value());

        var comToken = http.delete().uri("/list/70000000003")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().toEntity(String.class);
        assertEquals(204, comToken.getStatusCode().value());
    }

    @Test
    @DisplayName("token invalido devolve 401")
    void tokenInvalido() {
        var r = cadastrar("70000000004", "d@ex.com", "token-que-nao-existe");
        assertEquals(401, r.getStatusCode().value());
    }

    // ---------- leitura aberta ----------

    @Test
    @DisplayName("leitura nao exige token")
    void leituraAberta() {
        String token = autenticar();
        cadastrar("70000000005", "e@ex.com", token);

        var lista = http.get().uri("/list").retrieve().toEntity(String.class);
        assertEquals(200, lista.getStatusCode().value());
        assertTrue(lista.getBody().contains("70000000005"));

        var um = http.get().uri("/list/70000000005").retrieve().toEntity(String.class);
        assertEquals(200, um.getStatusCode().value());
    }

    @Test
    @DisplayName("documento inexistente devolve 404 e formato invalido devolve 400")
    void erros404e400() {
        var naoExiste = http.get().uri("/list/79999999999").retrieve().toEntity(String.class);
        assertEquals(404, naoExiste.getStatusCode().value());
        assertTrue(naoExiste.getBody().contains("NOT_FOUND"));

        var formatoRuim = http.get().uri("/list/abc").retrieve().toEntity(String.class);
        assertEquals(400, formatoRuim.getStatusCode().value());
        assertTrue(formatoRuim.getBody().contains("INVALID_DATA"));
    }

    // ---------- validacao ----------

    @Test
    @DisplayName("e-mail invalido devolve 400 mesmo autenticado")
    void emailInvalido() {
        var r = cadastrar("70000000006", "nao-e-email", autenticar());
        assertEquals(400, r.getStatusCode().value());
        assertTrue(r.getBody().contains("INVALID_DATA"));
    }

    @Test
    @DisplayName("documento duplicado devolve 409")
    void documentoDuplicado() {
        String token = autenticar();
        cadastrar("70000000007", "f@ex.com", token);
        var segunda = cadastrar("70000000007", "g@ex.com", token);
        assertEquals(409, segunda.getStatusCode().value());
        assertTrue(segunda.getBody().contains("ALREADY_EXISTS"));
    }
}
