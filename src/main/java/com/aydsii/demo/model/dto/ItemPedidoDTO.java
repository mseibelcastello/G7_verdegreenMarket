package com.aydsii.demo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Producto comprado dentro de un pedido")
public class ItemPedidoDTO {

    @Schema(description = "Código del producto", example = "1")
    private Integer codigoProducto;

    @Schema(description = "Nombre del producto", example = "Vaso PET 250cc")
    private String nombreProducto;

    @Schema(description = "Cantidad de packs comprados", example = "2")
    private Integer cantidad;

    @Schema(description = "Precio unitario del pack al momento del pedido (RN-21)", example = "4500")
    private Double precioUnitario;

    @Schema(description = "Descuento aplicado en pesos", example = "0")
    private Integer descuentoAplicado;

    @Schema(description = "Subtotal de la línea: cantidad × precio unitario − descuento (RN-26)", example = "9000")
    private Double subtotal;

}