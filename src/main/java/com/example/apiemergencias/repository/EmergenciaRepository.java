package com.example.apiemergencias.repository;

import com.example.apiemergencias.model.EmergenciaConsolidada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public interface EmergenciaRepository extends JpaRepository<EmergenciaConsolidada, String> {

    // KPI 4: Comparativa Interanual
    @Query(value = "SELECT " +
            "COALESCE(SUM(CASE WHEN EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio THEN total_emergencias ELSE 0 END), 0) AS total_actual, " +
            "COALESCE(SUM(CASE WHEN EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio - 1 THEN total_emergencias ELSE 0 END), 0) AS total_anterior " +
            "FROM emergencias_nacionales_consolidado " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:trimestre IS NULL OR EXTRACT(QUARTER FROM CAST(fecha AS date)) = :trimestre) " +
            "AND EXTRACT(YEAR FROM CAST(fecha AS date)) IN (:anio, :anio - 1)", nativeQuery = true)
    Map<String, BigDecimal> obtenerComparativaInteranual(@Param("anio") Integer anio, @Param("estado") String estado, @Param("trimestre") Integer trimestre);

    // KPI 5: Días Críticos
    @Query(value = "SELECT " +
            "EXTRACT(ISODOW FROM CAST(fecha AS date)) AS dia_semana, " +
            "SUM(total_emergencias) AS total_emergencias " +
            "FROM emergencias_nacionales_consolidado " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:anio IS NULL OR EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio) " +
            "AND (:trimestre IS NULL OR EXTRACT(QUARTER FROM CAST(fecha AS date)) = :trimestre) " +
            "GROUP BY EXTRACT(ISODOW FROM CAST(fecha AS date)) " +
            "ORDER BY dia_semana", nativeQuery = true)
    List<Map<String, Object>> obtenerDiasCriticos(@Param("estado") String estado, @Param("anio") Integer anio, @Param("trimestre") Integer trimestre);

    // KPI 1: Saturación Geoespacial
    @Query(value = "SELECT estado, SUM(total_emergencias) AS total_emergencias " +
            "FROM emergencias_nacionales_consolidado " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:anio IS NULL OR EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio) " +
            "AND (:trimestre IS NULL OR EXTRACT(QUARTER FROM CAST(fecha AS date)) = :trimestre) " +
            "GROUP BY estado " +
            "ORDER BY total_emergencias DESC", nativeQuery = true)
    List<Map<String, Object>> obtenerSaturacion(@Param("estado") String estado, @Param("anio") Integer anio, @Param("trimestre") Integer trimestre);

    // KPI 2: Tendencia Histórica
    @Query(value = "SELECT TO_CHAR(CAST(fecha AS date), 'YYYY-MM') AS mes, " +
            "SUM(total_accidentes) AS accidentes_viales, " +
            "SUM(total_delitos) AS delitos_registrados " +
            "FROM emergencias_nacionales_consolidado " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:anio IS NULL OR EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio) " +
            "AND (:trimestre IS NULL OR EXTRACT(QUARTER FROM CAST(fecha AS date)) = :trimestre) " +
            "GROUP BY TO_CHAR(CAST(fecha AS date), 'YYYY-MM') " +
            "ORDER BY mes", nativeQuery = true)
    List<Map<String, Object>> obtenerTendencia(@Param("estado") String estado, @Param("anio") Integer anio, @Param("trimestre") Integer trimestre);

    // KPI 3: Proporción (Viales vs Delitos)
    @Query(value = "SELECT COALESCE(SUM(total_accidentes), 0) AS total_accidentes, " +
            "COALESCE(SUM(total_delitos), 0) AS total_delitos " +
            "FROM emergencias_nacionales_consolidado " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:anio IS NULL OR EXTRACT(YEAR FROM CAST(fecha AS date)) = :anio) " +
            "AND (:trimestre IS NULL OR EXTRACT(QUARTER FROM CAST(fecha AS date)) = :trimestre)", nativeQuery = true)
    Map<String, Object> obtenerProporcion(@Param("estado") String estado, @Param("anio") Integer anio, @Param("trimestre") Integer trimestre);
}