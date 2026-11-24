# Etapa 1: Build da aplicação com Maven e Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copia os arquivos de configuração do Maven e o código-fonte
COPY pom.xml .
COPY src ./src

# Executa o build pulando os testes de integração durante a geração da imagem
RUN mvn clean package -DskipTests

# Etapa 2: Execução com JRE 17 enxuto e Healthcheck UptimeRobot a cada 30s
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instala curl para o Healthcheck de 30s do Docker / UptimeRobot
RUN apk add --no-cache curl

# Copia o JAR gerado na etapa de build
COPY --from=builder /app/target/gearRent-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Healthcheck nativo do Docker a cada 30 segundos (registra UP no log interno do Spring Boot)
HEALTHCHECK --interval=30s --timeout=10s --start-period=25s --retries=3 \
  CMD curl -fsS http://localhost:8080/api/health | grep -q '"status":"UP"' || exit 1

# Comando para inicializar o back-end
ENTRYPOINT ["java", "-jar", "app.jar"]
