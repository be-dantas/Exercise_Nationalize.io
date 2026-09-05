# Person Registry API

API REST para cadastrar pessoas e prever a provável nacionalidade delas a partir
de um serviço externo, com interface web e autenticação nas operações de escrita.

Java 21 · Spring Boot 4.1.1 · Maven · sem servidor de banco, sem Docker.

---

## Como rodar

Requisito único: um **JDK 21 ou superior**. O Maven não precisa estar instalado —
o wrapper baixa a versão certa sozinho.

```bash
./mvnw -q spring-boot:run
```

Ao subir, imprime:

```
  cadastro.  ->  http://localhost:8080

  login de demonstracao: admin / admin123
  encerrar: Ctrl+C
```

Depois é só abrir **http://localhost:8080**.

| O que | Comando |
|---|---|
| Rodar | `./mvnw -q spring-boot:run` |
| Testar | `./mvnw -q test` |
| Gerar o jar | `./mvnw -q clean package` |
| Rodar o jar sozinho | `java -jar target/*.jar` |

O `-q` deixa o build silencioso: **nenhuma saída significa sucesso**. Falha
continua aparecendo por inteiro — teste quebrado mostra a asserção, erro de
compilação mostra arquivo e linha, e falha ao subir mostra o diagnóstico do
Spring. Tire o `-q` para ver o log completo do Maven.


---

## O que a prova pedia

| Requisito | Onde está |
|---|---|
| Java + algum sistema de persistência | Java 21; armazenamento em memória, explicitamente permitido pelo enunciado |
| `POST /registrarName` com documento, nome, sobrenome e e-mail | `PessoaController` |
| `GET /list` | `PessoaController` |
| `GET /list/{param}` | `PessoaController`, o parâmetro é o documento |
| `DELETE /list/{param}` | `PessoaController` |
| `GET /findNacionalityByPerson/{param}` devolvendo o **nome** da nacionalidade | `NacionalidadeController` + `NationalizeClient` |
| Pelo menos uma validação de tipo por API | Value Objects `Documento`, `Nome`, `Email` |
| Autenticação na API mais crítica | `FiltroDeAutenticacao` no `POST` e no `DELETE` |
| Interface web consumindo ao menos uma API | `static/index.html`, consome todas |

---

## A API

| Método | Rota | Auth | Sucesso | Erros |
|---|---|:---:|---|---|
| `POST` | `/auth/login` | — | `200` `{token, expiresInSeconds}` | `401` |
| `POST` | `/registrarName` | 🔒 | `201` + `Location: /list/{document}` | `400` `401` `409` |
| `GET` | `/list` | — | `200` lista ordenada por nome | — |
| `GET` | `/list/{document}` | — | `200` | `400` `404` |
| `DELETE` | `/list/{document}` | 🔒 | `204` | `400` `401` `404` |
| `GET` | `/findNacionalityByPerson/{document}` | — | `200` `{name, nationality, probability}` | `400` `404` `503` |
| `GET` | `/health` | — | `200` | — |

Todo erro usa o mesmo formato:

```json
{ "error": "INVALID_DATA", "message": "e-mail invalido: nao-e-email" }
```

`INVALID_DATA` · `NOT_FOUND` · `ALREADY_EXISTS` · `UNAUTHORIZED` ·
`MALFORMED_REQUEST` · `EXTERNAL_SERVICE_UNAVAILABLE`

O contrato JSON está em inglês por ser a interface externa da API; o código
está em português, acompanhando a língua do enunciado.

### Exemplo de uso

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -X POST localhost:8080/registrarName \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"document":"529.982.247-25","name":"Beatriz","lastName":"Dantas","email":"be@exemplo.com"}'
# 201  {"document":"52998224725", ...}      a pontuação é normalizada

curl localhost:8080/findNacionalityByPerson/52998224725
# 200  {"name":"Beatriz Dantas","nationality":"Brazil","probability":0.665717}
```

---

## Decisões

O enunciado diz "a critério de quem realiza a prova" quatro vezes. Estas foram
as escolhas e o motivo de cada uma.

### O `{Parametro}` é o documento

É a identidade natural da entidade: único, estável e significativo para o
negócio. Um id autoincremento exporia um detalhe de persistência na API pública.

*Contrapartida:* se o documento for um identificador nacional, ele vira dado
pessoal dentro da URL — e URL vai parar em log de acesso, proxy e histórico do
navegador, em texto puro. Neste escopo a legibilidade de
`GET /list/52998224725` compensa, mas num sistema com dados pessoais reais eu
usaria um id opaco no caminho.

### O documento é um CPF, validado de verdade

O enunciado pede "documento" sem dizer qual. Escolhi **CPF** por ser a
identidade natural de uma pessoa no Brasil: nacional, único e — ao contrário do
RG — com formato padronizado e algoritmo de verificação.

O RG foi descartado por três motivos técnicos: é emitido por estado (cada SSP
numera do seu jeito), a mesma pessoa pode ter vários, e não existe algoritmo
para validá-lo. Um identificador que não é único não serve como identidade.

A validação é a oficial: dois dígitos verificadores por módulo 11. Inclui a
armadilha que muita implementação esquece — `00000000000`, `11111111111` e
afins **satisfazem** o cálculo do dígito verificador e ainda assim não são CPFs
válidos, então são rejeitados à parte.

A forma bruta é validada **antes** da normalização, de propósito: se a limpeza
viesse primeiro, `<script>alert(1)</script>` seria reduzido a dígitos e poderia
passar. Só ponto e hífen — os separadores que o CPF realmente usa — são aceitos
e removidos.

*Contrapartida assumida:* a API rejeita documentos de outros países (DNI, RUT,
passaporte). A mensagem de erro diz explicitamente
`"CPF invalido: digito verificador nao confere"`, para que isso se leia como
regra e não como defeito. Trocar de país significa mudar **apenas** o
`Documento` — nenhum caso de uso, controller ou teste de outra camada muda.

### A validação vive nos tipos, não em Bean Validation

`Documento`, `Nome` e `Email` validam dentro do próprio construtor, então uma
instância inválida não chega a existir em lugar nenhum do sistema.

`@Valid` / `@NotBlank` no DTO cobriria apenas o `POST` — anotação de DTO não
vale para `@PathVariable`, então `GET` e `DELETE` ficariam descobertos. Usar os
dois duplicaria cada regra em dois lugares que podem divergir. Uma regra, um
dono.

### O endpoint de nacionalidade devolve o nome do país

A `api.nationalize.io` responde com código ISO 3166-1 alpha-2 (`"BR"`), e o
enunciado pede o *nome* da nacionalidade. O `java.util.Locale` faz a conversão
usando os dados CLDR que já vêm no JDK — sem dependência extra.

**A consulta usa nome + sobrenome.** Medido contra a API real durante o
desenvolvimento:

| Consulta | Resultado |
|---|---|
| `Beatriz` | 🇪🇸 Espanha 19,8% — errado |
| `Beatriz Dantas` | 🇧🇷 Brasil **66,6%** — certo |
| `Beatriz Dantas da Silva` | 🇧🇷 Brasil 36,5% — certo, menos confiante |
| `Yuki` → `Yuki Tanaka` | Japão 46,3% → **65,5%** |

Nome do meio dilui a previsão, então nome + sobrenome é o ponto ótimo — que é
exatamente a estrutura de campos que o enunciado especifica. A resposta devolve
o nome que foi realmente enviado, para ser autoexplicativa.

Quando o serviço não tem palpite, o endpoint responde `200` com nacionalidade
nula: a pessoa existe, só falta a previsão. Um `404` daria a entender,
erradamente, que a pessoa não está cadastrada.

**O plano gratuito da API permite 25 requisições por dia** (cabeçalho
`x-rate-limit-limit`). Por isso o adapter guarda em cache o que já consultou:
repetir a mesma consulta não gasta cota. E se o limite estourar mesmo assim, a
resposta diz exatamente isso —

```json
{ "error": "EXTERNAL_SERVICE_UNAVAILABLE",
  "message": "limite diario de requisicoes da api.nationalize.io atingido (25/dia no plano gratuito); tente novamente mais tarde" }
```

— em vez de um erro genérico que pareceria defeito da aplicação. O cache, o
timeout e o tratamento do limite vivem todos dentro do adapter: nenhuma outra
camada sabe que existem.

### O armazenamento é em memória

O enunciado permite *"banco de dados, armazenamento em memória, etc."*. Um
`ConcurrentHashMap` atrás da porta `PessoaRepository` faz o avaliador rodar com
um comando, sem instalar nada.

*Consequência:* os dados não sobrevivem a um restart. Trocar por JPA/PostgreSQL
significa acrescentar uma classe adapter — nenhum caso de uso, controller ou
teste de domínio muda. Essa substituibilidade é justamente o motivo da porta
existir.

### A autenticação protege as operações de escrita

`POST /registrarName` e `DELETE /list/{document}` exigem token; os três `GET`
ficam abertos.

`DELETE` é a operação mais crítica — destrutiva e irreversível — e o `POST`
também altera estado. Leitura não muda nada, e deixá-la aberta permite avaliar
a API sem atrito.

**Token opaco em vez de JWT.** A aplicação roda em instância única, então a
validação sem estado do JWT não traria benefício algum aqui, enquanto o token
opaco dá revogação imediata. Escalar horizontalmente moveria o mapa de tokens
para um Redis, ou trocaria por JWT aceitando manter uma lista de revogados.

**Escrito à mão em vez de usar o `spring-boot-starter-security`.** O requisito é
pequeno, e um `OncePerRequestFilter` de 40 linhas mantém cada linha do controle
de acesso visível e explicável. Só o `spring-security-crypto` entra, para o
BCrypt — ele não traz cadeia de filtros nem autoconfiguração.

Detalhes que valem registro:

- A senha é guardada como hash BCrypt, nunca em texto puro.
- O hash é verificado mesmo quando o usuário está errado, para o tempo de
  resposta não revelar quais usuários existem.
- Usuário errado e senha errada devolvem a mesma mensagem.
- No navegador o token vive apenas em memória — não vai para `localStorage` nem
  aparece no código-fonte da página.
- As linhas da tabela são montadas com `createElement`/`textContent`, não com
  `innerHTML`, então um nome com HTML dentro aparece como texto e nunca executa.

### Arquitetura: Clean Architecture com parte do DDD tático

```
domain/          entidades, value objects, portas, erros de domínio  — Java puro
application/     casos de uso                                        — Java puro
infrastructure/  repositório em memória, cliente HTTP, autenticação
presentation/    controllers, DTOs, tratamento de erro, página estática
```

As dependências apontam para dentro: `domain` e `application` não importam
framework nenhum, e é por isso que os casos de uso são testados sem subir o
Spring e sem acessar a rede.

**Usados:** value object, entity, porta de repositório, porta de gateway.
**Deixados de fora de propósito:** aggregate, domain event, factory,
specification, bounded context. Este domínio tem uma entidade e quatro
atributos — aplicar o catálogo inteiro adicionaria indireção sem reduzir
complexidade.

Os cinco casos de uso estão agrupados em dois services em vez de cinco classes
de um método, e os DTOs ficam aninhados no controller que os usa. As duas
escolhas trocam cerimônia por legibilidade neste tamanho.

---

## Testes

```bash
./mvnw -q test
```

56 testes:

| Suíte | O que cobre |
|---|---|
| `DocumentoTest`, `NomeTest`, `EmailTest` | regras de validação, normalização e payloads de injeção |
| `NacionalidadeServiceTest` | caso de uso com adapters falsos — sem rede, sem Spring |
| `ApiEndToEndTest` | sobe a aplicação numa porta real e exercita todos os endpoints por HTTP, autenticação incluída |

A suíte end-to-end não chama o serviço externo; aquele caminho é coberto por
implementações falsas, então tudo roda sem internet.

---

## Limitações, e o que viria depois

- **Os dados não persistem** entre execuções. Está a uma classe adapter de um
  banco de verdade.
- **O cache da chamada externa é simples e não expira.** Resolve o limite de 25
  requisições por dia, mas num sistema real teria tempo de vida e limite de
  tamanho. Como está isolado no adapter, evoluir não toca em nenhuma outra
  camada.
- **Um único usuário fixo.** O enunciado não pede gestão de usuários. Usuários
  reais viveriam no repositório, atrás da mesma porta.
- **Sem conteinerização.** Rodar exige um comando e um JDK; containerizar seria
  o passo natural para deploy.
- **O documento é dado pessoal na URL** — discutido acima.
