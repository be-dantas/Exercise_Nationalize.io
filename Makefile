# Atalhos para os comandos do projeto.
# Opcional: tudo aqui pode ser feito direto com ./mvnw (ver README).

# Tira o banner e os avisos da JVM da saida dos testes; falhas continuam visiveis.
SILENCIO = -Dspring.main.banner-mode=off -DargLine="-XX:+EnableDynamicAgentLoading -Xshare:off"

.PHONY: run test build clean re help

## run   - sobe a aplicacao em http://localhost:8080
run:
	@./mvnw -q spring-boot:run

## test  - roda a suite de testes
test:
	@./mvnw -q test $(SILENCIO) && echo "todos os testes passaram"

## build - gera o jar em target/
build:
	@./mvnw -q clean package $(SILENCIO) && echo "jar gerado em target/"

## clean - apaga tudo que o build gerou (target/: .class e .jar)
##         no Maven nao existe fclean: target/ guarda os dois, entao
##         clean ja faz o que fclean faz em C
clean:
	@./mvnw -q clean

## re    - clean + build
re: clean build

help:
	@grep -E "^## " Makefile | sed "s/^## //"
