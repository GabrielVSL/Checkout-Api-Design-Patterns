package br.com.devlopes.checkout.chain;

import br.com.devlopes.checkout.dto.CheckoutRequest;
import br.com.devlopes.checkout.model.CupomDesconto;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * <h2>Elo 3 da Chain of Responsibility: Validação de Cupom</h2>
 * Caso um cupom promocional tenha sido informado, valida sua existência no catálogo
 * e se o subtotal dos itens atinge o valor mínimo exigido.
 *
 * <p><b>Padrão Singleton (Spring IoC):</b> Bean stateless gerenciado como Singleton pelo Spring
 * e posicionado como terceiro elo da cadeia via {@code @Order(3)}.</p>
 */
@Component
@Order(3)
public class ValidacaoCupomHandler implements ValidacaoPedidoHandler {

    @Override
    public void validar(CheckoutRequest request) {
        String codigoInformado = request.cupomDesconto();
        if (codigoInformado == null || codigoInformado.isBlank()) {
            return;
        }

        CupomDesconto cupom = CupomDesconto.buscarPorCodigo(codigoInformado)
                .orElseThrow(() -> new ValidacaoCheckoutException(
                        "Cupom de desconto inválido ou expirado: '" + codigoInformado + "'."
                ));

        BigDecimal valorItens = request.calcularValorTotalItens();
        if (!cupom.atendeValorMinimo(valorItens)) {
            throw new ValidacaoCheckoutException(
                    String.format(
                            Locale.forLanguageTag("pt-BR"),
                            "O cupom '%s' exige valor mínimo em itens de R$ %.2f.",
                            cupom.name(), cupom.getValorMinimoItens()
                    )
            );
        }
    }
}
