package com.aydsii.demo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// Datos que envía el cliente al agregar un producto al carrito.
public record AgregarCarritoRequestDTO(
                @NotBlank(message = "El DNI del cliente es obligatorio.") @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos.") @Schema(description = "DNI del cliente dueño del carrito", example = "30111222") String clienteDni,
                @NotNull(message = "El código del producto es obligatorio.") @Positive(message = "El código del producto debe ser mayor a 0.") @Schema(description = "Código del producto a agregar", example = "1") Integer productoCodigo,
                @NotNull(message = "La cantidad es obligatoria.") @Positive(message = "La cantidad debe ser un número entero mayor a 0.") @Schema(description = "Cantidad de packs (no de unidades sueltas)", example = "3") Integer cantidad) {
}
