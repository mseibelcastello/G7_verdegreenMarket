package com.aydsii.demo.exception;

//el producto no tiene stock disponible y no se puede agregar al carrito -> 409
public class ProductoSinStockException extends RuntimeException {
    public ProductoSinStockException() {
        super("No se puede agregar porque no hay stock.");
    }
}
