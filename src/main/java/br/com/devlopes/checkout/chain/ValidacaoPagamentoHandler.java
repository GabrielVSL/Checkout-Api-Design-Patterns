package br.com.devlopes.checkout.chain;

import br.com.devlopes.checkout.dto.CheckoutRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * <h2>Elo 2 da Chain of Responsibility: Validação de Pagamento</h2>
 * Valida se a forma de pagamento informada é aceita pela plataforma e se respeita
 * as políticas de limite de risco (ex.: teto para emissão de boleto).
 *
 * <p><b>Padrão Singleton (Spring IoC):</b> Bean stateless gerenciado como Singleton pelo Spring
 * e posicionado como segundo elo da cadeia via {@code @Order(2)}.</p>
 */
@Component
@Order(2)
public class ValidacaoPagamentoHandler implements ValidacaoPedidoHandler {

    private static final Set<String> FORMAS_PAGAMENTO_ACEITAS = Set.of(
            "CARTAO_CREDITO",
            "PIX",
            "BOLETO"
    );

    private static final BigDecimal LIMITE_MAXIMO_BOLETO = new BigDecimal("5000.00");

    @Override
    public void validar(CheckoutRequest request) {
        String formaPagamento = request.formaPagamento().trim().toUpperCase(Locale.ROOT);

        if (!FORMAS_PAGAMENTO_ACEITAS.contains(formaPagamento)) {
            throw new ValidacaoCheckoutException(
                    "Forma de pagamento inválida: '" + request.formaPagamento()
                            + "'. Aceitas: BOLETO, CARTAO_CREDITO, PIX."
            );
        }

        BigDecimal valorItens = request.calcularValorTotalItens();
        if ("BOLETO".equals(formaPagamento) && valorItens.compareTo(LIMITE_MAXIMO_BOLETO) > 0) {
            throw new ValidacaoCheckoutException(
                    "Pagamentos via BOLETO são permitidos apenas para pedidos de até R$ 5.000,00."
            );
        }
    }
}
