# CONTEXTO TÉCNICO PARA AGENTE DE IA
> **Instrucción para el LLM/Agente:** Lee todo este documento antes de generar o refactorizar cualquier bloque de código en este repositorio. Actúa como un desarrollador Senior de Java.

## 1. Estado de la Arquitectura
* **Tipo de Proyecto:** API REST de solo lectura en Spring Boot.
* **Base de datos:** PostgreSQL. La base de datos es un Data Mart preprocesado.
* **Restricción Crítica:** ESTÁ ESTRICTAMENTE PROHIBIDO generar código que cree, altere o borre tablas. Usa `nativeQuery = true` en JPA para consultas complejas.
* **Patrón de Retorno:** No se utilizan clases DTO tradicionales. Se utilizan **Projections** (Interfaces de Spring Data) para optimizar la memoria y mapear directamente los resultados del `GROUP BY` de SQL.

## 2. Convenciones de Código (Clean Code)
* Los controladores devuelven un `ResponseEntity<Map<String, Object>>` encapsulando la respuesta en un formato JSON estándar: `{"status": "success", "data": [...]}`.
* Es obligatorio incluir `@CrossOrigin(origins = "*")` en los controladores para permitir la conexión desde la aplicación móvil en Flutter.
* Nombramiento de variables en *camelCase*. Nombramiento de ramas en Git: `feat/nombre`, `fix/nombre`.

## 3. Registro de Cambios Activos (Ledger)
*(Añadir aquí el log de lo que se acaba de implementar para que la IA sepa en qué punto estamos)*

* **[2026-05-24] - Manu:** Repositorio inicializado. `application.properties` configurado. Estructura de paquetes base creada (`controller`, `repository`, `projection`, `model`). Pendiente: Crear la clase `@Entity` para mapear la tabla `emergencias_nacionales_consolidado` y escribir los tres Endpoints de lectura.

## 4. Tarea Actual
[El desarrollador describirá aquí brevemente el objetivo actual antes de pasarlo al prompt, ej: "Necesito implementar la interfaz de proyección y el repositorio para el KPI de Saturación"].