package com.aydsii.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aydsii.demo.model.ApiResponse;
import com.aydsii.demo.model.dto.AgregarCarritoRequestDTO;
import com.aydsii.demo.service.CarritoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carrito")
@Tag(name = "CU-03 Agregar producto al carrito")
public class CarritoController {

        private final CarritoService carritoService;

        public CarritoController(CarritoService carritoService) {
                this.carritoService = carritoService;
        }

        // -----------------------------------------------------------
        // ------AGREGAR UN PRODUCTO AL CARRITO DEL CLIENTE
        // -----------------------------------------------------------
        @Operation(summary = "Agregar un producto al carrito", description = "Agrega packs de un producto al carrito del cliente. Si el producto ya estaba en el carrito, suma la cantidad. Devuelve el total del carrito.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Producto agregado. Devuelve el total del carrito"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos: falta algún dato o la cantidad no es un entero mayor a 0"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No existe el cliente o el producto"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El producto no tiene stock, o la cantidad supera el stock disponible")
        })
        @PostMapping
        public ResponseEntity<ApiResponse<Double>> agregarProducto(
                        @Valid @RequestBody AgregarCarritoRequestDTO producto) {
                return ResponseEntity.ok(new ApiResponse<>(200, "Producto agregado al carrito",
                                carritoService.agregarProducto(producto)));
        }
}
