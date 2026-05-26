# API REST: Inteligencia de Emergencias Nacionales (Backend)

Este repositorio contiene el servidor backend desarrollado en Java y Spring Boot. Funciona como una API RESTful de solo lectura que consume un Data Mart consolidado de emergencias (PostgreSQL) y expone endpoints optimizados para el consumo de una aplicación móvil en Flutter.

## Equipo de Desarrollo (Célula Backend)
* **Manuel Hernández Soriano** - Tech Lead / Arquitectura y Base de Datos
* **Eric García Gallegos** - Desarrollo Backend
* **Rodrigo Candelaria Velazquez** - Desarrollo Backend

*(Nota: La aplicación cliente está siendo desarrollada en un repositorio separado por el equipo de Frontend: Jenny, Angel y Baruc).*

## 🛠 Stack Tecnológico
* **Lenguaje:** Java 17+
* **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA)
* **Base de Datos:** PostgreSQL
* **Patrón Arquitectónico:** MVC Simplificado (Controller -> Repository -> Entity/Projection)

## Configuración y Ejecución Local

1. Clonar el repositorio y moverse a la rama de desarrollo:
   ```bash
   git clone [URL_DEL_REPO]
   git checkout develop



2. Modificar el archivo `src/main/resources/application.properties` con las credenciales de la instancia local de PostgreSQL. Asegurarse de que la base de datos `bd_emergencias_nacional` esté corriendo.
3. El framework está configurado con `ddl-auto=none` para no alterar el esquema provisto por el pipeline ETL.
4. Compilar y ejecutar mediante Maven:
   ```bash
   ./mvnw spring-boot:run



El servidor se inicializará en `http://localhost:8080`.

## Contratos de API (Endpoints Disponibles)

* `GET /api/v1/kpi/saturacion`: Retorna top de estados colapsados (Volumen).
* `GET /api/v1/kpi/tendencia`: Retorna histórico de accidentes vs delitos (Temporalidad).
* `GET /api/v1/kpi/proporcion`: Retorna distribución porcentual nacional.

