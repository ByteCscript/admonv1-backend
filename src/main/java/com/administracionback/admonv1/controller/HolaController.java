package com.administracionback.admonv1.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HolaController {

    private final JdbcTemplate jdbcTemplate;

    public HolaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/hola")
    public String hola() {
        return "Hola, acá está tu primer endpoint con Spring";
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