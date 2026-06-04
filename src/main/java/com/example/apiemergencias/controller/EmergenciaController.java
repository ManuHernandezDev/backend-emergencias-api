package com.example.apiemergencias.controller;

import com.example.apiemergencias.projection.ProporcionNacionalProjection;
import com.example.apiemergencias.projection.SaturacionEstadoProjection;
import com.example.apiemergencias.projection.TendenciaHistoricaProjection;
import com.example.apiemergencias.repository.EmergenciaRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/api/v1/kpi")
@CrossOrigin(origins = "*") // Fundamental para que Flutter no tenga bloqueos de red
public class EmergenciaController {

    @Autowired
    private EmergenciaRepository repository;
    private final EmergenciaRepository emergenciaRepository;

    public EmergenciaController(EmergenciaRepository emergenciaRepository) {
        this.emergenciaRepository = emergenciaRepository;
    }

    // ==========================================
    // ENDPOINT KPI 1: SATURACIÓN
    // ==========================================
    // ==========================================
    // ENDPOINT KPI 1: SATURACIÓN (TODOS LOS ESTADOS)
    // ==========================================
    @GetMapping("/saturacion")
    public ResponseEntity<Map<String, Object>> getSaturacion(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) String trimestre) {

        Integer numTrimestre = parseTrimestre(trimestre);
        List<Map<String, Object>> dbResult = repository.obtenerSaturacion(estado, anio, numTrimestre);
        List<Map<String, Object>> dataResponse = new ArrayList<>();

        // Recorremos la lista completa de estados sin límite
        for (Map<String, Object> fila : dbResult) {
            int total = ((Number) fila.get("total_emergencias")).intValue();
            String nivelAlerta = total > 15000 ? "ROJO" : (total > 8000 ? "NARANJA" : "VERDE");

            dataResponse.add(Map.of(
                    "estado", fila.get("estado").toString(),
                    "totalEmergencias", total,
                    "nivelAlerta", nivelAlerta
            ));
        }
        return ResponseEntity.ok(Map.of("status", "success", "data", dataResponse));
    }

    // ==========================================
    // ENDPOINT KPI 2: TENDENCIA HISTÓRICA
    // ==========================================
    @GetMapping("/tendencia")
    public ResponseEntity<Map<String, Object>> getTendencia(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) String trimestre) {

        Integer numTrimestre = parseTrimestre(trimestre);
        List<Map<String, Object>> dbResult = repository.obtenerTendencia(estado, anio, numTrimestre );
        List<Map<String, Object>> dataResponse = new ArrayList<>();

        for (Map<String, Object> fila : dbResult) {
            dataResponse.add(Map.of(
                    "fecha", fila.get("mes").toString(),
                    "accidentesViales", ((Number) fila.get("accidentes_viales")).intValue(),
                    "delitosRegistrados", ((Number) fila.get("delitos_registrados")).intValue()
            ));
        }
        return ResponseEntity.ok(Map.of("status", "success", "data", dataResponse));
    }

    // ==========================================
    // ENDPOINT KPI 3: PROPORCIÓN
    // ==========================================
    @GetMapping("/proporcion")
    public ResponseEntity<Map<String, Object>> getProporcion(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) String trimestre) {

        Integer numTrimestre = parseTrimestre(trimestre);
        Map<String, Object> dbResult = repository.obtenerProporcion(estado, anio, numTrimestre);

        double accidentes = ((Number) dbResult.get("total_accidentes")).doubleValue();
        double delitos = ((Number) dbResult.get("total_delitos")).doubleValue();
        double total = accidentes + delitos;

        double porcentajeAccidentes = 0.0;
        double porcentajeDelitos = 0.0;

        if (total > 0) {
            // Calculamos los porcentajes y redondeamos a 1 decimal
            porcentajeAccidentes = Math.round((accidentes / total * 100) * 10.0) / 10.0;
            porcentajeDelitos = Math.round((delitos / total * 100) * 10.0) / 10.0;
        }

        List<Map<String, Object>> dataResponse = List.of(
                Map.of("tipo", "Accidentes Viales", "porcentaje", porcentajeAccidentes),
                Map.of("tipo", "Delitos Operativos", "porcentaje", porcentajeDelitos)
        );

        return ResponseEntity.ok(Map.of("status", "success", "data", dataResponse));
    }

    // Método auxiliar (Clean Code) para no repetir la estructura de éxito en cada endpoint
    private ResponseEntity<Map<String, Object>> construirRespuestaEstandar(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    // ENDPOINT 4: CRECIMIENTO INTERANUAL
    @GetMapping("/comparativa")
    public ResponseEntity<Map<String, Object>> getComparativa(
            @RequestParam(required = false, defaultValue = "2024") Integer anio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String trimestre) {

        Integer numTrimestre = parseTrimestre(trimestre);
        Map<String, BigDecimal> dbResult = repository.obtenerComparativaInteranual(anio, estado, numTrimestre);

        double totalActual = dbResult.get("total_actual").doubleValue();
        double totalAnterior = dbResult.get("total_anterior").doubleValue();

        double porcentaje = 0.0;
        String tendencia = "";
        String mensajeAlterno = null; // Para guardar el aviso de 2021

        // REGLA DE NEGOCIO: EXCEPCIÓN 2021
        if (anio == 2022) {
            tendencia = "INFO";
            mensajeAlterno = "No hay datos registrados para 2021";
            totalAnterior = 0; // Se fuerza a 0 por seguridad
        } else {
            if (totalAnterior > 0) {
                porcentaje = ((totalActual - totalAnterior) / totalAnterior) * 100;
            }
            tendencia = porcentaje >= 0 ? "ALZA" : "BAJA";
        }

        BigDecimal porcentajeRedondeado = new BigDecimal(porcentaje).setScale(1, RoundingMode.HALF_UP);
        String estadoString = (estado != null && !estado.isEmpty()) ? estado : "Nacional";

        Map<String, Object> data = new HashMap<>();
        data.put("kpi", "Crecimiento Interanual");
        data.put("estado", estadoString);
        data.put("anioActual", anio);
        data.put("totalActual", (int) totalActual);
        data.put("anioAnterior", anio - 1);
        data.put("totalAnterior", (int) totalAnterior);
        data.put("tendencia", tendencia);
        data.put("porcentajeCambio", porcentajeRedondeado.doubleValue());

        // Enviamos el mensaje al frontend si existe
        if (mensajeAlterno != null) {
            data.put("mensajeEspecial", mensajeAlterno);
        }

        return ResponseEntity.ok(Map.of("status", "success", "data", data));
    }

    // ENDPOINT 5: MAPA DE CALOR POR DÍAS
    @GetMapping("/dias-criticos")
    public ResponseEntity<Map<String, Object>> getDiasCriticos(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) String trimestre) {

        Integer numTrimestre = parseTrimestre(trimestre);
        List<Map<String, Object>> dbResult = repository.obtenerDiasCriticos(estado, anio, numTrimestre);

        // Diccionario para convertir números del motor SQL a nombres de días
        String[] nombresDias = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

        List<Map<String, Object>> dataResponse = new ArrayList<>();

        for (Map<String, Object> fila : dbResult) {
            int numDia = ((Number) fila.get("dia_semana")).intValue();
            int total = ((Number) fila.get("total_emergencias")).intValue();

            dataResponse.add(Map.of(
                    "dia", nombresDias[numDia],
                    "totalEmergencias", total
            ));
        }

        return ResponseEntity.ok(Map.of("status", "success", "data", dataResponse));
    }
    // ==========================================
    // ENDPOINT PARA DESCARGAR EL REPORTE EN PDF (VERSIÓN PREMIUM)
    // ==========================================
    @GetMapping(value = "/reportes/generar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarReportePdf(
            @RequestParam(required = false, defaultValue = "2024") Integer anio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String trimestre) {

        Document document = new Document(PageSize.A4, 40, 40, 50, 40); // Márgenes profesionales
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. PALETA DE COLORES Y FUENTES PREMIUM
            BaseColor COLOR_PRIMARIO = new BaseColor(44, 62, 80);    // Azul Oscuro (Cabeceras)
            BaseColor COLOR_SECUNDARIO = new BaseColor(41, 128, 185); // Azul Claro (Secciones)
            BaseColor COLOR_ALERTA = new BaseColor(231, 76, 60);     // Rojo
            BaseColor COLOR_EXITO = new BaseColor(39, 174, 96);      // Verde
            BaseColor COLOR_GRIS_CLARO = new BaseColor(242, 242, 242);

            Font fuenteTitulo = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, COLOR_PRIMARIO);
            Font fuenteSubtitulo = new Font(Font.FontFamily.HELVETICA, 14, Font.ITALIC, BaseColor.DARK_GRAY);
            Font fuenteSeccion = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, COLOR_SECUNDARIO);
            Font fuenteTexto = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
            Font fuenteCabeceraTabla = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);

            // 2. CABECERA DEL DOCUMENTO
            Paragraph titulo = new Paragraph("CENTRO DE MANDO - SISTEMA 911", fuenteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Reporte Ejecutivo Integral de Operaciones", fuenteSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitulo);
            document.add(new Paragraph(" "));

            // 3. CINTA DE FILTROS SELECCIONADOS
            String lblEstado = (estado != null && !estado.isEmpty()) ? estado : "Nacional";
            String lblTrimestre = (trimestre != null && !trimestre.equals("Todos")) ? trimestre : "Todo el año";

            PdfPTable tablaFiltros = new PdfPTable(3);
            tablaFiltros.setWidthPercentage(100);
            tablaFiltros.addCell(crearCelda("Año: " + anio, fuenteCabeceraTabla, COLOR_PRIMARIO, true));
            tablaFiltros.addCell(crearCelda("Estado: " + lblEstado, fuenteCabeceraTabla, COLOR_PRIMARIO, true));
            tablaFiltros.addCell(crearCelda("Trimestre: " + lblTrimestre, fuenteCabeceraTabla, COLOR_PRIMARIO, true));
            document.add(tablaFiltros);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            Integer numTrim = parseTrimestre(trimestre);

            // ==========================================================
            // SECCIÓN 1: RESUMEN OPERATIVO INTERANUAL
            // ==========================================================
            document.add(new Paragraph("1. Resumen Operativo Interanual", fuenteSeccion));
            document.add(new Paragraph(" ", fuenteTexto));

            Map<String, BigDecimal> dbResultComparativa = repository.obtenerComparativaInteranual(anio, estado, numTrim);
            int totalActual = dbResultComparativa.get("total_actual").intValue();
            int totalAnterior = dbResultComparativa.get("total_anterior").intValue();

            PdfPTable tablaCrecimiento = new PdfPTable(2);
            tablaCrecimiento.setWidthPercentage(100);
            tablaCrecimiento.addCell(crearCelda("Periodo Evaluado", fuenteCabeceraTabla, COLOR_SECUNDARIO, false));
            tablaCrecimiento.addCell(crearCelda("Volumen Total de Emergencias", fuenteCabeceraTabla, COLOR_SECUNDARIO, false));

            tablaCrecimiento.addCell(crearCelda("Año " + anio, fuenteTexto, BaseColor.WHITE, false));
            tablaCrecimiento.addCell(crearCelda(String.format("%,d", totalActual), fuenteTexto, BaseColor.WHITE, false));

            tablaCrecimiento.addCell(crearCelda("Año " + (anio - 1), fuenteTexto, COLOR_GRIS_CLARO, false));
            tablaCrecimiento.addCell(crearCelda(String.format("%,d", totalAnterior), fuenteTexto, COLOR_GRIS_CLARO, false));
            document.add(tablaCrecimiento);

            if (anio != null && anio == 2022) {
                document.add(new Paragraph("ℹ️ NOTA: No se cuenta con datos históricos validados del año 2021 para realizar la comparativa.", new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, BaseColor.GRAY)));
            } else if (totalActual > totalAnterior) {
                document.add(new Paragraph(">> ALERTA: Existe un incremento en el volumen operativo. Se sugiere revisión de estrategias de contención.", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, COLOR_ALERTA)));
            } else if (totalActual < totalAnterior) {
                document.add(new Paragraph(">> ÉXITO: Se registra una disminución en el volumen operativo. Las medidas muestran resultados favorables.", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, COLOR_EXITO)));
            }
            document.add(new Paragraph(" "));

            // ==========================================================
            // SECCIÓN 2: DISTRIBUCIÓN DE CARGA OPERATIVA (NUEVO)
            // ==========================================================
            document.add(new Paragraph("2. Distribución de la Carga Operativa", fuenteSeccion));
            document.add(new Paragraph(" ", fuenteTexto));

            Map<String, Object> dbResultProp = repository.obtenerProporcion(estado, anio, numTrim);
            double accidentes = ((Number) dbResultProp.get("total_accidentes")).doubleValue();
            double delitos = ((Number) dbResultProp.get("total_delitos")).doubleValue();
            double total = accidentes + delitos;
            double pAcc = total > 0 ? (accidentes / total * 100) : 0;
            double pDel = total > 0 ? (delitos / total * 100) : 0;

            PdfPTable tablaProp = new PdfPTable(3);
            tablaProp.setWidthPercentage(100);
            tablaProp.addCell(crearCelda("Tipo de Incidente", fuenteCabeceraTabla, COLOR_SECUNDARIO, false));
            tablaProp.addCell(crearCelda("Total Registrado", fuenteCabeceraTabla, COLOR_SECUNDARIO, false));
            tablaProp.addCell(crearCelda("Porcentaje (%)", fuenteCabeceraTabla, COLOR_SECUNDARIO, false));

            tablaProp.addCell(crearCelda("Accidentes Viales", fuenteTexto, BaseColor.WHITE, false));
            tablaProp.addCell(crearCelda(String.format("%,d", (int)accidentes), fuenteTexto, BaseColor.WHITE, false));
            tablaProp.addCell(crearCelda(String.format("%.1f%%", pAcc), fuenteTexto, BaseColor.WHITE, false));

            tablaProp.addCell(crearCelda("Delitos Operativos", fuenteTexto, COLOR_GRIS_CLARO, false));
            tablaProp.addCell(crearCelda(String.format("%,d", (int)delitos), fuenteTexto, COLOR_GRIS_CLARO, false));
            tablaProp.addCell(crearCelda(String.format("%.1f%%", pDel), fuenteTexto, COLOR_GRIS_CLARO, false));
            document.add(tablaProp);
            document.add(new Paragraph(" "));

            // ==========================================================
            // SECCIÓN 3: MAPA OPERATIVO SEMANAL
            // ==========================================================
            document.add(new Paragraph("3. Mapa Operativo Semanal (Días Críticos)", fuenteSeccion));
            document.add(new Paragraph(" ", fuenteTexto));

            List<Map<String, Object>> dbResultDias = repository.obtenerDiasCriticos(estado, anio, numTrim);
            String[] nombresDias = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

            PdfPTable tablaDias = new PdfPTable(2);
            tablaDias.setWidthPercentage(100);
            tablaDias.addCell(crearCelda("Día de la Semana", fuenteCabeceraTabla, COLOR_SECUNDARIO, false));
            tablaDias.addCell(crearCelda("Volumen Promedio/Total", fuenteCabeceraTabla, COLOR_SECUNDARIO, false));

            boolean colorFondo = false;
            for (Map<String, Object> fila : dbResultDias) {
                int numDia = ((Number) fila.get("dia_semana")).intValue();
                int totalDia = ((Number) fila.get("total_emergencias")).intValue();

                BaseColor bgColor = colorFondo ? COLOR_GRIS_CLARO : BaseColor.WHITE;
                tablaDias.addCell(crearCelda(nombresDias[numDia], fuenteTexto, bgColor, false));
                tablaDias.addCell(crearCelda(String.format("%,d", totalDia), fuenteTexto, bgColor, false));
                colorFondo = !colorFondo;
            }
            document.add(tablaDias);

            // ==========================================================
            // PIE DE PÁGINA CORPORATIVO
            // ==========================================================
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph pie = new Paragraph("Documento generado automáticamente por el Centro de Mando BI - Sistema 911.\nLa información contenida es de carácter confidencial y para uso exclusivo de inteligencia operativa.", new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "Reporte_Ejecutivo_911.pdf");
        headers.setContentType(MediaType.APPLICATION_PDF);

        return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
    }

    // ==========================================================
    // MÉTODO AUXILIAR PARA DISEÑAR LAS CELDAS DE LAS TABLAS
    // ==========================================================
    private PdfPCell crearCelda(String contenido, Font fuente, BaseColor colorFondo, boolean sinBorde) {
        PdfPCell celda = new PdfPCell(new Phrase(contenido, fuente));
        celda.setBackgroundColor(colorFondo);
        celda.setPadding(8f); // Más espacio interior para que no se vea apretado
        if (sinBorde) {
            celda.setBorder(Rectangle.NO_BORDER);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        } else {
            celda.setBorderColor(new BaseColor(189, 195, 199)); // Borde gris sutil
        }
        return celda;
    }


    private Integer parseTrimestre(String trimestre) {
        if (trimestre == null || trimestre.equals("Todos")) return null;
        switch (trimestre) {
            case "T1": return 1;
            case "T2": return 2;
            case "T3": return 3;
            case "T4": return 4;
            default: return null;
        }
    }

}