package com.aydsii.demo.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// PATRÓN SINGLETON (CU-04)
// Centraliza el formato de fecha DD-MM-AAAA que usa el historial de pedidos.
// Garantiza que exista UNA sola instancia del formateador en toda la
// aplicación y da un único punto de acceso a ella: getInstance().

// Se usa inicialización temprana: la instancia se crea cuando se
// carga la clase. Es la forma más simple y además es segura con varios
// hilos (thread-safe) sin necesidad de synchronized.

public final class FormatoFecha {

    // 1. guarda la única instancia
    private static final FormatoFecha INSTANCIA = new FormatoFecha();

    public static final String PATRON = "dd-MM-yyyy";

    private final DateTimeFormatter formateador;

    // 2. constructor privado, nadie puede hacer "new FormatoFecha()"
    private FormatoFecha() {
        this.formateador = DateTimeFormatter.ofPattern(PATRON);
    }

    // 3. devuelve siempre la misma instancia
    public static FormatoFecha getInstance() {
        return INSTANCIA;
    }

    // devuelve la fecha como DD-MM-AAAA, o "-" si no viene (filtro opcional)
    public String formatear(LocalDate fecha) {
        return (fecha != null) ? fecha.format(formateador) : "-";
    }
}
