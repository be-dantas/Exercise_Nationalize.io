# Atalhos para os comandos do projeto.
# Opcional: tudo aqui pode ser feito direto com ./mvnw (ver README).

# Tira o banner e os avisos da JVM da saida dos testes; falhas continuam visiveis.
SILENCIO = -Dspring.main.banner-mode=off -DargLine="-XX:+EnableDynamicAgentLoading -Xshare:off"

.PHONY: run test build clean help

## run   - sobe a aplicacao em http://localhost:8080
run:
	@./mvnw -q spring-boot:run

## test  - roda a suite de testes
test:
	@./mvnw -q test $(SILENCIO) && echo "todos os testes passaram"

## build - gera o jar em target/
build:
	@./mvnw -q clean package

## clean - apaga os arquivos gerados pelo build
clean:
	@./mvnw -q clean

help:
	@grep -E "^## " Makefile | sed "s/^## //"
