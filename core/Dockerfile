# syntax=docker/dockerfile:1

# ---------- Сборка ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Сначала зависимости: слой кэшируется, пока не изменился pom.xml
COPY .mvn .mvn
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/core.jar extract --layers --destination target/extracted

# ---------- Запуск ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app

# Слои от редко меняющихся к часто меняющимся
COPY --from=build /build/target/extracted/dependencies/ ./
COPY --from=build /build/target/extracted/spring-boot-loader/ ./
COPY --from=build /build/target/extracted/snapshot-dependencies/ ./
COPY --from=build /build/target/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "core.jar"]
