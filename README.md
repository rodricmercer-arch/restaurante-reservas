# Sistema de Reservas de Restaurante

Aplicación web para gestionar mesas, carta y reservas de un restaurante. Incluye una interfaz Thymeleaf con sesión y una API REST protegida con JWT.

**Stack:** Java 17 · Spring Boot 3.2.5 · Maven · Thymeleaf + Bootstrap 5 (CDN) · Spring Data JPA / Hibernate · Spring Security (sesión + JWT) · MySQL · springdoc-openapi (Swagger).

## Requisitos
- Java 17
- Maven
- MySQL en `localhost:3306`

## Configuración de la base de datos
El proyecto crea la base `restaurante_db` automáticamente (`createDatabaseIfNotExist=true`).
Edita `src/main/resources/application.properties` si tus credenciales de MySQL son distintas:

```
spring.datasource.username=root
spring.datasource.password=root
```

## Configuración de JWT
La clave de firma se lee de `app.jwt.secret`, que acepta la variable de entorno `JWT_SECRET` (mínimo 32 caracteres). El valor por defecto es solo para desarrollo local; define tu propia clave fuera del repositorio.

```
app.jwt.expiration-minutes=60
app.jwt.issuer=reservas-restaurante
```

## Ejecutar
```
mvn spring-boot:run
```
o ejecuta `ReservasApplication.java` desde tu IDE.

- Aplicación web: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Usuarios de prueba (se crean solos al arrancar)
| Rol | DNI | Contraseña |
| --- | --- | --- |
| ADMIN | 00000000 | admin123 |
| CLIENTE | 11111111 | cliente123 |

Solo son para pruebas locales. No hay registro público: el administrador crea usuarios desde `/admin/usuarios`.

## Seguridad
Hay dos cadenas de seguridad en `SecurityConfig`:

- **Web (`webChain`)**: sesión (`JSESSIONID`), login por DNI y contraseña (BCrypt), CSRF activo y logout por POST en `/logout`. Acceso denegado redirige a `/403`.
- **API (`apiChain`)**: sin sesión ni CSRF, autenticada con JWT (HS256, 60 minutos) en `/api/**` y Swagger.

Además, los servicios usan `@PreAuthorize` como segunda barrera.

| Ruta | Acceso |
| --- | --- |
| `/`, `/login`, `/403`, recursos estáticos | Público |
| `/admin/**`, `/reportes/**` | ADMIN |
| `/cliente/**` | CLIENTE |
| `/perfil/**` | CLIENTE y ADMIN |
| `GET /api/platos/**` | CLIENTE y ADMIN (JWT) |
| `POST/PUT/DELETE /api/platos/**` | ADMIN (JWT) |
| `/api/auth/login` | Público |

## API REST
Documentada en Swagger UI. Flujo básico:

1. Obtener el token:
```
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"00000000","password":"admin123"}'
```
2. Usarlo en las peticiones:
```
curl http://localhost:8080/api/platos -H "Authorization: Bearer <token>"
```

Endpoints: `POST /api/auth/login`, `GET /api/auth/me`, `GET /api/platos` (filtro opcional `?categoria=`), `GET /api/platos/{id}`, `POST /api/platos`, `PUT /api/platos/{id}`, `DELETE /api/platos/{id}`.
Los errores se devuelven como `{estado, mensaje, errores}` (400 validación, 401 credenciales, 409 regla de negocio).

## Reglas de negocio implementadas
- Un cliente solo puede tener **una reserva activa** (PENDIENTE o CONFIRMADA).
- Solo se reservan mesas **LIBRES**. El **plato principal es obligatorio**; postre y bebida son opcionales.
- Crear, cancelar y mover una reserva son operaciones transaccionales (rollback ante error). `Mesa` usa bloqueo optimista (`@Version`) para evitar que dos operaciones simultáneas confirmen la misma mesa.
- El **cliente** reserva y cancela su propia reserva. El **admin** lista y filtra reservas, las cancela y las mueve a otra mesa libre.
- El **admin** gestiona mesas, platos y usuarios. No se elimina una mesa con reservas, un plato usado en reservas, a sí mismo, a un usuario con reservas ni al último administrador.
- El **admin** consulta reportes (`/reportes`): reservas, mesas y platos por estado o categoría, top 5 de platos e ingreso estimado (sin contar reservas canceladas).
- Cualquier usuario puede ver su perfil y cambiar su contraseña en `/perfil`.

## Pruebas
```
mvn test
```
Usan JUnit 5, Mockito, MockMvc y H2 en memoria (`src/test/resources/application.properties`); no requieren MySQL. Cubren validaciones de reserva, cancelar/mover, consultas JPQL, rutas de seguridad, login JWT y el contrato HTTP de `/api/platos`.

## Estructura
Paquete base `com.restaurante.reservas`:

- `model`: entidades JPA y enumerados
- `repository`: repositorios Spring Data (métodos derivados y JPQL)
- `service`: lógica de negocio y transacciones
- `controller`: controladores MVC (Thymeleaf)
- `controller.rest`: API REST y manejo de errores
- `dto`: objetos de transferencia
- `config`: seguridad, JWT, OpenAPI y datos iniciales

## Limitaciones
Ejecución local (sin despliegue en la nube), sin pasarela de pagos, sin notificaciones en tiempo real y sin pruebas de alta concurrencia. La reserva no incluye fecha ni hora elegidas por el cliente: la mesa pasa de LIBRE a RESERVADA hasta que se cancele.