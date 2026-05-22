# =============================================================================
# Etapa de build
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# =============================================================================
# Etapa de execucao
# JRE Alpine: ~100MB vs ~400MB do JDK completo.
# Imagem menor = pull mais rapido no cold start do Render.
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Flags JVM para container com pouca RAM (Render Free ~512MB):
#
#   -XX:+UseSerialGC
#     GC simples e de baixo overhead. Adequado para app de baixo throughput
#     com uma unica instancia. G1GC (default) e otimizado para alta concorrencia
#     e desperdicaria memoria no Render Free.
#
#   -Xms64m -Xmx256m
#     Limita o heap. Sem limite, a JVM pode tentar alocar mais do que o Render
#     oferece e ser morta pelo OOM killer do container.
#
#   -XX:TieredStopAtLevel=1
#     Desativa o compilador JIT C2 (nivel 4). O C2 faz otimizacoes pesadas
#     que so valem a pena em apps de longa duracao com alto throughput.
#     Para TCC no Render Free, o custo de compilacao nao compensa.
#     Resultado: startup ~30% mais rapido.
#
#   -Dspring.profiles.active=production
#     Hardcoded aqui para garantir que o profile correto seja sempre ativado,
#     independente de env vars do Render.
#     NOTA: ${VAR:-default} nao funciona em CMD array JSON do Docker.
#     Por isso o valor e literal. O Render tambem injeta SPRING_PROFILES_ACTIVE
#     via env var, mas esta flag garante o fallback correto.
CMD ["java", "-XX:+UseSerialGC", "-Xms64m", "-Xmx256m", "-XX:TieredStopAtLevel=1", "-Dspring.profiles.active=production", "-jar", "app.jar"]
