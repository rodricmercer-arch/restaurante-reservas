# ---------- Etapa 1: compilar el proyecto con Maven y Java 17 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Las pruebas se ejecutan en GitHub Actions, no durante la construccion de la imagen
RUN mvn -B -ntp clean package -DskipTests

# ---------- Etapa 2: imagen final liviana, solo con Java 17 (JRE) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
