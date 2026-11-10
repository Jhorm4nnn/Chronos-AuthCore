# Etapa 1: compila con el wrapper de Gradle (la imagen ya trae el JDK 21 que pide la toolchain).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# Etapa 2: solo el JRE y el jar, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 authcore
COPY --from=build /app/build/libs/*.jar app.jar
USER authcore
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
