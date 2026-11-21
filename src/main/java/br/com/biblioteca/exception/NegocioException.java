package br.com.biblioteca.exception;

/**
 * Exceção para violações de regras de negócio (RN01 a RN09).
 * A mensagem é escrita para o usuário final, pois será exibida na tela.
 *
 * Herda de Exception, então é CHECKED: o compilador obriga quem chama
 * um método que a lança a tratá-la (try/catch) ou repassá-la (throws).
 * Assim, nenhuma tela consegue "esquecer" de tratar uma regra violada.
 */
public class NegocioException extends Exception {

    // Exigido por toda classe serializável (Exception é); evita aviso do compilador
    private static final long serialVersionUID = 1L;

    public NegocioException(String mensagem) {
        super(mensagem);   // repassa a mensagem ao construtor de Exception
    }
}