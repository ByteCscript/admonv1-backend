# Administración Backend

Backend del sistema de administración desarrollado con **Java y Spring Boot**.

El proyecto expone una API REST encargada de gestionar funcionalidades relacionadas con convocatorias, postulaciones, documentos, usuarios y autenticación.

---

## 🛠️ Stack tecnológico

El proyecto utiliza actualmente:

* **Java 21**
* **Spring Boot 4.1.1**
* **Maven**
* **Spring Web MVC**
* **Spring Data JPA**
* **Spring Security**
* **OAuth2 Resource Server / JWT**
* **Spring Validation**
* **H2 Database**
* **AWS SDK for Java - S3**
* **OpenAPI / Swagger**
* **Lombok**
* **Docker**

---

## 🏗️ Arquitectura actual

El backend está organizado utilizando una **arquitectura por capas (Layered Architecture)**.

La aplicación separa las responsabilidades principales en controladores, servicios, repositorios y modelos.

El flujo general de una petición es:

```text
Client
   │
   ▼
Controller
   │
   ▼
Service Interface
   │
   ▼
Service Implementation
   │
   ▼
Repository
   │
   ▼
JPA Entity / Model
   │
   ▼
Database
```

Esta estructura permite mantener separadas las responsabilidades de exposición de endpoints, lógica de negocio y acceso a datos.

Actualmente el proyecto **no implementa Clean Architecture ni Arquitectura Hexagonal**. La organización corresponde principalmente al patrón tradicional de capas utilizado en aplicaciones Spring Boot.

---

## 📂 Estructura del proyecto

```text
src/main/java/com/administracionback/admonv1
│
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtConfig.java
│   ├── JwtService.java
│   ├── LoginRequestDTO.java
│   └── LoginResponseDTO.java
│
├── config/
│   ├── ApplicationConfg.java
│   ├── CorsConfig.java
│   ├── CustomUserDetailsService.java
│   ├── OpenApiConfig.java
│   ├── S3Config.java
│   └── SecurityConfig.java
│
├── controller/
│   ├── ApplicationController.java
│   ├── CallController.java
│   ├── DocumentController.java
│   └── HolaController.java
│
├── dto/
│   ├── ApiResponse.java
│   ├── ApplicationDetailResponseDTO.java
│   ├── ApplicationEligibilityResponseDTO.java
│   ├── ApplicationFilterDTO.java
│   ├── ApplicationRequestDTO.java
│   ├── ApplicationResponseDTO.java
│   ├── CallDTO.java
│   ├── CallDetailDTO.java
│   ├── DocumentPresignedRequestDTO.java
│   ├── DocumentPresignedResponseDTO.java
│   ├── DocumentResponseDTO.java
│   ├── DocumentTypeResponseDTO.java
│   └── PageResponseDTO.java
│
├── model/
│   ├── Apartment.java
│   ├── Application.java
│   ├── ApplicationStatus.java
│   ├── Call.java
│   ├── CallStatus.java
│   ├── Document.java
│   ├── DocumentStatus.java
│   ├── DocumentType.java
│   ├── Role.java
│   ├── Tower.java
│   └── User.java
│
├── repository/
│   ├── ApplicationRepository.java
│   ├── ApplicationSpecification.java
│   ├── CallRepository.java
│   ├── DocumentRepository.java
│   ├── RoleRepository.java
│   └── UserRepository.java
│
├── service/
│   ├── IApplicationService.java
│   ├── ICallService.java
│   ├── IDocumentService.java
│   ├── ApplicationServiceImpl.java
│   ├── CallServiceImpl.java
│   └── DocumentServiceImpl.java
│
└── Admonv1Application.java
```

---

## 📦 Responsabilidad de las capas

### `controller`

Contiene los controladores REST de la aplicación.

Su responsabilidad principal es recibir las solicitudes HTTP, validar los datos de entrada correspondientes y delegar la ejecución de la lógica a la capa de servicios.

Ejemplos:

```text
ApplicationController
CallController
DocumentController
```

---

### `service`

Contiene la lógica de aplicación y negocio.

Se utilizan interfaces para definir los contratos de los servicios:

```text
IApplicationService
ICallService
IDocumentService
```

y clases concretas para implementarlos:

```text
ApplicationServiceImpl
CallServiceImpl
DocumentServiceImpl
```

Esto reduce el acoplamiento directo entre los controladores y las implementaciones concretas de los servicios.

---

### `repository`

Representa la capa de acceso a datos.

Los repositorios utilizan **Spring Data JPA** para interactuar con la base de datos.

Entre los repositorios actuales se encuentran:

```text
ApplicationRepository
CallRepository
DocumentRepository
RoleRepository
UserRepository
```

También existe:

```text
ApplicationSpecification
```

para la construcción de consultas dinámicas relacionadas con postulaciones.

---

### `model`

Contiene las entidades y enumeraciones utilizadas por el modelo de datos de la aplicación.

Entre las principales entidades se encuentran:

```text
User
Role
Apartment
Tower
Call
Application
Document
DocumentType
```

También se manejan estados mediante enumeraciones como:

```text
CallStatus
ApplicationStatus
DocumentStatus
```

---

### `dto`

Contiene los **Data Transfer Objects (DTO)** utilizados para transportar información entre la API y sus consumidores.

Esto permite evitar exponer directamente las entidades de persistencia mediante los endpoints REST.

Existen DTOs específicos para:

* Convocatorias.
* Postulaciones.
* Documentos.
* Paginación.
* Respuestas generales de la API.

---

### `auth`

Contiene los componentes relacionados con autenticación y manejo de tokens.

Actualmente incluye:

```text
AuthController
AuthService
JwtService
JwtConfig
LoginRequestDTO
LoginResponseDTO
```

La seguridad de la aplicación se complementa mediante **Spring Security** y soporte para **JWT / OAuth2 Resource Server**.

---

### `config`

Centraliza configuraciones transversales de la aplicación.

Actualmente contiene configuración relacionada con:

* Spring Security.
* CORS.
* JWT.
* OpenAPI.
* AWS S3.
* User Details.

---

## ☁️ Integración con AWS S3

El proyecto incluye el **AWS SDK for Java** y configuración para Amazon S3.

Esta integración es utilizada por el módulo de documentos para soportar operaciones relacionadas con almacenamiento de archivos y generación de URLs prefirmadas.

---

## 🔐 Seguridad

La aplicación utiliza **Spring Security**.

El proyecto cuenta con componentes específicos para autenticación y generación/manejo de JWT.

El flujo general de autenticación es:

```text
Credentials
     │
     ▼
AuthController
     │
     ▼
AuthService
     │
     ▼
JwtService
     │
     ▼
JWT
     │
     ▼
Protected API Resources
```

---

## 📖 Documentación de API

El proyecto incorpora **SpringDoc OpenAPI**, permitiendo generar documentación de los endpoints REST mediante Swagger/OpenAPI.

La configuración correspondiente se encuentra en:

```text
config/OpenApiConfig.java
```

---

## 🗄️ Persistencia

La persistencia se implementa mediante:

```text
Spring Data JPA
```

Actualmente el proyecto incluye soporte para:

```text
H2 Database
```

La configuración específica de conexión se encuentra en:

```text
src/main/resources/application.properties
```

También existe:

```text
src/main/resources/data.sql
```

para carga o inicialización de datos según la configuración del entorno.

---

## 🐳 Docker

El repositorio contiene un:

```text
Dockerfile
```

que permite construir una imagen del backend para su ejecución en contenedores.

---

## 🚀 CI/CD y despliegue

El repositorio incluye archivos relacionados con el proceso de construcción y despliegue:

```text
buildspec.yml
appspec.yml
deploy/
```

Dentro de `deploy/` se encuentran scripts para diferentes etapas del despliegue:

```text
before-install.sh
after-install.sh
start.sh
validate.sh
```

Esta estructura permite automatizar tareas asociadas al proceso de instalación, inicio y validación de la aplicación.

---

## ▶️ Ejecución local

### Requisitos

Tener instalado:

```text
Java 21
```

El proyecto incluye **Maven Wrapper**, por lo que no es obligatorio tener Maven instalado globalmente.

### macOS / Linux

```bash
./mvnw spring-boot:run
```

### Windows

```bash
mvnw.cmd spring-boot:run
```

---

## 🔨 Compilar el proyecto

```bash
./mvnw clean package
```

El artefacto generado será almacenado en:

```text
target/
```

---

## 🧪 Ejecutar pruebas

```bash
./mvnw test
```

---

## 🧭 Estado arquitectónico

La estructura actual representa el **AS-IS arquitectónico** del backend.

```text
Presentation
     │
     ▼
Business / Application Services
     │
     ▼
Persistence
     │
     ▼
Database
```

La organización actual es principalmente **package-by-layer**, donde los componentes se agrupan según su responsabilidad técnica:

```text
controller
service
repository
model
dto
```

Esta documentación sirve como punto de referencia para comprender la arquitectura actual y facilitar futuras decisiones de evolución, modularización o migración del sistema.

---

## 📌 Resumen

```text
Architecture : Layered Architecture
Packaging    : Package by Layer
Language     : Java 21
Framework    : Spring Boot
Build Tool   : Maven
Web          : Spring MVC
Persistence  : Spring Data JPA
Security     : Spring Security + JWT
Storage      : AWS S3
API Docs     : OpenAPI / Swagger
Container    : Docker
```
