# Sistema de Reservas de Restaurante

Sistema web para la gestión de reservas de un restaurante, desarrollado con **Spring Boot 3.2.5**, Java 17, Thymeleaf, Spring Security y MySQL.

## ¿Qué hace este proyecto?

Permite a los clientes realizar reservas de mesas eligiendo platos del menú, y a los administradores gestionar mesas, carta, reservas y usuarios. Incluye control de acceso por roles (ADMIN y CLIENTE), validaciones de negocio y carga automática de datos de ejemplo.

## ¿Por qué es útil?

- Facilita la gestión de reservas evitando conflictos de mesas.
- Separación clara de responsabilidades entre cliente y administrador.
- Incluye reglas de negocio reales (una reserva activa por cliente, plato principal obligatorio, etc.).
- Ideal como proyecto académico de Spring Boot con seguridad, JPA y arquitectura en capas.

## Tecnologías utilizadas

| Componente | Tecnología |
| :--- | :--- |
| **Lenguaje** | Java 17 |
| **Framework** | Spring Boot 3.2.5 |
| **Seguridad** | Spring Security + BCrypt |
| **Persistencia** | Spring Data JPA / Hibernate |
| **Base de Datos** | MySQL |
| **Vista** | Thymeleaf + CSS3 |

## Requisitos

- Java 17 JDK
- Apache Maven 3.8+
- MySQL Server corriendo en `localhost:3306` con la base de datos `restaurante_db` creada.

## Configuración de la base de datos

El proyecto crea la base de datos automáticamente. Si tu usuario o contraseña de MySQL son diferentes, edita el archivo:

`src/main/resources/application.properties`

```properties
spring.datasource.username=root
spring.datasource.password=root


