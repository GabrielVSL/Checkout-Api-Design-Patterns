package br.com.devlopes.checkout.repository;

import br.com.devlopes.checkout.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta de {@link Pedido}.
 *
 * <p><b>Singleton (Spring IoC):</b> O proxy gerado em tempo de execução pelo Spring Data
 * para esta interface é registrado no container como Singleton.</p>
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
