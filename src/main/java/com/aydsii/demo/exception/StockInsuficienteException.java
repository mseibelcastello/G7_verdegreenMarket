package com.aydsii.demo.exception;

//la cantidad pedida (sumada a la que ya hay en el carrito) supera el stock disponible -> 409
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(int disponible, int enCarrito, int pedida) {
        super("No hay stock suficiente. Disponible: " + disponible
                + " packs; ya tenés " + enCarrito + " en el carrito y pediste " + pedida + " más.");
    }
}
