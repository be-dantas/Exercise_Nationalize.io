package com.bedantas.prova.domain;

/**
 * Entity cuja identidade e o documento.
 *
 * Modelada como record porque neste escopo ela e imutavel e nunca e comparada
 * por igualdade - o repositorio a indexa pelo documento.
 *
 * Nao ha validacao aqui: cada campo ja e um Value Object que so existe valido.
 */
public record Pessoa(Documento documento, Nome nome, Nome sobrenome, Email email) {

    /**
     * Nome enviado ao servico de previsao de nacionalidade.
     *
     * Usa nome + sobrenome, nao apenas o primeiro nome. Medicao feita contra a
     * api.nationalize.io durante o desenvolvimento:
     *
     *   "Beatriz"                 -> ES 19,8%  (errado)
     *   "Beatriz Dantas"          -> BR 66,6%  (certo)
     *   "Beatriz Dantas da Silva" -> BR 36,5%  (certo, porem menos confiante)
     *
     * Nome do meio dilui a previsao, entao nome + sobrenome e o ponto otimo -
     * que e exatamente a estrutura de campos pedida pelo enunciado.
     */
    public Nome nomeParaPrevisao() {
        return new Nome(nome.valor() + " " + sobrenome.valor());
    }
}
