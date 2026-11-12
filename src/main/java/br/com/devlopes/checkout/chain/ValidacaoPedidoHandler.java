package br.com.devlopes.checkout.chain;

import br.com.devlopes.checkout.dto.CheckoutRequest;

import java.util.Objects;

/**
 * <h2>Padrão Chain of Responsibility (GoF)</h2>
 * Contrato funcional de cada elo (handler) no pipeline de validação do checkout.
 *
 * <p>Utiliza composição funcional do Java 21 ({@link #encadear(ValidacaoPedidoHandler)})
 * aliada à ordenação declarativa do Spring ({@code @Order}) para montar a corrente de
 * validação sem estado mutável nos beans Singleton.</p>
 */
@FunctionalInterface
public interface ValidacaoPedidoHandler {

    /**
     * Executa a validação deste elo sobre os dados do checkout.
     *
     * @param request dados da solicitação de checkout
     * @throws ValidacaoCheckoutException caso a regra de negócio deste elo seja violada
     */
    void validar(CheckoutRequest request);

    /**
     * Encadeia este handler com o próximo elo da corrente (Chain of Responsibility).
     * Se a validação atual for aprovada, delega automaticamente para o {@code proximo}.
     *
     * @param proximo próximo handler a ser executado na sequência
     * @return handler composto que executa este elo seguido do próximo
     */
    default ValidacaoPedidoHandler encadear(ValidacaoPedidoHandler proximo) {
        Objects.requireNonNull(proximo, "O próximo handler da cadeia não pode ser nulo");
        return request -> {
            this.validar(request);
            proximo.validar(request);
        };
    }
}
