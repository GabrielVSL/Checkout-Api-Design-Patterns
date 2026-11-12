package br.com.devlopes.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO imutável (Java 21 Record) que representa um item enviado na requisição de checkout.
 */
public record ItemCheckoutRequest(

        @Schema(description = "Identificador único do produto", example = "PROD-001")
        @NotBlank(message = "O ID do produto é obrigatório")
        String produtoId,

        @Schema(description = "Nome do produto", example = "Teclado Mecânico Keychron K2")
        @NotBlank(message = "O nome do produto é obrigatório")
        String nomeProduto,

        @Schema(description = "Quantidade comprada", example = "2")
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade mínima por item é 1")
        Integer quantidade,

        @Schema(description = "Preço unitário do produto em reais", example = "349.90")
        @NotNull(message = "O preço unitário é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço unitário deve ser maior que zero")
        BigDecimal precoUnitario
) {
    /**
     * Calcula o subtotal deste item multiplicando preço unitário pela quantidade.
     */
    public BigDecimal calcularSubtotal() {
        return precoUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
