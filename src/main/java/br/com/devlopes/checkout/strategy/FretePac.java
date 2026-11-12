package br.com.devlopes.checkout.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementação concreta do padrão <b>Strategy</b> para cálculo de frete via <b>PAC</b>.
 *
 * <p><b>Padrão Singleton (Spring IoC):</b> Por ser um {@code @Component} com escopo padrão
 * do Spring ({@code singleton}), apenas uma instância desta classe existe em memória durante
 * todo o ciclo de vida da aplicação, sendo indexada pela chave {@code "PAC"} no
 * {@code Map<String, FreteStrategy>}.</p>
 */
@Component("PAC")
public class FretePac implements FreteStrategy {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.90");
    private static final BigDecimal VALOR_POR_KG = new BigDecimal("2.10");
    private static final BigDecimal VALOR_MINIMO_FRETE_GRATIS = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg, BigDecimal valorItens) {
        if (valorItens.compareTo(VALOR_MINIMO_FRETE_GRATIS) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return TAXA_FIXA
                .add(pesoTotalKg.multiply(VALOR_POR_KG))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getModalidade() {
        return "PAC";
    }
}
