package br.com.devlopes.checkout.controller;

import br.com.devlopes.checkout.chain.ValidacaoCheckoutException;
import br.com.devlopes.checkout.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Interceptador global de exceções ({@code @RestControllerAdvice}) para padronizar
 * as respostas HTTP de erro da API e evitar vazamento de stack traces (erro 500 genérico)
 * em falhas de validação de negócio ou entrada de dados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata violações de regras de negócio disparadas pelos elos da Chain of Responsibility
     * ou pela resolução de modalidade de frete no Strategy.
     */
    @ExceptionHandler(ValidacaoCheckoutException.class)
    public ResponseEntity<ApiErrorResponse> handleValidacaoCheckout(
            ValidacaoCheckoutException ex,
            HttpServletRequest request
    ) {
        return construirRespostaErro(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Regra de negócio violada",
                ex.getMessage(),
                request.getRequestURI(),
                List.of(ex.getMessage())
        );
    }

    /**
     * Trata falhas de validação sintática de campos (Bean Validation / {@code @Valid}).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        return construirRespostaErro(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Um ou mais campos da requisição possuem valores inválidos.",
                request.getRequestURI(),
                detalhes
        );
    }

    /**
     * Trata payloads JSON malformados ou tipos incompatíveis no corpo da requisição.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return construirRespostaErro(
                HttpStatus.BAD_REQUEST,
                "JSON inválido",
                "O corpo da requisição está ausente ou possui formato JSON inválido.",
                request.getRequestURI(),
                List.of("Verifique a sintaxe do JSON e os tipos dos campos enviados.")
        );
    }

    /**
     * Trata parâmetros de rota ou query com tipo inválido (ex.: ID não numérico).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String mensagem = String.format("O parâmetro '%s' recebeu um valor inválido: '%s'.",
                ex.getName(), ex.getValue());
        return construirRespostaErro(
                HttpStatus.BAD_REQUEST,
                "Parâmetro inválido",
                mensagem,
                request.getRequestURI(),
                List.of(mensagem)
        );
    }

    /**
     * Trata consultas a recursos inexistentes (ex.: ID de pedido não encontrado).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoSuchElementException ex,
            HttpServletRequest request
    ) {
        return construirRespostaErro(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                ex.getMessage(),
                request.getRequestURI(),
                List.of(ex.getMessage())
        );
    }

    /**
     * Fallback de segurança para exceções inesperadas, evitando expor detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        return construirRespostaErro(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor",
                "Ocorreu um erro inesperado ao processar a requisição.",
                request.getRequestURI(),
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> construirRespostaErro(
            HttpStatus status,
            String erro,
            String mensagem,
            String path,
            List<String> detalhes
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                erro,
                mensagem,
                path,
                detalhes
        );
        return ResponseEntity.status(status).body(body);
    }
}
