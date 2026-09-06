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
  nacion.  ->  http://localhost:8080

  chave de demonstracao: chave-de-demonstracao-2026
  encerrar: Ctrl+C
```

Depois é só abrir **http://localhost:8080**.

| O que | Comando |
|---|---|
| Rodar | `./mvnw -q spring-boot:run` |
| Testar | `./mvnw -q test` |
| Apagar o que o build gerou | `./mvnw -q clean` |
| Gerar o jar | `./mvnw -q clean package` |
| Rodar o jar sozinho | `java -jar target/*.jar` |

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

---

## Decisões

O enunciado diz "a critério de quem realiza a prova" quatro vezes. Estas são as
escolhas e o motivo de cada uma.

### O framework é Spring Boot

Java era obrigatório; o framework, livre. Escolhi **Spring Boot 4.1.1**, padrão do
mercado Java, e dele usei apenas o `starter-webmvc` — duas dependências de
produção no `pom.xml` inteiro.

Deixei de fora, por decisão: **Spring Security** (um filtro de 86 linhas atende e
continua explicável), **JPA** (o enunciado permite memória), **Lombok** (`record`
já resolve), **Bean Validation** (a validação vive nos tipos) e **Docker** (um
processo único, sem serviço externo para orquestrar).

### O `{Parametro}` é o documento

Identidade natural da entidade: único, estável e significativo para o negócio. Um
id autoincremento exporia um detalhe de persistência na API pública.

*Contrapartida:* o CPF vira dado pessoal na URL, que vai parar em log e
histórico. Com dados reais, eu usaria um id opaco no caminho.

### O documento é um CPF, validado pelo dígito verificador

CPF é a identidade natural de uma pessoa no Brasil. O RG foi descartado por ser
estadual, não único e sem algoritmo de validação.

A validação é o módulo 11 sobre os dois dígitos, e rejeita à parte a armadilha de
`00000000000` e afins, que **passam** no cálculo sem serem CPFs válidos. A forma
bruta é conferida **antes** da normalização: limpando primeiro,
`<script>alert(1)</script>` viraria dígitos e passaria.

*Contrapartida:* documentos de outros países são recusados; a mensagem cita o
dígito verificador para que isso se leia como regra, não como defeito.

### A validação vive nos tipos, não em Bean Validation

Os value objects validam no construtor, então instância inválida não chega a
existir. `@Valid` cobriria só o `POST` — não vale para `@PathVariable` — e usar
os dois duplicaria cada regra em lugares que podem divergir.

### O endpoint de nacionalidade devolve o nome do país

A API externa responde com código ISO (`"BR"`) e o enunciado pede o *nome*:
`java.util.Locale` converte com os dados que já vêm no JDK.

**A consulta usa nome + sobrenome**, medido contra a API durante o
desenvolvimento:

| Consulta | Resultado |
|---|---|
| `Beatriz` | 🇪🇸 Espanha 19,8% — errado |
| `Beatriz Dantas` | 🇧🇷 Brasil **66,6%** — certo |
| `Beatriz Dantas da Silva` | 🇧🇷 Brasil 36,5% — menos confiante |

Nome do meio dilui a previsão, então nome + sobrenome é o ponto ótimo — que é
exatamente a estrutura de campos do enunciado.

O plano gratuito permite 25 requisições por dia, então o adapter mantém cache do
que já consultou, e o estouro de cota é dito explicitamente na resposta. Sem
palpite para o nome, responde `200` com nacionalidade nula: a pessoa existe, só
falta a previsão.

### O armazenamento é em memória

Permitido pelo enunciado, e faz o avaliador rodar com um comando sem instalar
nada. Os dados não sobrevivem a um restart; trocar por JPA/PostgreSQL é
acrescentar uma classe adapter, sem tocar em caso de uso, controller ou teste de
domínio.

### A autenticação cobre todas as APIs, por chave

Das duas opções do enunciado — a API mais crítica ou todas — escolhi a segunda,
com **chave de API**: as cinco exigem o cabeçalho `X-API-Key`. Fica de fora só a
página, que precisa carregar para que alguém digite a chave. O mecanismo mais
simples que atende foi escolhido de propósito: proporcional a um cadastro de
dados fictícios, com o controle de acesso inteiro em 86 linhas.

**A chave nunca aparece no código da página** — é digitada no portão e vive só na
memória da aba, dentro de uma closure. Quem não a tem não encontra nada no
DevTools: ela não está em nenhum arquivo que o navegador baixa, nem alcançável
pelo console. Quem já digitou vê a própria chave nos cabeçalhos das requisições
que fez, o que é inerente a HTTP e não vaza nada para terceiros — é o mesmo que
acontece com o cookie de sessão de qualquer site.

Embutir a chave no JavaScript é justamente o que se evitou: aí qualquer visitante
leria o segredo sem ter credencial nenhuma. Criptografar no cliente não
resolveria, porque a chave de descriptografia estaria no mesmo arquivo.

A comparação é em **tempo constante**, já que `String.equals` retorna no primeiro
caractere diferente e essa diferença pode ser medida para recuperar a chave.

*Limitações assumidas:* a chave não expira, não é revogável sem reiniciar e não há
noção de usuário. Com dados reais, seria login com senha em hash, token com
expiração e auditoria.

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

As pastas nomeiam os padrões, então a listagem já descreve o domínio: uma
entidade, cinco objetos de valor, duas portas e um tipo de falha.

As portas ficam em `domain/port` e não em `application`: elas usam apenas tipos
de domínio e fazem parte da linguagem do negócio — é a leitura do DDD e da
arquitetura hexagonal. A Clean Architecture na formulação original as coloca na
camada de casos de uso; as duas respeitam a regra da dependência, muda apenas
onde o arquivo é arquivado. O `domain/` compila isoladamente, sem as demais
camadas.

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
