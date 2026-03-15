FROM eclipse-temurin:11-jdk AS builder
WORKDIR /build
COPY VulnApp.java .
RUN javac VulnApp.java

FROM debian:bullseye-slim
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    default-jre-headless \
    python3 \
    libcap2-bin \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m appuser

RUN mkdir -p /opt/sys-tools && \
    cp /usr/bin/python3 /opt/sys-tools/python3 && \
    setcap cap_setuid+ep /opt/sys-tools/python3

COPY --from=builder /build/VulnApp*.class /app/

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]