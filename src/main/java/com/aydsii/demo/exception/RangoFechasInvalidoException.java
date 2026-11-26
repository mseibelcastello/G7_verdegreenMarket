package com.aydsii.demo.exception;

// CU-04 (FE-02): la fecha "desde" del filtro es posterior a la fecha "hasta" -> 400
public class RangoFechasInvalidoException extends RuntimeException {
    public RangoFechasInvalidoException(String mensaje) {
        super(mensaje);
    }
}
