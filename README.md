# Person Registry API

API REST para cadastrar pessoas e prever a provável nacionalidade delas a partir
de um serviço externo, com interface web e autenticação nas operações de escrita.

Java 21 · Spring Boot 4.1.1 · Maven · sem servidor de banco, sem Docker.

---

## Como rodar

Requisito único: um **JDK 21 ou superior**. O Maven não precisa estar instalado —
o wrapper baixa a versão certa sozinho.

```bash
./mvnw -q spring-boot:run       # Linux e macOS
.\mvnw.cmd -q spring-boot:run   # Windows (PowerShell ou cmd)
```

Ao subir, imprime:

```
  nacion.  ->  http://localhost:8080

  chave de demonstracao: chave-de-demonstracao-2026
  encerrar: Ctrl+C
```

Depois é só abrir **http://localhost:8080**.

| O que | Linux · macOS | Windows |
|---|---|---|
| Rodar | `./mvnw -q spring-boot:run` | `.\mvnw.cmd -q spring-boot:run` |
| Testar | `./mvnw -q test` | `.\mvnw.cmd -q test` |
| Apagar o que o build gerou | `./mvnw -q clean` | `.\mvnw.cmd -q clean` |
| Gerar o jar | `./mvnw -q clean package` | `.\mvnw.cmd -q clean package` |
| Rodar o jar sozinho | `java -jar target/*.jar` | `java -jar target\person-registry-api-0.0.1-SNAPSHOT.jar` |

O `.\` na frente do `mvnw.cmd` é obrigatório no PowerShell, que não procura
executáveis na pasta atual. Se o projeto chegou como `.zip` e o Linux ou macOS
recusar o `./mvnw` com *permission denied*, rode `chmod +x mvnw` uma vez.

O `clean` apaga a pasta `target/`, que guarda os `.class` e o jar e é recriada
a cada build — nada dela vai para o repositório. Vale rodar quando o resultado
parecer inconsistente com o código, por exemplo depois de renomear ou mover
arquivos: o compilador é incremental e pode reaproveitar classes que já não
correspondem ao fonte.

O `-q` deixa o build silencioso: **nenhuma saída significa sucesso**. Falha
continua aparecendo por inteiro — teste quebrado mostra a asserção, erro de
compilação mostra arquivo e linha, e falha ao subir mostra o diagnóstico do
Spring. Tire o `-q` para ver o log completo do Maven.


---

## O que a prova pede

| Requisito | Onde está |
|---|---|
| Java + algum sistema de persistência | Java 21; armazenamento em memória, explicitamente permitido pelo enunciado |
| `POST /registrarName` com documento, nome, sobrenome e e-mail | `PessoaController` |
| `GET /list` | `PessoaController` |
| `GET /list/{param}` | `PessoaController`, o parâmetro é o documento |
| `DELETE /list/{param}` | `PessoaController` |
| `GET /findNacionalityByPerson/{param}` devolvendo o **nome** da nacionalidade | `NacionalidadeController` + `NationalizeClient` |
| Pelo menos uma validação de tipo por API | Value Objects `Documento`, `Nome`, `Sobrenome`, `Email` |
| Autenticação (opção geral, para todas as APIs) | `FiltroDeAutenticacao`, cabeçalho `X-API-Key` |
| Interface web consumindo ao menos uma API | `static/index.html`, consome todas |

---

## A API

| Método | Rota | Auth | Sucesso | Erros |
|---|---|:---:|---|---|
| `POST` | `/registrarName` | 🔒 | `201` + `Location: /list/{document}` | `400` `401` `409` |
| `GET` | `/list` | 🔒 | `200` lista ordenada por nome | — |
| `GET` | `/list/{document}` | 🔒 | `200` | `400` `404` |
| `DELETE` | `/list/{document}` | 🔒 | `204` | `400` `401` `404` |
| `GET` | `/findNacionalityByPerson/{document}` | 🔒 | `200` `{name, nationality, probability}` | `400` `404` `503` |

Todo erro usa o mesmo formato, exemplo:

```json
{ "error": "INVALID_DATA", "message": "e-mail invalido: nao-e-email" }
```

`INVALID_DATA` · `NOT_FOUND` · `ALREADY_EXISTS` · `UNAUTHORIZED` ·
`MALFORMED_REQUEST` · `EXTERNAL_SERVICE_UNAVAILABLE`

O contrato JSON está em inglês por ser a interface externa da API; o código
está em português, acompanhando a língua do enunciado.

### Exemplo de uso

```bash
CHAVE='chave-de-demonstracao-2026'

curl -X POST localhost:8080/registrarName \
  -H "X-API-Key: $CHAVE" -H 'Content-Type: application/json' \
  -d '{"document":"529.982.247-25","name":"Beatriz","lastName":"Dantas","email":"be@exemplo.com"}'
# 201  {"document":"52998224725", ...}      a pontuação é normalizada

curl -H "X-API-Key: $CHAVE" localhost:8080/findNacionalityByPerson/52998224725
# 200  {"name":"Beatriz Dantas","nationality":"Brazil","probability":0.665717}
```

No Windows, `curl` é apelido de `Invoke-WebRequest` e não aceita os mesmos
argumentos. O equivalente nativo do PowerShell:

```powershell
$chave = 'chave-de-demonstracao-2026'
$corpo = '{"document":"529.982.247-25","name":"Beatriz","lastName":"Dantas","email":"be@exemplo.com"}'

Invoke-RestMethod -Method Post http://localhost:8080/registrarName `
  -Headers @{ 'X-API-Key' = $chave } -ContentType 'application/json' -Body $corpo

Invoke-RestMethod http://localhost:8080/findNacionalityByPerson/52998224725 `
  -Headers @{ 'X-API-Key' = $chave }
```

---

## Decisões

O enunciado diz "a critério de quem realiza a prova". Estas são as
escolhas e o motivo de cada uma.

Esta foi minha primeira vez escrevendo Java. Aprender uma linguagem nova nunca
foi o obstáculo, minha base em C++ ajudou demais nesse projeto.

Também ajudou ter feito o **WebServer** da 42, onde implementei o protocolo
HTTP na mão: parsear a requisição, montar a resposta, gerenciar as conexões.
Resolver o mesmo problema com um framework, sabendo exatamente o que ele faz por
baixo, foi a parte mais interessante deste projeto.

### Framework: Spring Boot

Java era obrigatório; o framework, livre. Escolhi **Spring Boot 4.1.1**, padrão do
mercado Java, e dele usei apenas o `starter-webmvc` — duas dependências de
produção no `pom.xml` inteiro.

Ficaram de fora por decisão: **Spring Security**, **JPA**, **Lombok**, **Bean
Validation** e **Docker**.

### `{Parametro}`: documento (CPF)

CPF é a identidade natural de uma pessoa no Brasil. O RG foi descartado por ser
estadual, não único e sem algoritmo de validação. Em contrapartida, documentos
de outros países são recusados; a mensagem cita o dígito verificador para que
isso se leia como regra, não como defeito.

A validação é o módulo 11 sobre os dois dígitos, e rejeita à parte a armadilha de
`00000000000` e afins.

### Validação: vive nos tipos.

Os value objects validam no construtor, então instância inválida não chega a
existir.

### Armazenamento: em memória

Permitido pelo enunciado, e faz o avaliador rodar com um comando sem instalar
nada. Os dados não sobrevivem a um restart.

### Autenticação: cobre todas as APIs, por chave

Das duas opções do enunciado — a API mais crítica ou todas — escolhi a segunda,
com **chave de API**: as cinco exigem o cabeçalho `X-API-Key`. Fica de fora só a
página, que precisa carregar para que alguém digite a chave.

**A chave nunca aparece no código da página** — é digitada no portão e vive só na
memória da aba, dentro de uma closure. Quem não a tem não encontra nada no
DevTools: ela não está em nenhum arquivo que o navegador baixa, nem alcançável
pelo console. Quem já digitou vê a própria chave nos cabeçalhos das requisições
que fez, o que é inerente a HTTP e não vaza nada para terceiros — é o mesmo que
acontece com o cookie de sessão de qualquer site.

### Arquitetura: Clean Architecture com parte do DDD tático

```
domain/
  entity/        Pessoa — tem identidade (o documento)        — Java puro
  valueobject/   Documento, Nome, Sobrenome, Email,
                 Nacionalidade — definidos pelos valores      — Java puro
  port/          contratos com o mundo externo                — Java puro
  error/         falhas de negócio                            — Java puro
application/     casos de uso                                 — Java puro
infrastructure/  repositório em memória, cliente HTTP, autenticação
presentation/    controllers, DTOs, tratamento de erro, página estática
```

As portas ficam em `domain/port` e não em `application`: elas usam apenas tipos
de domínio e fazem parte da linguagem do negócio — é a leitura do DDD e da
arquitetura hexagonal. A Clean Architecture na formulação original as coloca na
camada de casos de uso; as duas respeitam a regra da dependência, muda apenas
onde o arquivo é arquivado. O `domain/` compila isoladamente, sem as demais
camadas.

---

## Testes

```bash
./mvnw -q test        # Linux e macOS
.\mvnw.cmd -q test    # Windows
```

**88 testes**, nenhum deles precisa de rede:

| Suíte | Testes | O que cobre |
|---|---:|---|
| `DocumentoTest` | 24 | CPF: dígito verificador, dígitos repetidos, payloads de injeção |
| `NomeTest` / `SobrenomeTest` | 29 | regras de nome, normalização e a mensagem citando o campo certo |
| `EmailTest` | 12 | formato e normalização |
| `PessoaTest` | 3 | a entidade e o nome enviado à previsão |
| `PessoaServiceTest` | 3 | duplicidade, e concorrência: 100 rodadas de 8 threads no mesmo documento |
| `NacionalidadeServiceTest` | 5 | caso de uso com adapters falsos — sem rede, sem Spring |
| `ApiEndToEndTest` | 11 | sobe numa porta real e exercita os cinco endpoints por HTTP, autenticação incluída |
| contexto Spring | 1 | a aplicação sobe |
