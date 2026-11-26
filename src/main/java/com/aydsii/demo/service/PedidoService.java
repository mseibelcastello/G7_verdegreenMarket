package com.aydsii.demo.service;

import org.springframework.stereotype.Service;

import com.aydsii.demo.model.dto.CambioEstadoDTO;
import com.aydsii.demo.model.dto.HistorialPedidoDTO;
import com.aydsii.demo.model.dto.ItemPedidoDTO;
import com.aydsii.demo.model.dto.PedidoDetalleDTO;
import com.aydsii.demo.model.dto.PedidoResumenDTO;
import com.aydsii.demo.model.tablas.DetallePedido;
import com.aydsii.demo.model.tablas.HistorialEstado;
import com.aydsii.demo.model.tablas.Pedido;
import com.aydsii.demo.model.tablas.TipoEstado;
import com.aydsii.demo.repository.ClienteRepository;
import com.aydsii.demo.repository.DetallePedidoRepository;
import com.aydsii.demo.repository.HistorialEstadoRepository;
import com.aydsii.demo.repository.PedidoRepository;
import com.aydsii.demo.exception.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final HistorialEstadoRepository historialRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            HistorialEstadoRepository historialRepository, DetallePedidoRepository detallePedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.historialRepository = historialRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    public List<PedidoResumenDTO> listarPedidos(String dni) {
        if (!clienteRepository.existsById(dni)) {
            throw new ClienteNoEncontradoException(dni);
        }

        List<Pedido> pedidos = pedidoRepository.findByClienteDniOrderByFechaDesc(dni);
        List<PedidoResumenDTO> resultado = new ArrayList<>();

        for (Pedido p : pedidos) {
            resultado.add(new PedidoResumenDTO(p.getId(), p.getFecha(), p.getEstado(),
                    p.getModoEntrega(), p.getValorTotal()));
        }

        return resultado;
    }

    public HistorialPedidoDTO consultarHistorial(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));

        List<HistorialEstado> cambios = historialRepository.findByPedidoIdOrderByFechaHoraAsc(id);
        List<CambioEstadoDTO> historial = new ArrayList<>();

        for (HistorialEstado h : cambios) {
            historial.add(new CambioEstadoDTO(h.getEstado(), h.getFechaHora()));
        }

        return new HistorialPedidoDTO(pedido.getId(), pedido.getEstado(),
                pedido.getMotivoCancelacion(), historial);
    }

    // -----------------------------------------------------------
    // ------ CU-04: CONSULTAR HISTORIAL DE PEDIDOS
    // -----------------------------------------------------------
    // Sigue el diagrama de secuencia: valida, busca los pedidos y, por cada
    // pedido, arma sus ítems (loop externo) calculando el subtotal de cada
    // línea (loop interno). estado, desde y hasta son filtros opcionales.
    public List<PedidoDetalleDTO> consultarHistorialPedidos(String dni, TipoEstado estado,
            LocalDate desde, LocalDate hasta) {

        // FE-02: primero las fechas, porque no necesitan consultar la base
        validarRangoFechas(desde, hasta);

        // RN-22 / FE-01: el cliente tiene que existir
        if (!clienteRepository.existsById(dni)) {
            throw new ClienteNoEncontradoException(dni);
        }

        // el repositorio trabaja con fecha y hora: "desde" arranca a las 00:00 y
        // "hasta" se pasa al día siguiente a las 00:00 (la query usa <), así
        // entra todo el día "hasta"
        LocalDateTime inicio = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime fin = (hasta != null) ? hasta.plusDays(1).atStartOfDay() : null;

        // RN-25: vienen ordenados del más nuevo al más viejo
        List<Pedido> pedidos = pedidoRepository.buscarHistorial(dni, estado, inicio, fin);
        List<PedidoDetalleDTO> resultado = new ArrayList<>();

        // loop [para cada pedido en pedidos]
        for (Pedido pedido : pedidos) {
            List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(pedido.getId());
            List<ItemPedidoDTO> items = new ArrayList<>();

            // loop [para cada detalle en detalles]
            for (DetallePedido detalle : detalles) {
                double subtotal = calcularSubtotal(detalle);
                items.add(new ItemPedidoDTO(
                        detalle.getProducto().getCodigo(),
                        detalle.getProducto().getNombre(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getDescuentoAplicado(),
                        subtotal));
            }

            resultado.add(new PedidoDetalleDTO(pedido.getId(), pedido.getFecha(), pedido.getEstado(),
                    pedido.getModoEntrega(), pedido.getValorTotal(), items));
        }

        // FA-02 / FA-03: si no hay pedidos se devuelve la lista vacía
        return resultado;
    }

    // FE-02: si vienen las dos fechas, "desde" no puede ser posterior a "hasta"
    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new RangoFechasInvalidoException(
                    "La fecha desde (" + desde + ") no puede ser posterior a la fecha hasta (" + hasta + ")");
        }
    }

    // RN-26: subtotal = cantidad de packs x precio unitario - descuento aplicado
    // (el precio es el guardado en el pedido, no el actual del producto: RN-21)
    private double calcularSubtotal(DetallePedido detalle) {
        int descuento = (detalle.getDescuentoAplicado() != null) ? detalle.getDescuentoAplicado() : 0;
        return detalle.getCantidad() * detalle.getPrecioUnitario() - descuento;
    }
}
