# syntax=docker/dockerfile:1

# --- Construcción ---
FROM eclipse-temurin:21-jdk AS construccion
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY config/ config/
COPY src/ src/
# Las pruebas y el análisis corren en la CI (./mvnw verify); aquí solo se empaqueta.
RUN ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true -Djacoco.skip=true

# --- Ejecución ---
FROM eclipse-temurin:21-jre
RUN groupadd --system italarm && useradd --system --gid italarm --uid 10001 italarm
WORKDIR /app
COPY --from=construccion /app/target/italarm-api.jar app.jar
USER italarm

# El perfil (staging o prod) y los secretos los define el proveedor de hosting.
# Memoria ajustada al plan gratis de Render (512 MB, P-04): montón al 50 %, recolector Serial (un
# solo núcleo), solo el compilador C1, pila de hilos de 512 KB y metaspace acotado. Medido con los
# datos de volumen: máximo 410 MB de memoria real (plan de la Fase 7, sección 9).
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:MaxMetaspaceSize=150m -XX:ReservedCodeCacheSize=32m -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
