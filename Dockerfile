FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package dependency:copy-dependencies -DincludeScope=runtime

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/classes /app/libs /app/config

COPY --from=build /build/target/classes /app/classes
COPY --from=build /build/target/dependency /app/libs

CMD ["java", "-cp", "/app/config:/app/classes:/app/libs/*", "com.litovskiy.Main"]
