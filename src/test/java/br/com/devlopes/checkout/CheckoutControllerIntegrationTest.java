package br.com.devlopes.checkout;

import br.com.devlopes.checkout.dto.CheckoutRequest;
import br.com.devlopes.checkout.dto.ItemCheckoutRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/checkout deve criar pedido no H2 e permitir consulta por ID e listagem")
    void deveCriarPedidoViaEndpointEConsultarPorId() throws Exception {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-010", "Headset Wireless", 1, new BigDecimal("300.00"))),
                "PAC",
                "PIX",
                "BEMVINDO20",
                new BigDecimal("1.0")
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").isNumber())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cupomAplicado").value("BEMVINDO20"))
                .andExpect(jsonPath("$.valorItens").value(300.00))
                .andExpect(jsonPath("$.valorDesconto").value(60.00))
                .andExpect(jsonPath("$.valorFrete").value(15.00))
                .andExpect(jsonPath("$.valorTotal").value(255.00))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(postResult.getResponse().getContentAsString());
        long pedidoId = jsonNode.get("pedidoId").asLong();

        mockMvc.perform(get("/api/v1/checkout/pedidos/{id}", pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(pedidoId))
                .andExpect(jsonPath("$.clienteNome").value("Fred Lopes"));

        mockMvc.perform(get("/api/v1/checkout/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /api/v1/checkout deve retornar 422 quando falhar validação de estoque na Chain")
    void deveRetornar422ParaProdutoSemEstoque() throws Exception {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-SEM-ESTOQUE", "Cadeira Ergonômica", 1, new BigDecimal("1200.00"))),
                "SEDEX",
                "PIX",
                null,
                new BigDecimal("12.0")
        );

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.path").value("/api/v1/checkout"))
                .andExpect(jsonPath("$.mensagem", containsString("Estoque insuficiente")));
    }

    @Test
    @DisplayName("POST /api/v1/checkout deve retornar 422 quando modalidade de frete não existir no Strategy")
    void deveRetornar422ParaModalidadeFreteInexistente() throws Exception {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-010", "Headset Wireless", 1, new BigDecimal("300.00"))),
                "TRANSPORTADORA_X",
                "PIX",
                null,
                new BigDecimal("1.0")
        );

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.mensagem", containsString("Modalidade de frete não suportada")));
    }

    @Test
    @DisplayName("POST /api/v1/checkout deve retornar 422 para cupom inválido na Chain")
    void deveRetornar422ParaCupomInvalido() throws Exception {
        CheckoutRequest request = new CheckoutRequest(
                "Fred Lopes",
                "fred@devlopes.com.br",
                List.of(new ItemCheckoutRequest("PROD-010", "Headset Wireless", 1, new BigDecimal("300.00"))),
                "SEDEX",
                "PIX",
                "CUPOM-INEXISTENTE",
                new BigDecimal("1.0")
        );

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.mensagem").value("Cupom de desconto inválido ou expirado: 'CUPOM-INEXISTENTE'."));
    }

    @Test
    @DisplayName("POST /api/v1/checkout deve retornar 400 Bad Request quando violar Bean Validation")
    void deveRetornar400QuandoPayloadInvalido() throws Exception {
        CheckoutRequest requestInvalido = new CheckoutRequest(
                "",
                "email-invalido",
                List.of(),
                "",
                "",
                null,
                new BigDecimal("0.0")
        );

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                .andExpect(jsonPath("$.path").value("/api/v1/checkout"))
                .andExpect(jsonPath("$.detalhes", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @DisplayName("GET /api/v1/checkout/pedidos/{id} deve retornar 404 para ID inexistente")
    void deveRetornar404ParaPedidoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/checkout/pedidos/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.mensagem").value("Pedido não encontrado com ID: 99999"));
    }
}
