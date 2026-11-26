package com.aydsii.demo.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.aydsii.demo.model.tablas.TipoEntrega;
import com.aydsii.demo.model.tablas.TipoEstado;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pedido del historial del cliente con el detalle de los productos comprados")
public class PedidoDetalleDTO {

    @Schema(description = "Número del pedido", example = "1")
    private Long pedidoId;

    @Schema(description = "Fecha y hora en que se hizo el pedido")
    private LocalDateTime fecha;

    @Schema(description = "Estado actual del pedido", example = "PAGADO")
    private TipoEstado estado;

    @Schema(description = "Modo de entrega del pedido", example = "RETIRO_LOCAL")
    private TipoEntrega modoEntrega;

    @Schema(description = "Valor total del pedido en pesos", example = "9000")
    private Double valorTotal;

    @Schema(description = "Productos comprados en el pedido")
    private List<ItemPedidoDTO> items;

}