
package com.aydsii.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aydsii.demo.model.tablas.DetallePedido;

import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    // devuelve las líneas de detalle (productos) de un pedido
    List<DetallePedido> findByPedidoId(Long pedidoId);
}
