# ============================================
# Stage 1: Build (used by prod target)
# ============================================
FROM amazoncorretto:21-alpine AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ============================================
# Stage 2: Dev (docker compose target)
# ============================================
FROM amazoncorretto:21-alpine AS dev

RUN apk add --no-cache wget bash

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/

EXPOSE 8080 5005

CMD ["./mvnw", "spring-boot:run", \
     "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]

# ============================================
# Stage 3: Production
# ============================================
FROM amazoncorretto:21-alpine AS prod

RUN apk add --no-cache wget
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /app/target/keybudget-backend-*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
