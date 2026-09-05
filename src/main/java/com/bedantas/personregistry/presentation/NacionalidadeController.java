package com.bedantas.personregistry.presentation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.bedantas.personregistry.application.NacionalidadeService;
import com.bedantas.personregistry.domain.Documento;

@RestController
public class NacionalidadeController {

    private final NacionalidadeService servico;

    public NacionalidadeController(NacionalidadeService servico) {
        this.servico = servico;
    }

    /**
     * Devolve o NOME do pais, nao o codigo ISO - e o que o enunciado pede.
     *
     * Quando o servico externo nao tem palpite para o nome, respondemos 200 com
     * nationality nula: a pessoa existe, so nao ha previsao. Um 404 aqui daria a
     * entender, erradamente, que a pessoa nao esta cadastrada.
     *
     * O campo "name" devolve o nome exatamente como foi enviado ao servico
     * externo (nome + sobrenome), para a resposta ser autoexplicativa.
     */
    public record NacionalidadeResponse(String name, String nationality, Double probability) {
    }

    @GetMapping("/findNacionalityByPerson/{document}")
    public NacionalidadeResponse descobrir(@PathVariable String document) {
        var resultado = servico.descobrir(new Documento(document));
        return resultado.previsao()
                .map(n -> new NacionalidadeResponse(
                        resultado.pessoa().nomeParaPrevisao(), n.nomeDoPais(), n.probabilidade()))
                .orElseGet(() -> new NacionalidadeResponse(
                        resultado.pessoa().nomeParaPrevisao(), null, null));
    }
}
