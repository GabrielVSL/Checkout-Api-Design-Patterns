package br.com.devlopes.checkout.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementação concreta do padrão <b>Strategy</b> para cálculo de frete via <b>SEDEX</b>.
 *
 * <p><b>Padrão Singleton (Spring IoC):</b> A anotação {@code @Component("SEDEX")} instrui o
 * container do Spring a criar <b>uma única instância</b> (escopo {@code singleton} padrão)
 * desta classe sem estado mutável e registrá-la sob a chave {@code "SEDEX"} no mapa
 * {@code Map<String, FreteStrategy>} injetado na {@code CheckoutFacade}.</p>
 */
@Component("SEDEX")
public class FreteSedex implements FreteStrategy {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("24.90");
    private static final BigDecimal VALOR_POR_KG = new BigDecimal("4.50");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg, BigDecimal valorItens) {
        return TAXA_FIXA
                .add(pesoTotalKg.multiply(VALOR_POR_KG))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getModalidade() {
        return "SEDEX";
    }
}
