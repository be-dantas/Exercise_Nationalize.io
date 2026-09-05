package com.bedantas.prova.presentation;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de vida da aplicacao. Nao faz parte dos requisitos do enunciado:
 * existe para provar que o ciclo build/run/test funciona (fatia 0).
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
