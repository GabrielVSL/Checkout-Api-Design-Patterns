package br.com.devlopes.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Catálogo de cupons promocionais válidos da plataforma.
 * Centraliza as regras de percentual e valor mínimo para evitar duplicação
 * entre o elo de validação (Chain) e o cálculo financeiro do pedido (Facade).
 */
public enum CupomDesconto {

    DEV10(new BigDecimal("0.10"), BigDecimal.ZERO),
    BEMVINDO20(new BigDecimal("0.20"), new BigDecimal("100.00"));

    private final BigDecimal percentual;
    private final BigDecimal valorMinimoItens;

    CupomDesconto(BigDecimal percentual, BigDecimal valorMinimoItens) {
        this.percentual = percentual;
        this.valorMinimoItens = valorMinimoItens;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public BigDecimal getValorMinimoItens() {
        return valorMinimoItens;
    }

    public boolean atendeValorMinimo(BigDecimal valorItens) {
        return valorItens.compareTo(valorMinimoItens) >= 0;
    }

    public BigDecimal calcularDesconto(BigDecimal valorItens) {
        return valorItens.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
    }

    public static Optional<CupomDesconto> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        String codigoNormalizado = codigo.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(cupom -> cupom.name().equals(codigoNormalizado))
                .findFirst();
    }
}
