# Build
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests

# Runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /app/target/sistema-estoque-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
