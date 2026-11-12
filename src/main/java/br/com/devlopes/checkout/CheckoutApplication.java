package br.com.devlopes.checkout;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal de inicialização da API de Checkout.
 */
@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "E-commerce Checkout API - Design Patterns (GoF) & SOLID",
                version = "1.0.0",
                description = "API REST de Checkout demonstrando os padrões Strategy, Chain of Responsibility, "
                        + "Facade e Singleton com Java 21 e Spring Boot 3.",
                contact = @Contact(name = "DevLopes", url = "https://github.com/devlopes")
        )
)
public class CheckoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutApplication.class, args);
    }
}
