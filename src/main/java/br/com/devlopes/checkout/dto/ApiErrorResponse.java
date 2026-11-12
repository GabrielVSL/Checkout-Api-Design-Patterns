package br.com.devlopes.checkout.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO padronizado para respostas de erro HTTP da API REST.
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        List<String> detalhes
) {
}
