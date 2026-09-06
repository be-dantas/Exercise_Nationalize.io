package com.bedantas.personregistry.domain.entity;

import com.bedantas.personregistry.domain.valueobject.Documento;
import com.bedantas.personregistry.domain.valueobject.Email;
import com.bedantas.personregistry.domain.valueobject.Nome;
import com.bedantas.personregistry.domain.valueobject.Sobrenome;

public record Pessoa(Documento documento, Nome nome, Sobrenome sobrenome, Email email) {
    
    public String nomeParaPrevisao() {
        return nome.valor() + " " + sobrenome.valor();
    }
}
