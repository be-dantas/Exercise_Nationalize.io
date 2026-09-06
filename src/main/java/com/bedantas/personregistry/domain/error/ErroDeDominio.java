package com.bedantas.personregistry.domain.error;

public abstract class ErroDeDominio extends RuntimeException {

    protected ErroDeDominio(String mensagem) {
        super(mensagem);
    }

    protected ErroDeDominio(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public static class DadoInvalido extends ErroDeDominio {
        public DadoInvalido(String mensagem) {
            super(mensagem);
        }
    }

    public static class PessoaNaoEncontrada extends ErroDeDominio {
        public PessoaNaoEncontrada(String documento) {
            super("nenhuma pessoa cadastrada com o documento " + documento);
        }
    }

    public static class PessoaJaCadastrada extends ErroDeDominio {
        public PessoaJaCadastrada(String documento) {
            super("ja existe uma pessoa cadastrada com o documento " + documento);
        }
    }

    public static class ServicoExternoIndisponivel extends ErroDeDominio {
        public ServicoExternoIndisponivel(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }
}
