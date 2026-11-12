package br.com.devlopes.checkout.facade;

import br.com.devlopes.checkout.chain.ValidacaoCheckoutException;
import br.com.devlopes.checkout.chain.ValidacaoPedidoHandler;
import br.com.devlopes.checkout.dto.CheckoutRequest;
import br.com.devlopes.checkout.dto.ItemCheckoutRequest;
import br.com.devlopes.checkout.dto.PedidoResponse;
import br.com.devlopes.checkout.model.CupomDesconto;
import br.com.devlopes.checkout.model.ItemPedido;
import br.com.devlopes.checkout.model.Pedido;
import br.com.devlopes.checkout.model.StatusPedido;
import br.com.devlopes.checkout.repository.PedidoRepository;
import br.com.devlopes.checkout.strategy.FreteStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * <h2>Padrão Facade (GoF) + Singleton (Spring IoC)</h2>
 *
 * <p><b>Facade:</b> Fornece uma interface unificada e simplificada para o subsistema complexo
 * de checkout. O {@code CheckoutController} depende exclusivamente desta classe, permanecendo
 * desacoplado dos elos de validação, dos algoritmos de frete e dos detalhes de persistência.</p>
 *
 * <p><b>Orquestração realizada:</b></p>
 * <ol>
 *   <li>Execução da cadeia de validações (<b>Chain of Responsibility</b>).</li>
 *   <li>Resolução dinâmica do cálculo de frete via {@code Map<String, FreteStrategy>}
 *       (<b>Strategy</b>), sem uso de {@code if/else} ou {@code switch}.</li>
 *   <li>Cálculo de descontos, consolidação do pedido e persistência em banco H2.</li>
 *   <li>Conversão da entidade persistida para o DTO {@link PedidoResponse}.</li>
 * </ol>
 *
 * <p><b>Singleton:</b> A anotação {@code @Service} registra esta Facade como um bean
 * Singleton gerenciado pelo container de Injeção de Dependência do Spring.</p>
 */
@Service
public class CheckoutFacade {

    private final ValidacaoPedidoHandler cadeiaValidacao;
    private final Map<String, FreteStrategy> freteStrategies;
    private final PedidoRepository pedidoRepository;

    /**
     * O Spring injeta automaticamente:
     * <ul>
     *   <li>{@code validacoes}: todos os beans {@link ValidacaoPedidoHandler} ordenados por {@code @Order},
     *       compostos funcionalmente em uma única cadeia via {@link ValidacaoPedidoHandler#encadear}.</li>
     *   <li>{@code freteStrategies}: mapa cuja chave é o nome do bean ({@code "SEDEX"}, {@code "PAC"})
     *       e o valor é a respectiva instância Singleton de {@link FreteStrategy}.</li>
     * </ul>
     */
    public CheckoutFacade(
            List<ValidacaoPedidoHandler> validacoes,
            Map<String, FreteStrategy> freteStrategies,
            PedidoRepository pedidoRepository
    ) {
        if (validacoes == null || validacoes.isEmpty()) {
            throw new IllegalStateException("Nenhum handler de validação foi registrado no container Spring.");
        }
        this.cadeiaValidacao = validacoes.stream()
                .reduce(ValidacaoPedidoHandler::encadear)
                .orElseThrow();
        this.freteStrategies = freteStrategies;
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Orquestra todo o fluxo de fechamento de pedido (checkout).
     */
    @Transactional
    public PedidoResponse processarCheckout(CheckoutRequest request) {
        // 1. Executa o pipeline de validação (Chain of Responsibility)
        cadeiaValidacao.validar(request);

        // 2. Resolve a estratégia de frete dinamicamente sem if/else ou switch (Strategy)
        FreteStrategy freteStrategy = resolverEstrategiaFrete(request.modalidadeFrete());

        // 3. Realiza os cálculos financeiros do pedido
        BigDecimal valorItens = request.calcularValorTotalItens().setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorFrete = freteStrategy.calcularFrete(request.pesoTotalKg(), valorItens);
        Optional<CupomDesconto> cupomAplicado = CupomDesconto.buscarPorCodigo(request.cupomDesconto());
        BigDecimal valorDesconto = cupomAplicado
                .map(cupom -> cupom.calcularDesconto(valorItens))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        BigDecimal valorTotal = valorItens
                .subtract(valorDesconto)
                .add(valorFrete)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Constrói e persiste o pedido com seus itens
        Pedido pedido = construirPedido(
                request,
                freteStrategy.getModalidade(),
                cupomAplicado.map(Enum::name).orElse(null),
                valorItens,
                valorFrete,
                valorDesconto,
                valorTotal
        );

        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        return PedidoResponse.fromEntity(pedidoSalvo);
    }

    /**
     * Lista todos os pedidos persistidos.
     */
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidos() {
        return pedidoRepository.findAll().stream()
                .map(PedidoResponse::fromEntity)
                .toList();
    }

    /**
     * Busca um pedido específico pelo seu identificador.
     */
    @Transactional(readOnly = true)
    public PedidoResponse buscarPedidoPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado com ID: " + id));
        return PedidoResponse.fromEntity(pedido);
    }

    private FreteStrategy resolverEstrategiaFrete(String modalidadeFrete) {
        String chave = modalidadeFrete.trim().toUpperCase(Locale.ROOT);
        return Optional.ofNullable(freteStrategies.get(chave))
                .orElseThrow(() -> new ValidacaoCheckoutException(
                        "Modalidade de frete não suportada: '" + modalidadeFrete
                                + "'. Opções disponíveis: " + freteStrategies.keySet().stream().sorted().toList()
                ));
    }

    private Pedido construirPedido(
            CheckoutRequest request,
            String modalidadeFrete,
            String cupomAplicado,
            BigDecimal valorItens,
            BigDecimal valorFrete,
            BigDecimal valorDesconto,
            BigDecimal valorTotal
    ) {
        Pedido pedido = new Pedido(
                request.clienteNome().trim(),
                request.clienteEmail().trim(),
                modalidadeFrete,
                request.formaPagamento().trim().toUpperCase(Locale.ROOT),
                cupomAplicado,
                valorItens,
                valorFrete,
                valorDesconto,
                valorTotal,
                StatusPedido.CONFIRMADO
        );

        for (ItemCheckoutRequest itemRequest : request.itens()) {
            pedido.adicionarItem(new ItemPedido(
                    itemRequest.produtoId().trim(),
                    itemRequest.nomeProduto().trim(),
                    itemRequest.quantidade(),
                    itemRequest.precoUnitario()
            ));
        }

        return pedido;
    }
}
