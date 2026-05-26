package com.example.apiemergencias.controller;

import com.example.apiemergencias.projection.ProporcionNacionalProjection;
import com.example.apiemergencias.projection.SaturacionEstadoProjection;
import com.example.apiemergencias.projection.TendenciaHistoricaProjection;
import com.example.apiemergencias.repository.EmergenciaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kpi")
@CrossOrigin(origins = "*") // Fundamental para que Flutter no tenga bloqueos de red
public class EmergenciaController {

    private final EmergenciaRepository emergenciaRepository;

    public EmergenciaController(EmergenciaRepository emergenciaRepository) {
        this.emergenciaRepository = emergenciaRepository;
    }

    @GetMapping("/saturacion")
    public ResponseEntity<Map<String, Object>> getSaturacion() {
        List<SaturacionEstadoProjection> data = emergenciaRepository.obtenerSaturacionPorEstado();
        return construirRespuestaEstandar(data);
    }

    @GetMapping("/tendencia")
    public ResponseEntity<Map<String, Object>> getTendencia() {
        List<TendenciaHistoricaProjection> data = emergenciaRepository.obtenerTendenciaHistorica();
        return construirRespuestaEstandar(data);
    }

    @GetMapping("/proporcion")
    public ResponseEntity<Map<String, Object>> getProporcion() {
        ProporcionNacionalProjection data = emergenciaRepository.obtenerProporcionNacional();

        // Formateamos la respuesta de la proporción para que encaje perfecto con el JSON que le prometiste a Frontend
        List<Map<String, Object>> dataFormateada = List.of(
                Map.of("tipo", "Accidentes Viales", "porcentaje", data.getTotalAccidentes()),
                Map.of("tipo", "Delitos Operativos", "porcentaje", data.getTotalDelitos())
        );
        return construirRespuestaEstandar(dataFormateada);
    }

    // Método auxiliar (Clean Code) para no repetir la estructura de éxito en cada endpoint
    private ResponseEntity<Map<String, Object>> construirRespuestaEstandar(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}