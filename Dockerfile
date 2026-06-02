FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/libs/*[!p][!l][!a][!i][!n].jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]