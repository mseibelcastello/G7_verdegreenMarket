package com.aydsii.demo.model.tablas;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

// Una fila por cada producto que un cliente tiene en su carrito.
// La restricción única evita tener dos filas del mismo producto para el mismo
// cliente: si agrega de nuevo, se suma a la cantidad existente.
@Entity
@Table(name = "carrito", uniqueConstraints = @UniqueConstraint(columnNames = { "cliente_dni", "producto_codigo" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Producto que un cliente tiene en su carrito, con la cantidad de packs")
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador. Se genera automaticamente")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_dni")
    @NotNull(message = "El cliente es obligatorio.")
    @Schema(description = "Cliente dueño del carrito")
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_codigo")
    @NotNull(message = "El producto es obligatorio.")
    @Schema(description = "Producto agregado al carrito")
    private Producto producto;

    // Los productos se venden solo por pack completo
    @NotNull(message = "La cantidad es obligatoria.")
    @Positive(message = "La cantidad debe ser mayor a cero.")
    @Schema(description = "Cantidad de packs en el carrito", example = "3")
    private Integer cantidad;
}
