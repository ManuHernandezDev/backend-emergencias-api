package com.example.apiemergencias.controller;

import com.example.apiemergencias.projection.ProporcionNacionalProjection;
import com.example.apiemergencias.projection.SaturacionEstadoProjection;
import com.example.apiemergencias.projection.TendenciaHistoricaProjection;
import com.example.apiemergencias.repository.EmergenciaRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
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

    // ENDPOINT 4: CRECIMIENTO INTERANUAL
    @GetMapping("/comparativa")
    public ResponseEntity<Map<String, Object>> getComparativa(
            @RequestParam(required = false, defaultValue = "2024") Integer anio,
            @RequestParam(required = false) String estado) {

        Map<String, BigDecimal> dbResult = repository.obtenerComparativaInteranual(anio, estado);

        double totalActual = dbResult.get("total_actual").doubleValue();
        double totalAnterior = dbResult.get("total_anterior").doubleValue();

        // Calcular porcentaje de cambio
        double porcentaje = 0.0;
        if (totalAnterior > 0) {
            porcentaje = ((totalActual - totalAnterior) / totalAnterior) * 100;
        }

        BigDecimal porcentajeRedondeado = new BigDecimal(porcentaje).setScale(1, RoundingMode.HALF_UP);
        String tendencia = porcentaje >= 0 ? "ALZA" : "BAJA";
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

        return ResponseEntity.ok(Map.of("status", "success", "data", data));
    }

    // ENDPOINT 5: MAPA DE CALOR POR DÍAS
    @GetMapping("/dias-criticos")
    public ResponseEntity<Map<String, Object>> getDiasCriticos(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer anio) {

        List<Map<String, Object>> dbResult = repository.obtenerDiasCriticos(estado, anio);

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
    // ENDPOINT PARA DESCARGAR EL REPORTE EN PDF
    @GetMapping(value = "/reportes/generar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarReportePdf(
            @RequestParam(required = false, defaultValue = "2024") Integer anio,
            @RequestParam(required = false) String estado) {

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. PALETA DE COLORES Y FUENTES CORPORATIVAS
            BaseColor colorPrimario = new BaseColor(44, 62, 80); // Azul oscuro
            BaseColor colorAcento = new BaseColor(41, 128, 185); // Azul claro
            BaseColor colorFondoTabla = new BaseColor(236, 240, 241); // Gris muy claro

            Font fuenteTitulo = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, colorPrimario);
            Font fuenteSubtitulo = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, BaseColor.GRAY);
            Font fuenteHeaderTabla = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
            Font fuenteCelda = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
            Font fuenteAlerta = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(192, 57, 43)); // Rojo
            Font fuenteExito = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(39, 174, 96)); // Verde

            // 2. CABECERA DEL DOCUMENTO
            document.add(new Paragraph("Centro de Mando - Sistema 911", fuenteTitulo));
            document.add(new Paragraph("Reporte Analítico Integral de Emergencias", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, colorAcento)));
            document.add(new Paragraph("Filtros de Extracción -> Año: " + anio + " | Estado: " + (estado != null ? estado : "Nacional"), fuenteSubtitulo));
            document.add(new Paragraph(" "));

            // ==========================================================
            // SECCIÓN 1: COMPARATIVA INTERANUAL
            // ==========================================================
            document.add(new Paragraph("1. Comparativa Interanual (Volumen Operativo)", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph(" "));

            Map<String, BigDecimal> dbResultComparativa = repository.obtenerComparativaInteranual(anio, estado);
            int totalActual = dbResultComparativa.get("total_actual").intValue();
            int totalAnterior = dbResultComparativa.get("total_anterior").intValue();

            PdfPTable tablaCrecimiento = new PdfPTable(2);
            tablaCrecimiento.setWidthPercentage(100);

            // Headers
            PdfPCell c1 = new PdfPCell(new Phrase("Periodo", fuenteHeaderTabla));
            c1.setBackgroundColor(colorPrimario);
            c1.setPadding(8f);
            tablaCrecimiento.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase("Total de Emergencias", fuenteHeaderTabla));
            c2.setBackgroundColor(colorPrimario);
            c2.setPadding(8f);
            c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaCrecimiento.addCell(c2);

            // Datos Año Actual
            PdfPCell c3 = new PdfPCell(new Phrase("Año " + anio, fuenteCelda));
            c3.setPadding(8f);
            tablaCrecimiento.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Phrase(String.valueOf(totalActual), fuenteCelda));
            c4.setPadding(8f);
            c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaCrecimiento.addCell(c4);

            // Datos Año Anterior
            PdfPCell c5 = new PdfPCell(new Phrase("Año " + (anio - 1), fuenteCelda));
            c5.setBackgroundColor(colorFondoTabla);
            c5.setPadding(8f);
            tablaCrecimiento.addCell(c5);

            PdfPCell c6 = new PdfPCell(new Phrase(String.valueOf(totalAnterior), fuenteCelda));
            c6.setBackgroundColor(colorFondoTabla);
            c6.setPadding(8f);
            c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaCrecimiento.addCell(c6);

            document.add(tablaCrecimiento);

            // Diagnóstico
            if (totalActual > totalAnterior) {
                document.add(new Paragraph(">> ALERTA: Tendencia al ALZA detectada. Se requiere reforzar los protocolos de despacho.", fuenteAlerta));
            } else if (totalActual < totalAnterior) {
                document.add(new Paragraph(">> ÉXITO: Tendencia a la BAJA registrada. Las estrategias de contención son efectivas.", fuenteExito));
            }
            document.add(new Paragraph(" "));

            // ==========================================================
            // SECCIÓN 2: DÍAS CRÍTICOS (MAPA DE CALOR)
            // ==========================================================
            document.add(new Paragraph("2. Saturación por Día de la Semana", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph("Distribución de la carga de llamadas al 911 a lo largo de la semana.", fuenteCelda));
            document.add(new Paragraph(" "));

            List<Map<String, Object>> dbResultDias = repository.obtenerDiasCriticos(estado, anio);
            String[] nombresDias = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

            PdfPTable tablaDias = new PdfPTable(2);
            tablaDias.setWidthPercentage(100);

            PdfPCell dh1 = new PdfPCell(new Phrase("Día de la Semana", fuenteHeaderTabla));
            dh1.setBackgroundColor(colorAcento);
            dh1.setPadding(6f);
            tablaDias.addCell(dh1);

            PdfPCell dh2 = new PdfPCell(new Phrase("Volumen", fuenteHeaderTabla));
            dh2.setBackgroundColor(colorAcento);
            dh2.setPadding(6f);
            dh2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaDias.addCell(dh2);

            // Iterar los días de la semana directamente desde la base de datos
            boolean colorFondo = false;
            for (Map<String, Object> fila : dbResultDias) {
                int numDia = ((Number) fila.get("dia_semana")).intValue();
                int totalDia = ((Number) fila.get("total_emergencias")).intValue();

                PdfPCell cd1 = new PdfPCell(new Phrase(nombresDias[numDia], fuenteCelda));
                cd1.setPadding(6f);
                if(colorFondo) cd1.setBackgroundColor(colorFondoTabla);
                tablaDias.addCell(cd1);

                PdfPCell cd2 = new PdfPCell(new Phrase(String.valueOf(totalDia), fuenteCelda));
                cd2.setPadding(6f);
                cd2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                if(colorFondo) cd2.setBackgroundColor(colorFondoTabla);
                tablaDias.addCell(cd2);

                colorFondo = !colorFondo; // Intercalar colores de las filas
            }

            document.add(tablaDias);

            // ==========================================================
            // PIE DE PÁGINA
            // ==========================================================
            document.add(new Paragraph(" "));
            Paragraph pie = new Paragraph("*** Documento generado automáticamente vía PostgreSQL y Spring Boot ***", new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY));
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "Reporte_Integral_911_" + anio + ".pdf");

        return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
    }
}