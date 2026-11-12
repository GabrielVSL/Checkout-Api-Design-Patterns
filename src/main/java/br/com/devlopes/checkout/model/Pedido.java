package br.com.devlopes.checkout.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidade principal do domínio de Checkout, persistida no banco H2.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clienteNome;

    @Column(nullable = false)
    private String clienteEmail;

    @Column(nullable = false)
    private String modalidadeFrete;

    @Column(nullable = false)
    private String formaPagamento;

    private String cupomAplicado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorItens;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorFrete;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDesconto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(
            String clienteNome,
            String clienteEmail,
            String modalidadeFrete,
            String formaPagamento,
            String cupomAplicado,
            BigDecimal valorItens,
            BigDecimal valorFrete,
            BigDecimal valorDesconto,
            BigDecimal valorTotal,
            StatusPedido status
    ) {
        this.clienteNome = clienteNome;
        this.clienteEmail = clienteEmail;
        this.modalidadeFrete = modalidadeFrete;
        this.formaPagamento = formaPagamento;
        this.cupomAplicado = cupomAplicado;
        this.valorItens = valorItens;
        this.valorFrete = valorFrete;
        this.valorDesconto = valorDesconto;
        this.valorTotal = valorTotal;
        this.status = status;
    }

    @PrePersist
    void prePersist() {
        if (this.criadoEm == null) {
            this.criadoEm = LocalDateTime.now();
        }
    }

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        this.itens.add(item);
    }

    public Long getId() {
        return id;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public String getModalidadeFrete() {
        return modalidadeFrete;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public String getCupomAplicado() {
        return cupomAplicado;
    }

    public BigDecimal getValorItens() {
        return valorItens;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
