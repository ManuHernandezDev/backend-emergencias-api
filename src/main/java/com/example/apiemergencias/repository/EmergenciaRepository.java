package com.example.apiemergencias.repository;

import com.example.apiemergencias.model.EmergenciaConsolidada;
import com.example.apiemergencias.projection.ProporcionNacionalProjection;
import com.example.apiemergencias.projection.SaturacionEstadoProjection;
import com.example.apiemergencias.projection.TendenciaHistoricaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergenciaRepository extends JpaRepository<EmergenciaConsolidada, String> {

    // KPI 1: Gráfica de Barras (Volumen por Estado)
    @Query(value = "SELECT estado, SUM(total_emergencias) AS totalEmergencias " +
            "FROM emergencias_nacionales_consolidado " +
            "GROUP BY estado " +
            "ORDER BY totalEmergencias DESC", nativeQuery = true)
    List<SaturacionEstadoProjection> obtenerSaturacionPorEstado();

    // KPI 2: Gráfica de Líneas (Tendencia Temporal)
    @Query(value = "SELECT fecha, SUM(total_delitos) AS delitos, " +
            "SUM(total_accidentes) AS accidentes " +
            "FROM emergencias_nacionales_consolidado " +
            "GROUP BY fecha " +
            "ORDER BY fecha ASC", nativeQuery = true)
    List<TendenciaHistoricaProjection> obtenerTendenciaHistorica();

    // KPI 3: Gráfica de Pastel (Proporción Global)
    @Query(value = "SELECT SUM(total_delitos) AS totalDelitos, " +
            "SUM(total_accidentes) AS totalAccidentes " +
            "FROM emergencias_nacionales_consolidado", nativeQuery = true)
    ProporcionNacionalProjection obtenerProporcionNacional();
}