package br.com.devlopes.checkout.dto;

import br.com.devlopes.checkout.model.Pedido;
import br.com.devlopes.checkout.model.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta retornado pela Facade após a conclusão do checkout.
 */
public record PedidoResponse(
        Long pedidoId,
        String clienteNome,
        String clienteEmail,
        String modalidadeFrete,
        String formaPagamento,
        String cupomAplicado,
        BigDecimal valorItens,
        BigDecimal valorFrete,
        BigDecimal valorDesconto,
        BigDecimal valorTotal,
        StatusPedido status,
        LocalDateTime criadoEm,
        List<ItemPedidoResponse> itens
) {
    public static PedidoResponse fromEntity(Pedido pedido) {
        List<ItemPedidoResponse> itensResponse = pedido.getItens().stream()
                .map(ItemPedidoResponse::fromEntity)
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getClienteNome(),
                pedido.getClienteEmail(),
                pedido.getModalidadeFrete(),
                pedido.getFormaPagamento(),
                pedido.getCupomAplicado(),
                pedido.getValorItens(),
                pedido.getValorFrete(),
                pedido.getValorDesconto(),
                pedido.getValorTotal(),
                pedido.getStatus(),
                pedido.getCriadoEm(),
                itensResponse
        );
    }
}
