package com.administracionback.admonv1.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class HolaController {

    private final JdbcTemplate jdbcTemplate;

    public HolaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/hola")
    public String hola() {
        return "Hola, acá está tu primer endpoint con Spring ejemplo desde johan";
    }

    @GetMapping("/db")
    public Map<String, Object> probarConexionDb() {

        Integer resultado = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        return Map.of(
                "conexion", "OK",
                "baseDatos", "PostgreSQL",
                "resultado", resultado
        );
    }
}