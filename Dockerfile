# syntax=docker/dockerfile:1

# ---- build -------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependências primeiro: esta camada só é refeita quando o pom.xml muda.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src src
# Os testes rodam no CI; a imagem só empacota.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# ---- runtime -----------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system unnia && useradd --system --gid unnia --no-create-home unnia
COPY --from=build /workspace/target/*.jar app.jar
USER unnia

EXPOSE 8080
# MaxRAMPercentage faz a JVM respeitar o limite de memória do contêiner.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
