# --- Build stage --------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Dependencies first, cached separately from source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B clean package -DskipTests

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
