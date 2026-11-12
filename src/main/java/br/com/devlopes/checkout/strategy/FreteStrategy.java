package br.com.devlopes.checkout.strategy;

import java.math.BigDecimal;

/**
 * <h2>Padrão Strategy (GoF)</h2>
 * Define o contrato comum para diferentes algoritmos de cálculo de frete.
 *
 * <p><b>Por que aplicar Strategy aqui?</b></p>
 * <ul>
 *   <li>Elimina estruturas condicionais rígidas ({@code if/else} ou {@code switch})
 *       para seleção de transportadora/modalidade.</li>
 *   <li>Respeita o princípio <b>OCP (Open/Closed Principle)</b> do SOLID: novas
 *       modalidades (ex.: {@code FreteMotoboy}, {@code FreteTransportadora}) podem ser
 *       adicionadas criando novas implementações sem alterar a {@code CheckoutFacade}.</li>
 *   <li>O Spring resolve a estratégia dinamicamente via injeção de {@code Map<String, FreteStrategy>},
 *       usando o nome do bean como chave.</li>
 * </ul>
 */
public interface FreteStrategy {

    /**
     * Calcula o valor do frete com base no peso total e no valor dos itens do pedido.
     *
     * @param pesoTotalKg peso total da remessa em quilogramas
     * @param valorItens  soma dos subtotais dos itens
     * @return valor calculado do frete com escala de 2 casas decimais
     */
    BigDecimal calcularFrete(BigDecimal pesoTotalKg, BigDecimal valorItens);

    /**
     * Retorna o identificador da modalidade de frete atendida por esta estratégia.
     */
    String getModalidade();
}
