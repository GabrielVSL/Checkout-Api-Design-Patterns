package br.com.devlopes.checkout;

import br.com.devlopes.checkout.chain.ValidacaoCheckoutException;
import br.com.devlopes.checkout.chain.ValidacaoCupomHandler;
import br.com.devlopes.checkout.chain.ValidacaoEstoqueHandler;
import br.com.devlopes.checkout.chain.ValidacaoPagamentoHandler;
import br.com.devlopes.checkout.chain.ValidacaoPedidoHandler;
import br.com.devlopes.checkout.dto.CheckoutRequest;
import br.com.devlopes.checkout.dto.ItemCheckoutRequest;
import br.com.devlopes.checkout.dto.PedidoResponse;
import br.com.devlopes.checkout.facade.CheckoutFacade;
import br.com.devlopes.checkout.model.Pedido;
import br.com.devlopes.checkout.model.StatusPedido;
import br.com.devlopes.checkout.repository.PedidoRepository;
import br.com.devlopes.checkout.strategy.FretePac;
import br.com.devlopes.checkout.strategy.FreteSedex;
import br.com.devlopes.checkout.strategy.FreteStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutFacadeTest {

    @Mock
    private PedidoRepository pedidoRepository;

    private CheckoutFacade checkoutFacade;

    @BeforeEach
    void setUp() {
        List<ValidacaoPedidoHandler> handlers = List.of(
                new ValidacaoEstoqueHandler(),
                new ValidacaoPagamentoHandler(),
                new ValidacaoCupomHandler()
        );
        Map<String, FreteStrategy> strategies = Map.of(
                "SEDEX", new FreteSedex(),
                "PAC", new FretePac()
        );
        checkoutFacade = new CheckoutFacade(handlers, strategies, pedidoRepository);
    }

    @Test
    @DisplayName("Deve processar checkout com SEDEX e cupom DEV10 validando cálculo e persistência")
    void deveProcessarCheckoutSedexComCupom() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-001", "Teclado Mecânico", 2, new BigDecimal("200.00"))),
                "sedex",
                "pix",
                "dev10",
                new BigDecimal("2.0")
        );

        PedidoResponse response = checkoutFacade.processarCheckout(request);

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(pedidoCaptor.capture());
        Pedido pedidoPersistido = pedidoCaptor.getValue();

        assertThat(pedidoPersistido.getItens()).hasSize(1);
        assertThat(pedidoPersistido.getItens().getFirst().getSubtotal()).isEqualByComparingTo("400.00");

        assertThat(response.status()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(response.modalidadeFrete()).isEqualTo("SEDEX");
        assertThat(response.formaPagamento()).isEqualTo("PIX");
        assertThat(response.cupomAplicado()).isEqualTo("DEV10");
        assertThat(response.valorItens()).isEqualByComparingTo("400.00");
        assertThat(response.valorDesconto()).isEqualByComparingTo("40.00");
        assertThat(response.valorFrete()).isEqualByComparingTo("33.90");
        assertThat(response.valorTotal()).isEqualByComparingTo("393.90");
    }

    @Test
    @DisplayName("Deve calcular frete PAC normal quando valor dos itens for inferior a R$ 500,00")
    void deveCalcularFretePacNormalAbaixoDeQuinhentos() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(
                "Carlos Mendes",
                "carlos@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-005", "Mousepad XL", 2, new BigDecimal("100.00"))),
                "PAC",
                "CARTAO_CREDITO",
                null,
                new BigDecimal("2.0")
        );

        PedidoResponse response = checkoutFacade.processarCheckout(request);

        assertThat(response.modalidadeFrete()).isEqualTo("PAC");
        assertThat(response.valorItens()).isEqualByComparingTo("200.00");
        assertThat(response.valorFrete()).isEqualByComparingTo("17.10");
        assertThat(response.valorTotal()).isEqualByComparingTo("217.10");
    }

    @Test
    @DisplayName("Deve conceder frete grátis via PAC para pedidos com itens a partir de R$ 500,00")
    void deveConcederFreteGratisPacParaPedidosAcimaDeQuinhentos() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(
                "Ana Silva",
                "ana@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-002", "Monitor 4K", 1, new BigDecimal("500.00"))),
                "pac",
                "CARTAO_CREDITO",
                null,
                new BigDecimal("4.0")
        );

        PedidoResponse response = checkoutFacade.processarCheckout(request);

        assertThat(response.modalidadeFrete()).isEqualTo("PAC");
        assertThat(response.valorFrete()).isEqualByComparingTo("0.00");
        assertThat(response.valorTotal()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Deve interromper a Chain no Elo 1 quando produto estiver sem estoque")
    void deveFalharQuandoProdutoSemEstoque() {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-SEM-ESTOQUE", "Mouse Gamer", 1, new BigDecimal("150.00"))),
                "SEDEX",
                "PIX",
                null,
                new BigDecimal("0.5")
        );

        assertThatThrownBy(() -> checkoutFacade.processarCheckout(request))
                .isInstanceOf(ValidacaoCheckoutException.class)
                .hasMessage("Estoque insuficiente para o produto 'Mouse Gamer' (PROD-SEM-ESTOQUE). Solicitado: 1, disponível: 0.");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve interromper a Chain no Elo 1 quando quantidade superar estoque limitado")
    void deveFalharQuandoQuantidadeExcederEstoqueLimitado() {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-LIMITADO", "Placa de Vídeo", 4, new BigDecimal("2500.00"))),
                "SEDEX",
                "PIX",
                null,
                new BigDecimal("1.5")
        );

        assertThatThrownBy(() -> checkoutFacade.processarCheckout(request))
                .isInstanceOf(ValidacaoCheckoutException.class)
                .hasMessageContaining("Solicitado: 4, disponível: 3.");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve interromper a Chain no Elo 2 quando forma de pagamento não for aceita")
    void deveFalharQuandoFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-001", "Teclado Mecânico", 1, new BigDecimal("200.00"))),
                "SEDEX",
                "CRIPTO",
                null,
                new BigDecimal("1.0")
        );

        assertThatThrownBy(() -> checkoutFacade.processarCheckout(request))
                .isInstanceOf(ValidacaoCheckoutException.class)
                .hasMessage("Forma de pagamento inválida: 'CRIPTO'. Aceitas: BOLETO, CARTAO_CREDITO, PIX.");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve interromper a Chain no Elo 2 quando pagamento via BOLETO exceder R$ 5.000,00")
    void deveFalharQuandoBoletoExcederLimite() {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-003", "Notebook Pro", 1, new BigDecimal("5000.01"))),
                "SEDEX",
                "BOLETO",
                null,
                new BigDecimal("2.5")
        );

        assertThatThrownBy(() -> checkoutFacade.processarCheckout(request))
                .isInstanceOf(ValidacaoCheckoutException.class)
                .hasMessage("Pagamentos via BOLETO são permitidos apenas para pedidos de até R$ 5.000,00.");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve interromper a Chain no Elo 3 quando cupom não atingir valor mínimo")
    void deveFalharQuandoCupomNaoAtingirValorMinimo() {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-009", "Cabo USB-C", 1, new BigDecimal("59.90"))),
                "PAC",
                "PIX",
                "BEMVINDO20",
                new BigDecimal("0.2")
        );

        assertThatThrownBy(() -> checkoutFacade.processarCheckout(request))
                .isInstanceOf(ValidacaoCheckoutException.class)
                .hasMessage("O cupom 'BEMVINDO20' exige valor mínimo em itens de R$ 100,00.");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar quando modalidade de frete não estiver registrada no Strategy Map")
    void deveFalharQuandoModalidadeFreteInvalida() {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-001", "Teclado Mecânico", 1, new BigDecimal("200.00"))),
                "DRONE",
                "PIX",
                null,
                new BigDecimal("1.0")
        );

        assertThatThrownBy(() -> checkoutFacade.processarCheckout(request))
                .isInstanceOf(ValidacaoCheckoutException.class)
                .hasMessage("Modalidade de frete não suportada: 'DRONE'. Opções disponíveis: [PAC, SEDEX]");

        verify(pedidoRepository, never()).save(any());
    }
}
