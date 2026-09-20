# Java 25 in both stages, matching the JDK the service is developed against.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Resolve dependencies against the POM alone first, so editing source does not
# re-download the world on every rebuild.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/storage-*.jar app.jar

# Uploaded PDFs live here; mount a volume over it or they die with the container.
ENV FILE_STORAGE_PATH=/app/files
RUN mkdir -p /app/files

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
