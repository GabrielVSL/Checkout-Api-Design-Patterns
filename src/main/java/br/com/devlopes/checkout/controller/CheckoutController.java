package br.com.devlopes.checkout.controller;

import br.com.devlopes.checkout.dto.ApiErrorResponse;
import br.com.devlopes.checkout.dto.CheckoutRequest;
import br.com.devlopes.checkout.dto.PedidoResponse;
import br.com.devlopes.checkout.facade.CheckoutFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST responsável por expor os endpoints de Checkout.
 *
 * <p>Seguindo o padrão <b>Facade</b> e o Princípio da Responsabilidade Única (SRP),
 * este controller depende exclusivamente de {@link CheckoutFacade}, delegando toda a
 * orquestração de regras de negócio, validações, cálculo de frete e persistência.</p>
 */
@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout", description = "Endpoints para fechamento e consulta de pedidos de e-commerce")
public class CheckoutController {

    private final CheckoutFacade checkoutFacade;

    public CheckoutController(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    @PostMapping
    @Operation(
            summary = "Realizar checkout de pedido",
            description = "Executa a cadeia de validações (estoque, pagamento e cupom), calcula o frete "
                    + "dinamicamente via Strategy (SEDEX ou PAC) e persiste o pedido no banco H2."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido confirmado e persistido com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Falha em regra de negócio na cadeia de validação ou modalidade de frete",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PedidoResponse> finalizarCheckout(@Valid @RequestBody CheckoutRequest request) {
        PedidoResponse response = checkoutFacade.processarCheckout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pedidos")
    @Operation(
            summary = "Listar pedidos realizados",
            description = "Retorna todos os pedidos processados e armazenados em memória no banco H2."
    )
    @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada com sucesso")
    public ResponseEntity<List<PedidoResponse>> listarPedidos() {
        return ResponseEntity.ok(checkoutFacade.listarPedidos());
    }

    @GetMapping("/pedidos/{id}")
    @Operation(
            summary = "Buscar pedido por ID",
            description = "Consulta os detalhes de um pedido específico pelo seu identificador único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(checkoutFacade.buscarPedidoPorId(id));
    }
}
