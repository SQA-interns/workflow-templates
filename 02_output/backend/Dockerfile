# Backend image (architecture.md deploys_as; ADR-11). Build the jar first: ./mvnw -B -DskipTests package
FROM eclipse-temurin:21.0.13_9-jre
# F-35: the base image's pebble service manager (Go stdlib with High advisories) is never started
RUN rm -f /usr/bin/pebble \
 && groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY target/contacts-backend.jar /app/app.jar
USER app
EXPOSE 8080
# ES-09: health from the actuator endpoint (bash /dev/tcp; the image has no curl)
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=4 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
