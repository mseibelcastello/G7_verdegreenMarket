package com.aydsii.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aydsii.demo.model.tablas.ItemCarrito;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    // Busca si el cliente ya tiene ese producto en su carrito
    Optional<ItemCarrito> findByClienteDniAndProductoCodigo(String dni, Integer codigo);

    // devuelve el carrito completo del cliente
    List<ItemCarrito> findByClienteDni(String dni);
}
