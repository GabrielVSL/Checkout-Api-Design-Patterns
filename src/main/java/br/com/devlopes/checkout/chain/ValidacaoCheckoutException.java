package br.com.devlopes.checkout.chain;

/**
 * Exceção de domínio lançada quando algum elo da cadeia de validação
 * ou regra de negócio do checkout falha.
 */
public class ValidacaoCheckoutException extends RuntimeException {

    public ValidacaoCheckoutException(String message) {
        super(message);
    }
}
