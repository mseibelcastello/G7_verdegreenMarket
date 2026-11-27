package com.aydsii.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.aydsii.demo.util.FormatoFecha;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    // SLF4J (Simple Logging Facade): el código escribe contra la interfaz Logger
    // de org.slf4j y no sabe qué librería registra de verdad. En este proyecto
    // por debajo está Log4j2 (log4j2-spring.xml): consola y logs/demo.log.
    // Es static final: un solo logger para toda la clase.
    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

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

    // CU-04: CONSULTAR HISTORIAL DE PEDIDOS

    public List<PedidoDetalleDTO> consultarHistorialPedidos(String dni, TipoEstado estado,
            LocalDate desde, LocalDate hasta) {

        FormatoFecha formato = FormatoFecha.getInstance(); // Singleton

        // los {} se reemplazan por los argumentos en orden: el mensaje solo se
        // arma si ese nivel de log está activo
        log.info("CU-04 consultar historial: dni={}, estado={}, desde={}, hasta={}",
                dni, estado, formato.formatear(desde), formato.formatear(hasta));

        // primero las fechas, porque no necesitan consultar la base
        validarRangoFechas(desde, hasta);

        // el cliente tiene que existir
        if (!clienteRepository.existsById(dni)) {
            log.warn("CU-04 historial rechazado: no existe el cliente con dni={}", dni);
            throw new ClienteNoEncontradoException(dni);
        }

        LocalDateTime inicio = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime fin = (hasta != null) ? hasta.plusDays(1).atStartOfDay() : null;

        // vienen ordenados del más nuevo al más viejo
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

            log.debug("CU-04 pedido {}: {} producto(s)", pedido.getId(), items.size());

            resultado.add(new PedidoDetalleDTO(pedido.getId(), pedido.getFecha(), pedido.getEstado(),
                    pedido.getModoEntrega(), pedido.getValorTotal(), items));
        }

        log.info("CU-04 historial dni={}: {} pedido(s) encontrado(s)", dni, resultado.size());

        // si no hay pedidos se devuelve la lista vacía
        return resultado;
    }

    // si vienen las dos fechas, "desde" no puede ser posterior a "hasta"
    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            FormatoFecha formato = FormatoFecha.getInstance(); // Singleton
            String mensaje = "La fecha desde (" + formato.formatear(desde)
                    + ") no puede ser posterior a la fecha hasta (" + formato.formatear(hasta) + ")";
            log.warn("CU-04 historial rechazado: {}", mensaje);
            throw new RangoFechasInvalidoException(mensaje);
        }
    }

    // subtotal = cantidad de packs x precio unitario - descuento aplicado

    private double calcularSubtotal(DetallePedido detalle) {
        int descuento = (detalle.getDescuentoAplicado() != null) ? detalle.getDescuentoAplicado() : 0;
        return detalle.getCantidad() * detalle.getPrecioUnitario() - descuento;
    }
}
