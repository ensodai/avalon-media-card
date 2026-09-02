# ==============================================================================
# 🌿 Avalon Media Server - Production Dockerfile
# ==============================================================================

FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="Avalon MediaCard Server" \
      org.opencontainers.image.description="Next-Generation Cross-Platform Media Center, Streaming Aggregator & SDUI Server" \
      org.opencontainers.image.url="https://github.com/ensodai/avalon-media-card" \
      org.opencontainers.image.source="https://github.com/ensodai/avalon-media-card"

WORKDIR /app

# Install runtime utilities
RUN apk add --no-cache bash curl tzdata && \
    mkdir -p /app/data /app/plugins /app/web /app/default-plugins

# Copy application artifacts from pre-built context
COPY build/docker-dist/avalon-server.jar /app/avalon-server.jar
COPY build/docker-dist/web/ /app/web/
COPY build/docker-dist/plugins/ /app/default-plugins/
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh

# Environment variables with sane defaults
ENV PORT=8080 \
    DB_URL=jdbc:sqlite:/app/data/avalon.db \
    WEB_DIR=/app/web \
    ADMIN_USERNAME=admin \
    ADMIN_PASSWORD=admin \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# Expose HTTP / WebSocket port
EXPOSE 8080

# Expose persistent data and plugins volume mountpoints
VOLUME ["/app/data", "/app/plugins"]

# Healthcheck to verify the server is responding
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:${PORT}/ || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar /app/avalon-server.jar"]
