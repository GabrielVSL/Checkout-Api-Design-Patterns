package br.com.devlopes.checkout.dto;

import br.com.devlopes.checkout.model.ItemPedido;

import java.math.BigDecimal;

/**
 * DTO de resposta para representação de um item salvo no pedido.
 */
public record ItemPedidoResponse(
        String produtoId,
        String nomeProduto,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static ItemPedidoResponse fromEntity(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getProdutoId(),
                item.getNomeProduto(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }
}
