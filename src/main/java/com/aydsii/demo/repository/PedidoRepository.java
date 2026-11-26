package com.aydsii.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aydsii.demo.model.tablas.Pedido;
import com.aydsii.demo.model.tablas.TipoEstado;

import java.util.List;
import java.time.LocalDateTime;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // query que devuelve los pedidos del cliente con el dni ingresado del más nuevo
    // al más viejo
    List<Pedido> findByClienteDniOrderByFechaDesc(String dni);

    // CU-04: pedidos del cliente, del más nuevo al más viejo (RN-25).
    // Los filtros son opcionales (FA-01): si un parámetro llega null, esa
    // condición no filtra nada.
    @Query("""
            SELECT p FROM Pedido p
            WHERE p.cliente.dni = :dni
              AND (:estado IS NULL OR p.estado = :estado)
              AND (:desde IS NULL OR p.fecha >= :desde)
              AND (:hasta IS NULL OR p.fecha < :hasta)
            ORDER BY p.fecha DESC
            """)
    List<Pedido> buscarHistorial(@Param("dni") String dni,
            @Param("estado") TipoEstado estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

}

// Es JPQL, no SQL: escribo nombres de clases y atributos de Java (Pedido,
// p.cliente.dni),
// no de tablas. Hibernate la traduce a SQL.