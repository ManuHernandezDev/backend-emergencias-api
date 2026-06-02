package com.example.apiemergencias.repository;

import com.example.apiemergencias.model.EmergenciaConsolidada;
import com.example.apiemergencias.projection.ProporcionNacionalProjection;
import com.example.apiemergencias.projection.SaturacionEstadoProjection;
import com.example.apiemergencias.projection.TendenciaHistoricaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    // KPI 4: Comparativa Interanual (Año vs Año Anterior)
    @Query(value = "SELECT " +
            "COALESCE(SUM(CASE WHEN EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio THEN total_emergencias ELSE 0 END), 0) AS total_actual, " +
            "COALESCE(SUM(CASE WHEN EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio - 1 THEN total_emergencias ELSE 0 END), 0) AS total_anterior " +
            "FROM emergencias_nacionales_consolidado " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND EXTRACT(YEAR FROM CAST(fecha AS date)) IN (:anio, :anio - 1)", nativeQuery = true)
    Map<String, BigDecimal> obtenerComparativaInteranual(@Param("anio") Integer anio, @Param("estado") String estado);

    // KPI 5: Días Críticos (Mapa de Calor)
    @Query(value = "SELECT " +
            "EXTRACT(ISODOW FROM CAST(fecha AS date)) AS dia_semana, " +
            "SUM(total_emergencias) AS total_emergencias " +
            "FROM emergencias_nacionales_consolidado " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:anio IS NULL OR EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio) " +
            "GROUP BY EXTRACT(ISODOW FROM CAST(fecha AS date)) " +
            "ORDER BY dia_semana", nativeQuery = true)
    List<Map<String, Object>> obtenerDiasCriticos(@Param("estado") String estado, @Param("anio") Integer anio);
}