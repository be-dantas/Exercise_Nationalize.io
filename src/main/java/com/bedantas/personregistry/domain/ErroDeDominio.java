package com.bedantas.personregistry.domain;

/**
 * Todas as falhas de negocio do sistema, num lugar so.
 *
 * Cada subclasse vira um status HTTP diferente no TratadorGlobalDeErros.
 * Manter a lista reunida aqui deixa visivel, numa tela, tudo que pode dar
 * errado neste dominio - e cada uma continua sendo um tipo proprio, entao o
 * tratamento por tipo continua funcionando normalmente.
 */
public abstract class ErroDeDominio extends RuntimeException {

    protected ErroDeDominio(String mensagem) {
        super(mensagem);
    }

    protected ErroDeDominio(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    /** Dado que nao satisfaz as regras de formato do dominio. -> HTTP 400 */
    public static class DadoInvalido extends ErroDeDominio {
        public DadoInvalido(String mensagem) {
            super(mensagem);
        }
    }

    /** Documento com formato valido, mas ninguem cadastrado com ele. -> HTTP 404 */
    public static class PessoaNaoEncontrada extends ErroDeDominio {
        public PessoaNaoEncontrada(String documento) {
            super("nenhuma pessoa cadastrada com o documento " + documento);
        }
    }

    /** Documento ja existe no sistema. -> HTTP 409 (conflito, nao erro de formato) */
    public static class PessoaJaCadastrada extends ErroDeDominio {
        public PessoaJaCadastrada(String documento) {
            super("ja existe uma pessoa cadastrada com o documento " + documento);
        }
    }

    /** Usuario ou senha nao conferem. -> HTTP 401 */
    public static class CredencialInvalida extends ErroDeDominio {
        public CredencialInvalida() {
            super("usuario ou senha invalidos");
        }
    }

    /** A API de terceiro falhou, expirou ou recusou. -> HTTP 503 */
    public static class ServicoExternoIndisponivel extends ErroDeDominio {
        public ServicoExternoIndisponivel(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }
}
