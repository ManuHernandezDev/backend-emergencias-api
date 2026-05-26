package com.example.apiemergencias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "emergencias_nacionales_consolidado")
public class EmergenciaConsolidada {
    @Id
    @Column(name = "estado")
    private String estado;

    @Column(name = "fecha")
    private String fecha;

    @Column(name = "total_delitos")
    private Long totalDelitos;

    @Column(name = "total_accidentes")
    private Long totalAccidentes;

    @Column(name = "total_emergencias")
    private Long totalEmergencias;

}