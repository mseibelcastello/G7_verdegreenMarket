package com.aydsii.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aydsii.demo.exception.ClienteNoEncontradoException;
import com.aydsii.demo.exception.ProductoNoEncontradoException;
import com.aydsii.demo.exception.ProductoSinStockException;
import com.aydsii.demo.exception.StockInsuficienteException;
import com.aydsii.demo.model.dto.AgregarCarritoRequestDTO;
import com.aydsii.demo.model.tablas.Cliente;
import com.aydsii.demo.model.tablas.ItemCarrito;
import com.aydsii.demo.model.tablas.Producto;
import com.aydsii.demo.repository.ClienteRepository;
import com.aydsii.demo.repository.ItemCarritoRepository;
import com.aydsii.demo.repository.ProductoRepository;

@Service
public class CarritoService {

    // Simple Logging Facade: el código usa solo las interfaces de SLF4J.
    // Quien escribe los logs es Log4j2, configurado en log4j2-spring.xml.
    private static final Logger log = LoggerFactory.getLogger(CarritoService.class);

    private final ItemCarritoRepository carritoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    public CarritoService(ItemCarritoRepository carritoRepository, ClienteRepository clienteRepository,
            ProductoRepository productoRepository) {
        this.carritoRepository = carritoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
    }

    public double agregarProducto(AgregarCarritoRequestDTO producto) {

        log.info("Agregar al carrito: cliente={}, producto={}, cantidad={}",
                producto.clienteDni(), producto.productoCodigo(), producto.cantidad());

        Cliente cliente = clienteRepository.findById(producto.clienteDni())
                .orElseThrow(() -> {
                    log.warn("Cliente inexistente: dni={}", producto.clienteDni());
                    return new ClienteNoEncontradoException(producto.clienteDni());
                });

        Producto productoCatalogo = productoRepository.findById(producto.productoCodigo())
                .orElseThrow(() -> {
                    log.warn("Producto inexistente: codigo={}", producto.productoCodigo());
                    return new ProductoNoEncontradoException(
                            "No se encontró el producto con código " + producto.productoCodigo() + ".");
                });

        int disponible = productoCatalogo.getStockDisponible();

        if (disponible <= 0) {
            log.warn("Producto sin stock: codigo={}", producto.productoCodigo());
            throw new ProductoSinStockException();
        }

        // si el producto ya está en el carrito, se suma a lo que había
        ItemCarrito item = carritoRepository
                .findByClienteDniAndProductoCodigo(producto.clienteDni(), producto.productoCodigo())
                .orElse(null);
        int enCarrito = 0;
        if (item != null) {
            enCarrito = item.getCantidad();
        }

        log.debug("Stock disponible={}, ya en el carrito={}", disponible, enCarrito);

        if (enCarrito + producto.cantidad() > disponible) {
            log.warn("Stock insuficiente: producto={}, disponible={}, enCarrito={}, pedida={}",
                    producto.productoCodigo(), disponible, enCarrito, producto.cantidad());
            throw new StockInsuficienteException(disponible, enCarrito, producto.cantidad());
        }

        if (item == null) {
            item = new ItemCarrito();
            item.setCliente(cliente);
            item.setProducto(productoCatalogo);
        }

        item.setCantidad(enCarrito + producto.cantidad());
        carritoRepository.save(item);

        double total = calcularTotal(producto.clienteDni());
        log.info("Producto agregado al carrito: cliente={}, producto={}, total={}",
                producto.clienteDni(), producto.productoCodigo(), total);

        return total;
    }

    // suma los subtotales (precio del pack x cantidad) de todo el carrito del cliente
    private double calcularTotal(String dni) {
        double total = 0;
        for (ItemCarrito i : carritoRepository.findByClienteDni(dni)) {
            total += i.getProducto().getPrecioPack() * i.getCantidad();
        }
        return total;
    }
}
