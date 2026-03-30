# ---- Stage 1 : Build avec Maven ----
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copie du wrapper et du pom en premier pour le cache des dépendances
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -q

# Compilation du projet (sans tests — ceux-ci tournent en CI)
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ---- Stage 2 : Image de runtime minimale ----
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
