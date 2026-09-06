package com.bedantas.personregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class CadastroPessoasApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext contexto =
                SpringApplication.run(CadastroPessoasApplication.class, args);
        anunciar(contexto.getEnvironment());
    }

    private static void anunciar(Environment ambiente) {
        String porta = ambiente.getProperty("server.port", "8080");
        String chave = ambiente.getProperty("auth.api-key", "");

        System.out.printf("""

                  cadastro.  ->  http://localhost:%s

                  chave de demonstracao: %s
                  encerrar: Ctrl+C

                """, porta, chave);
    }
}
