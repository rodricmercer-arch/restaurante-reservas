# Sistema de Reservas de Restaurante

Aplicación para gestionar mesas, carta y reservas de un restaurante. Tiene tres partes:

- **Backend** con interfaz Thymeleaf (sesión) y API REST protegida con JWT.
- **Frontend Angular** en `frontend/`, que consume la API REST.
- **Infraestructura** con Docker, Docker Compose, Kubernetes y workflows de GitHub Actions.

**Stack:** Java 17 · Spring Boot 3.2.5 · Maven · Thymeleaf + Bootstrap 5 (CDN) · Spring Data JPA / Hibernate · Spring Security (sesión + JWT) · MySQL · springdoc-openapi (Swagger) · Angular (`frontend/`) · nginx · Docker · Kubernetes · GitHub Actions · AWS EC2.

**Curso:** Desarrollo Web Integrado.

## Contenido

1. [Cumplimiento del tercer avance](#cumplimiento-del-tercer-avance)
2. [Puertos y servicios](#puertos-y-servicios)
3. [Requisitos](#requisitos)
4. [Ejecución local sin contenedores](#ejecución-local-sin-contenedores)
5. [Usuarios de prueba](#usuarios-de-prueba)
6. [Seguridad](#seguridad)
7. [API REST](#api-rest)
8. [Reglas de negocio implementadas](#reglas-de-negocio-implementadas)
9. [Pruebas](#pruebas)
10. [Estructura del repositorio](#estructura-del-repositorio)
11. [Flujo de trabajo con Git y GitHub](#flujo-de-trabajo-con-git-y-github)
12. [GitHub Actions](#github-actions)
13. [Docker](#docker)
14. [Kubernetes (Docker Desktop)](#kubernetes-docker-desktop)
15. [Despliegue web online (AWS EC2)](#despliegue-web-online-aws-ec2)
16. [Solución de problemas](#solución-de-problemas)
17. [Limitaciones](#limitaciones)

## Cumplimiento del tercer avance

| Requisito del docente | Puntos | Estado | Dónde se documenta |
| --- | --- | --- | --- |
| Desplegar la aplicación en una web online (al menos el frontend) | 3 | Cumplido: frontend publicado en AWS EC2 en http://13.184.109.142 | [Despliegue web online (AWS EC2)](#despliegue-web-online-aws-ec2) |
| Desplegar toda la aplicación en Kubernetes con Docker Desktop (al menos 1 pod y 1 servicio) | 5 | Cumplido: 3 Deployments (3 pods) y 3 Services en `k8s/` | [Kubernetes (Docker Desktop)](#kubernetes-docker-desktop) |
| Borrador de documentación para la entrega final (Git, GitHub Actions, Docker, Kubernetes y forma de despliegue) | 5 | Cumplido: documentado en este README | [Git](#flujo-de-trabajo-con-git-y-github) · [GitHub Actions](#github-actions) · [Docker](#docker) · [Kubernetes](#kubernetes-docker-desktop) · [AWS EC2](#despliegue-web-online-aws-ec2) |

### Distribución del trabajo del equipo

| Integrante | Módulo | Entregable |
| --- | --- | --- |
| Integrante 1 | Orquestación en Kubernetes (Docker Desktop) | Carpeta `k8s/` con `mysql.yaml`, `backend.yaml` y `frontend.yaml` |
| Integrante 2 | Contenerización con Docker | `frontend/Dockerfile`, `frontend/nginx.conf`, `docker-compose.yml` |
| Integrante 3 | Despliegue web online en AWS EC2 | Frontend publicado en http://13.184.109.142 |
| Integrante 4 | Integración Git y GitHub Actions | Ramas, Pull Requests y workflows en `.github/workflows/` |
| Integrante 5 | Documentación | Este README y el borrador de la entrega final |

## Puertos y servicios

| Entorno | Servicio | Puerto | Acceso / nota |
| --- | --- | --- | --- |
| Local sin contenedores | MySQL | 3306 | `localhost:3306`, base `restaurante_db` |
| Local sin contenedores | Backend Spring Boot | 8080 | http://localhost:8080 |
| Docker Compose | MySQL (`restaurante-mysql`) | 3307 (host) → 3306 (contenedor) | Solo pruebas locales, `root`/`root` |
| Docker Compose | Backend (`restaurante-backend`) | 8080 | http://localhost:8080 |
| Docker Compose | Frontend (`restaurante-frontend`) | 80 | http://localhost |
| Kubernetes | `mysql-service` (ClusterIP) | 3306 | Solo interno al clúster |
| Kubernetes | `backend-service` (ClusterIP) | 8080 | `kubectl port-forward svc/backend-service 8080:8080` → http://localhost:8080 |
| Kubernetes | `frontend-service` | 80 | `kubectl port-forward svc/frontend-service 4200:80` → http://localhost:4200 |
| CORS (origen por defecto) | Angular | 4200 | Valor por defecto de `app.cors.allowed-origins` |
| AWS EC2 | SSH | 22 | Abierto a `0.0.0.0/0` (solo demostración académica) |
| AWS EC2 | Frontend | 80 | http://13.184.109.142 |
| AWS EC2 | Backend | 8080 | Abierto a `0.0.0.0/0` (solo demostración académica) |

Otras URLs del backend: Swagger UI en `/swagger-ui.html` (por ejemplo http://localhost:8080/swagger-ui.html).

## Requisitos

- Java 17 y Maven (ejecución local del backend)
- MySQL en `localhost:3306` (ejecución local del backend)
- Node.js y npm (compilar el frontend en `frontend/`)
- Docker Desktop (Compose y Kubernetes)
- `kubectl` (incluido con Docker Desktop al activar Kubernetes)
- PowerShell en Windows para los comandos de este documento

## Ejecución local sin contenedores

### Base de datos

El proyecto crea la base `restaurante_db` automáticamente (`createDatabaseIfNotExist=true`).
Edita `src/main/resources/application.properties` si tus credenciales de MySQL son distintas:

```properties
spring.datasource.username=root
spring.datasource.password=root
```

> `root`/`root` son credenciales **solo para entorno local de pruebas**. No las uses en un entorno real.

### JWT

La clave de firma se lee de `app.jwt.secret`, que acepta la variable de entorno `JWT_SECRET` (mínimo 32 caracteres). El valor por defecto es solo para desarrollo local; define tu propia clave fuera del repositorio.

```properties
app.jwt.expiration-minutes=60
app.jwt.issuer=reservas-restaurante
```

### CORS

`SecurityConfig` registra CORS solo para `/api/**`, y solo en la cadena de la API:

- Orígenes permitidos: propiedad `app.cors.allowed-origins` (varios separados por coma). Por defecto: `http://localhost:4200`.
- En contenedores se define con la variable de entorno `APP_CORS_ALLOWED_ORIGINS`.
- Métodos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`. Cabeceras: `Authorization`, `Content-Type`. Sin credenciales.

### Ejecutar el backend

```powershell
mvn spring-boot:run
```

Resultado esperado: el log termina con el arranque de Spring Boot y la aplicación queda escuchando en el puerto 8080.

También puedes ejecutar `ReservasApplication.java` desde tu IDE.

- Aplicación web (Thymeleaf): http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### Frontend Angular

El frontend está en `frontend/`. La URL base de la API está definida en `api.config.ts` como `http://localhost:8080/api`.

```powershell
cd frontend
npm install
npm run build
```

Resultado esperado: el build termina sin errores y genera los archivos estáticos en `frontend/dist/frontend/browser`.

El origen `http://localhost:4200` es el que el backend acepta por defecto en CORS.

`frontend/vercel.json` contiene la regla de reescritura de todas las rutas hacia `/index.html`, necesaria para servir una SPA Angular en plataformas tipo Vercel. El despliegue online del proyecto es en AWS EC2.

## Usuarios de prueba

Se crean solos al arrancar.

| Rol | DNI | Contraseña |
| --- | --- | --- |
| ADMIN | 00000000 | admin123 |
| CLIENTE | 11111111 | cliente123 |

Solo son para pruebas locales. Existe registro público (`/registro` en la web y `POST /api/auth/registro` en la API) y el administrador también crea usuarios desde `/admin/usuarios`.

## Seguridad

`SecurityConfig` define dos cadenas de seguridad:

- **API (`apiChain`, orden 1)**: sin sesión ni CSRF, autenticada con JWT (HS256, 60 minutos). Cubre `/api/**`, `/v3/api-docs/**` y Swagger. El rol se lee del claim `roles` del token.
- **Web (`webChain`, orden 2)**: sesión (`JSESSIONID`), login por DNI y contraseña (BCrypt), CSRF activo y logout por `POST` en `/logout`. El acceso denegado redirige a `/403`.

Además, los servicios usan `@PreAuthorize` como segunda barrera (`@EnableMethodSecurity`).

### Rutas web

| Ruta | Acceso |
| --- | --- |
| `/`, `/login`, `/registro`, `/carta`, `/403`, `/error`, `/css/**`, `/js/**`, `/img/**` | Público |
| `/reportes/**` | ADMIN |
| `/perfil/**` | CLIENTE y ADMIN |
| `/admin/**` | ADMIN |
| `/cliente/**` | CLIENTE |
| Cualquier otra | Autenticado |

### Rutas de la API

| Ruta | Acceso |
| --- | --- |
| `/api/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Público |
| `POST /api/auth/registro` | Público |
| `GET /api/publico/**` | Público |
| `/api/cliente/**` | CLIENTE (JWT) |
| `/api/admin/**` | ADMIN (JWT) |
| `/api/perfil/**` | CLIENTE y ADMIN (JWT) |
| `GET /api/platos/**` | CLIENTE y ADMIN (JWT) |
| `POST`, `PUT`, `DELETE /api/platos/**` | ADMIN (JWT) |
| Cualquier otra bajo `/api/**` | Autenticado |

## API REST

Documentada en Swagger UI. Flujo básico en PowerShell:

1. Obtener el token:

```powershell
curl.exe -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"dni\":\"00000000\",\"password\":\"admin123\"}'
```

Resultado esperado: respuesta JSON con el token JWT.

2. Usarlo en las peticiones:

```powershell
curl.exe http://localhost:8080/api/platos -H "Authorization: Bearer <token>"
```

Resultado esperado: lista JSON de platos (HTTP 200). Sin token responde 401.

Endpoints principales: `POST /api/auth/login`, `GET /api/auth/me`, `GET /api/platos` (filtro opcional `?categoria=`), `GET /api/platos/{id}`, `POST /api/platos`, `PUT /api/platos/{id}`, `DELETE /api/platos/{id}`. El resto de endpoints (`/api/publico`, `/api/cliente`, `/api/admin`, `/api/perfil`) está documentado en Swagger UI.

Los errores se devuelven como `{estado, mensaje, errores}` (400 validación, 401 credenciales, 409 regla de negocio).

## Reglas de negocio implementadas

- Un cliente solo puede tener **una reserva activa** (PENDIENTE o CONFIRMADA).
- Solo se reservan mesas **LIBRES**. El **plato principal es obligatorio**; postre y bebida son opcionales.
- Crear, cancelar y mover una reserva son operaciones transaccionales (rollback ante error). `Mesa` usa bloqueo optimista (`@Version`) para evitar que dos operaciones simultáneas confirmen la misma mesa.
- El **cliente** reserva y cancela su propia reserva. El **admin** lista y filtra reservas, las cancela y las mueve a otra mesa libre.
- El **admin** gestiona mesas, platos y usuarios. No se elimina una mesa con reservas, un plato usado en reservas, a sí mismo, a un usuario con reservas ni al último administrador.
- El **admin** consulta reportes (`/reportes`): reservas, mesas y platos por estado o categoría, top 5 de platos e ingreso estimado (sin contar reservas canceladas).
- Cualquier usuario autenticado puede ver su perfil y cambiar su contraseña en `/perfil`.

## Pruebas

```powershell
mvn test
```

Resultado esperado: `BUILD SUCCESS` con todas las pruebas en verde.

Usan JUnit 5, Mockito, MockMvc y H2 en memoria (`src/test/resources/application.properties`); no requieren MySQL. Cubren validaciones de reserva, cancelar/mover, consultas JPQL, rutas de seguridad, login JWT y el contrato HTTP de `/api/platos`.

## Estructura del repositorio

```
.
├── src/                      # Backend (paquete base com.restaurante.reservas)
├── frontend/                 # Frontend Angular (Dockerfile, nginx.conf, vercel.json)
├── k8s/                      # Manifiestos de Kubernetes (mysql.yaml, backend.yaml, frontend.yaml)
├── scripts/                  # Scripts auxiliares (cargar-imagenes.ps1)
├── .github/workflows/        # 6 workflows de GitHub Actions
├── Dockerfile                # Imagen del backend
├── docker-compose.yml        # MySQL + backend + frontend
└── pom.xml
```

Paquete base del backend `com.restaurante.reservas`:

- `model`: entidades JPA y enumerados
- `repository`: repositorios Spring Data (métodos derivados y JPQL)
- `service`: lógica de negocio y transacciones
- `controller`: controladores MVC (Thymeleaf)
- `controller.rest`: API REST y manejo de errores
- `dto`: objetos de transferencia
- `config`: seguridad, JWT, CORS, OpenAPI y datos iniciales

## Flujo de trabajo con Git y GitHub

### Ramas

| Rama | Uso |
| --- | --- |
| `main` | Rama principal |
| `dev` | Integración de los cambios de las ramas de trabajo |
| `feature/usuarios-seguridad` | Integrante 1: usuarios y seguridad |
| `feature/mesas` | Integrante 2: mesas |
| `feature/carta-platos-api` | Integrante 3: carta, platos y API |
| `feature/reservas` | Integrante 4: reservas |
| `feature/infraestructura-docs` | Integrante 5: infraestructura y documentación |

### Flujo

1. Cada integrante trabaja solo en su rama `feature/*`.
2. Antes de subir cambios, sincroniza tu rama con `dev` (ejemplo con la rama de mesas):

```powershell
git switch dev
git pull origin dev
git switch feature/mesas
git merge --no-edit dev
```

Resultado esperado: `dev` actualizada y tu rama con los cambios de `dev` integrados, sin conflictos pendientes.

3. Revisa, registra y sube tus cambios:

```powershell
git status
git add .
git commit -m "feat: descripción corta del cambio"
git push origin feature/mesas
```

Resultado esperado: `git status` lista los archivos modificados; el `push` termina sin errores y la rama aparece actualizada en GitHub. El `push` a una rama `feature/*` dispara su workflow de CI (ver [GitHub Actions](#github-actions)).

4. Abre un **Pull Request** en GitHub desde tu rama `feature/*` hacia `dev` (o `main`). Cada Pull Request hacia `dev` o `main` ejecuta los workflows de CI.
5. Si algún check falla, corrige en tu rama y vuelve a hacer `push`; el Pull Request se actualiza solo.
6. Cuando los checks están en verde, se revisa y se fusiona el Pull Request.

## GitHub Actions

Hay 6 workflows en `.github/workflows/`. Todos corren en `ubuntu-latest`. Los workflows de Java usan Java 17 (Temurin) con caché de Maven.

| Workflow (`name`) | Qué hace | Se ejecuta en |
| --- | --- | --- |
| CI Usuarios y Seguridad - Compilacion | Compila el código principal y el de pruebas (`mvn -B -ntp clean test-compile`) | Push a `feature/usuarios-seguridad` y `main`; PR hacia `dev` o `main`; manual |
| CI Mesas - Pruebas | Ejecuta todas las pruebas (`mvn -B -ntp clean test`, usan H2) y guarda los informes de `target/surefire-reports/` como artefacto `informes-pruebas-mesas` | Push a `feature/mesas` y `main`; PR hacia `dev` o `main`; manual |
| CI Carta y API - Calidad de codigo | Revisa el estilo con Checkstyle (reglas de Google). Es **informativo**: muestra advertencias y guarda `checkstyle-result.xml` como artefacto `informe-checkstyle`, pero no hace fallar el workflow | Push a `feature/carta-platos-api` y `main`; PR hacia `dev` o `main`; manual |
| CI Reservas - Empaquetado | Genera el JAR sin ejecutar pruebas (`mvn -B -ntp clean package -DskipTests`) y lo guarda como artefacto `reservas-jar` | Push a `feature/reservas` y `main`; PR hacia `dev` o `main`; manual |
| CI Infraestructura - Docker | Valida `docker-compose.yml`, construye las imágenes, levanta la aplicación con MySQL y espera hasta 40 intentos (cada 5 s) a que `http://localhost:8080/login` responda HTTP 200. Si falla, muestra los registros. Al final siempre ejecuta `docker compose down -v` | Push a `feature/infraestructura-docs` y `main`; PR hacia `dev` o `main`; manual |
| CI Frontend Angular | Instala dependencias con `npm ci` y compila con `npm run build` en `frontend/` (Node 20, caché de npm) | Push a `main` y `dev` con cambios en `frontend/**`; PR hacia `main` o `dev` |

Notas:

- Los artefactos se conservan 7 días.
- Los workflows de Java e infraestructura usan `concurrency`: una ejecución nueva sobre la misma referencia cancela la anterior.
- Las pruebas no se ejecutan al construir las imágenes Docker (`-DskipTests`); se ejecutan en GitHub Actions.

## Docker

### Archivos

| Archivo | Descripción |
| --- | --- |
| `Dockerfile` (raíz) | Imagen del backend en dos etapas: compila con `maven:3.9-eclipse-temurin-17` (`mvn clean package -DskipTests`) y ejecuta el JAR sobre `eclipse-temurin:17-jre` con un usuario no root (`spring`, uid 1001). Expone el 8080 |
| `frontend/Dockerfile` | Imagen del frontend en dos etapas: compila Angular con `node:22-alpine` (`npm run build`) y sirve `dist/frontend/browser` con `nginx:alpine`. Expone el 80 |
| `frontend/nginx.conf` | Sirve la aplicación Angular y redirige las rutas desconocidas a `/index.html` (`try_files`). **No** hay proxy hacia `/api` |
| `docker-compose.yml` | Define tres servicios: `mysql` (`mysql:8.0`, volumen `mysql_data`, healthcheck), `backend` (espera a que MySQL esté sano) y `frontend` |

En `docker-compose.yml`, el backend recibe `APP_CORS_ALLOWED_ORIGINS` con `http://localhost,http://localhost:4200`. El Angular llama a la API en `http://localhost:8080/api`.

> Las credenciales `root`/`root` de `docker-compose.yml` son solo para entorno local de pruebas. `JWT_SECRET` no se define en el Compose, por lo que se usa el valor por defecto de desarrollo.

### Levantar

Desde la raíz del repositorio:

```powershell
docker compose up --build
```

Resultado esperado: se construyen las imágenes y arrancan `restaurante-mysql`, `restaurante-backend` y `restaurante-frontend`. El backend queda en http://localhost:8080 y el frontend en http://localhost.

Para ejecutarlo en segundo plano y comprobar el estado:

```powershell
docker compose up -d --build
docker compose ps
```

Resultado esperado: los tres contenedores aparecen en estado `Up`; `restaurante-mysql` aparece como `healthy`.

Para ver las imágenes construidas:

```powershell
docker images
```

Resultado esperado: la lista incluye las imágenes del backend y del frontend.

### Apagar

```powershell
docker compose down
```

Resultado esperado: se detienen y eliminan los contenedores y la red. Los datos de MySQL se conservan en el volumen `mysql_data`.

```powershell
docker compose down -v
```

Resultado esperado: igual que el anterior, pero también borra el volumen `mysql_data` (se pierden los datos de MySQL).

> Apaga Docker Compose antes de pasar a Kubernetes para liberar el puerto 8080.

## Kubernetes (Docker Desktop)

Se despliega toda la aplicación (MySQL, backend y frontend) en el clúster de Kubernetes de Docker Desktop: 3 Deployments (3 pods) y 3 Services. Es un clúster tipo kind con un único nodo, `desktop-control-plane`.

### Manifiestos de `k8s/`

| Archivo | Recursos |
| --- | --- |
| `k8s/mysql.yaml` | `Deployment` `mysql-deployment` (1 réplica, `mysql:8.0`, base `restaurante_db`) y `Service` `mysql-service` (ClusterIP, 3306) |
| `k8s/backend.yaml` | `Deployment` `backend-deployment` (1 réplica, imagen `restaurante-backend:latest`) con un `initContainer` (`busybox:1.36`) que espera a `mysql-service:3306` antes de arrancar, y `Service` `backend-service` (ClusterIP, 8080). Define `APP_CORS_ALLOWED_ORIGINS=http://localhost:4200` |
| `k8s/frontend.yaml` | `Deployment` `frontend-deployment` (1 réplica, imagen `restaurante-frontend:latest`) y `Service` `frontend-service` (puerto 80) |

Los manifiestos usan `imagePullPolicy: Never`: Kubernetes no descarga las imágenes de ningún registro, solo usa las que ya existan en el nodo. Los manifiestos no definen un volumen persistente para MySQL (no hay PVC).

> Las credenciales `root`/`root` de `k8s/mysql.yaml` y `k8s/backend.yaml` son solo para entorno local de pruebas.

### Requisitos previos

1. En Docker Desktop: **Settings** → **Kubernetes** → marcar **Enable Kubernetes** → **Apply & Restart**, hasta que el indicador inferior muestre Kubernetes en ejecución (verde).
2. Docker Compose apagado (`docker compose down`).
3. Verificar el nodo:

```powershell
kubectl get nodes
```

Resultado esperado: un nodo `desktop-control-plane` en estado `Ready`.

### 1. Construir las imágenes

Desde la raíz del repositorio:

```powershell
docker build -t restaurante-backend:latest .
docker build -t restaurante-frontend:latest ./frontend
```

Resultado esperado: ambos builds terminan sin errores y las imágenes `restaurante-backend:latest` y `restaurante-frontend:latest` aparecen en `docker images`.

### 2. Importar las imágenes al nodo

El nodo de Kubernetes tiene su propio almacén de imágenes, distinto del de Docker. Con `imagePullPolicy: Never`, las imágenes construidas en el paso anterior deben importarse al nodo. Sin este paso, los pods quedan en `ErrImageNeverPull`.

```powershell
$NODO = kubectl get nodes -o jsonpath="{.items[0].metadata.name}"
```

Resultado esperado: sin salida. `$NODO` contiene `desktop-control-plane`.

```powershell
docker save -o backend.tar restaurante-backend:latest
docker save -o frontend.tar restaurante-frontend:latest
```

Resultado esperado: sin errores; se crean `backend.tar` y `frontend.tar` en el directorio actual.

```powershell
docker cp backend.tar ${NODO}:/backend.tar
docker cp frontend.tar ${NODO}:/frontend.tar
```

Resultado esperado: sin errores; los archivos `.tar` quedan copiados dentro del nodo.

```powershell
docker exec $NODO ctr -n k8s.io images import /backend.tar
docker exec $NODO ctr -n k8s.io images import /frontend.tar
```

Resultado esperado: sin mensajes de error; cada comando importa una imagen en el espacio de nombres `k8s.io` del nodo.

Para comprobar que el nodo tiene las imágenes:

```powershell
docker exec $NODO ctr -n k8s.io images ls | Select-String restaurante
```

Resultado esperado: aparecen `restaurante-backend:latest` y `restaurante-frontend:latest`.

El repositorio incluye `scripts/cargar-imagenes.ps1` para este paso. Los archivos `backend.tar` y `frontend.tar` generados se pueden borrar después de importar:

```powershell
Remove-Item backend.tar, frontend.tar
```

### 3. Desplegar

```powershell
kubectl apply -f k8s/
```

Resultado esperado: se informa la creación (`created`) de los tres `Deployment` y los tres `Service`.

```powershell
kubectl get pods -w
```

Resultado esperado: aparecen 3 pods (`mysql-deployment-…`, `backend-deployment-…`, `frontend-deployment-…`) que terminan en `1/1 Running`. El backend es el último porque su `initContainer` espera a MySQL (pasa por `Init:0/1`). Sal con Ctrl+C cuando los 3 estén en `1/1 Running`.

### 4. Acceder a la aplicación

En Docker Desktop el acceso se hace con `port-forward`. Ejecuta **cada comando en su propia terminal** y déjalos corriendo:

```powershell
kubectl port-forward svc/backend-service 8080:8080
```

```powershell
kubectl port-forward svc/frontend-service 4200:80
```

Resultado esperado: cada terminal muestra `Forwarding from 127.0.0.1:<puerto> -> <puerto>` y queda ocupada mientras dure el reenvío.

| Qué | URL |
| --- | --- |
| Frontend Angular | http://localhost:4200 (en `/mesas` se ven las mesas con datos) |
| Backend con Thymeleaf | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

**Por qué hacen falta los dos `port-forward` y el puerto 4200:** el backend solo acepta el origen definido en `APP_CORS_ALLOWED_ORIGINS` (`k8s/backend.yaml`: `http://localhost:4200`), y el frontend llama a la API en `http://localhost:8080/api`. Por eso el frontend debe abrirse en `http://localhost:4200` y el backend debe estar expuesto en el 8080 al mismo tiempo.

### 5. Verificar

```powershell
kubectl get pods
```

Resultado esperado: los 3 pods en `1/1 Running`.

```powershell
kubectl get svc
```

Resultado esperado: aparecen `backend-service`, `frontend-service` y `mysql-service`.

```powershell
kubectl get pvc
```

Resultado esperado: sin recursos (`No resources found`), porque los manifiestos no definen ningún PVC.

```powershell
kubectl exec deploy/backend-deployment -- printenv APP_CORS_ALLOWED_ORIGINS
```

Resultado esperado: `http://localhost:4200`.

### 6. Limpiar

1. Detén los dos `port-forward` con Ctrl+C en cada terminal.
2. Elimina los recursos:

```powershell
kubectl delete -f k8s/
```

Resultado esperado: se informa la eliminación (`deleted`) de los tres `Deployment` y los tres `Service`.

```powershell
kubectl get pods
```

Resultado esperado: `No resources found` (los pods pueden tardar unos segundos en pasar por `Terminating`).

## Despliegue web online (AWS EC2)

El frontend está publicado en AWS EC2 y se accede desde varios computadores del equipo:

- URL: http://13.184.109.142 (por ejemplo, http://13.184.109.142/mesas)

La IP pública puede cambiar si se detiene y se vuelve a iniciar la instancia.

### Instancia

| Parámetro | Valor |
| --- | --- |
| Nombre | `restaurante-fullstack-ec2` |
| Sistema operativo | Ubuntu Server 22.04 LTS (64-bit x86) |
| Tipo de instancia | `t3.small` |
| Almacenamiento raíz | 16 GB gp3 |
| Acceso | Par de claves SSH (`restaurante-key.pem`) |

### Security Group

| Puerto | Origen | Uso |
| --- | --- | --- |
| 22 | 0.0.0.0/0 | SSH |
| 80 | 0.0.0.0/0 | Frontend |
| 8080 | 0.0.0.0/0 | Backend |

> Esta apertura a `0.0.0.0/0` es solo para la demostración académica. No es una configuración adecuada para un entorno real.

### Pasos del despliegue

1. Crear la instancia en la consola de AWS (**EC2** → **Launch Instance**) con los parámetros de la tabla anterior y las tres reglas de entrada del Security Group.
2. Conectarse por SSH (desde PowerShell, en la carpeta de la clave):

```powershell
ssh -i "restaurante-key.pem" ubuntu@<IP-PUBLICA-EC2>
```

Resultado esperado: sesión abierta en la instancia Ubuntu.

3. Instalar Docker, Docker Compose y Git en la instancia (comandos de Linux, ejecutados dentro de la instancia):

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu
newgrp docker
```

Resultado esperado: paquetes instalados sin errores; el usuario `ubuntu` puede ejecutar `docker` sin `sudo`.

4. Clonar el repositorio y levantar la aplicación:

```bash
git clone <URL-DEL-REPOSITORIO>
cd <carpeta-del-repositorio>
docker compose up -d --build
docker compose ps
```

Resultado esperado: se construyen las imágenes y `restaurante-mysql`, `restaurante-backend` y `restaurante-frontend` quedan en estado `Up`.

5. Abrir `http://<IP-PUBLICA-EC2>` en el navegador (la IP está en el panel de EC2).

### Conexión del frontend con la API

El despliegue usa los archivos del repositorio tal como están: `api.config.ts` define `http://localhost:8080/api`, `nginx.conf` solo sirve Angular (sin proxy hacia `/api`) y el CORS de `docker-compose.yml` permite `http://localhost` y `http://localhost:4200`. El frontend publicado se verificó accesible desde varios computadores del equipo en http://13.184.109.142.

## Solución de problemas

| Síntoma | Causa | Solución |
| --- | --- | --- |
| Pods en `ErrImageNeverPull` | Las imágenes no están importadas en el nodo (`imagePullPolicy: Never`) | Repetir el [paso 2](#2-importar-las-imágenes-al-nodo) (o ejecutar `scripts/cargar-imagenes.ps1`) y luego: `kubectl rollout restart deployment/backend-deployment deployment/frontend-deployment` |
| El frontend carga pero no muestra datos; error CORS en la consola del navegador | El backend solo acepta el origen de `APP_CORS_ALLOWED_ORIGINS` (`http://localhost:4200` en Kubernetes) | Abrir el frontend en http://localhost:4200 y mantener el `port-forward` del backend en el 8080 |
| `port-forward` falla al iniciar | El puerto local está ocupado (por ejemplo, el 8080 por Docker Compose) | Apagar lo que use el puerto (`docker compose down`) o cerrar el proceso que lo ocupa, y relanzar el `port-forward` |
| El `port-forward` se corta | El pod fue reemplazado (por ejemplo, tras `rollout restart`) | Volver a ejecutar el `port-forward` |
| El backend tarda en quedar `Running` | El `initContainer` espera a que MySQL acepte conexiones en `mysql-service:3306` | Esperar; verificar con `kubectl get pods -w` |
| `docker compose up` falla porque el puerto 8080 está ocupado | Otro proceso (por ejemplo, el backend local o un `port-forward` de Kubernetes) usa el 8080 | Detener ese proceso y repetir |
| En EC2 la IP pública cambió | Se detuvo y se inició la instancia | Copiar la nueva IP pública desde el panel de EC2 y usarla en la URL |

## Limitaciones

Sin pasarela de pagos, sin notificaciones en tiempo real y sin pruebas de alta concurrencia. La reserva no incluye fecha ni hora elegidas por el cliente: la mesa pasa de LIBRE a RESERVADA hasta que se cancele. En Kubernetes, MySQL no usa volumen persistente, por lo que sus datos no se conservan al eliminar los recursos. El despliegue en AWS EC2 es una demostración académica. 