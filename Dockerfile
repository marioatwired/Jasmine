# syntax=docker/dockerfile:1.12@sha256:93bfd3b68c109427185cd78b4779fc82b484b0b7618e36d0f104d4d801e66d25

# Multi-stage build for Jasmine.
# Base images are pinned to immutable digests. Dependabot updates them.
# Stage 1: Build the application
FROM maven:3.9.12-eclipse-temurin-25-alpine@sha256:91fbbf32cc5076b6142ed98405f3a1a21e102d876eafa5eaf359b5591e8e3fd8 AS builder

ARG BUILD_TIMESTAMP
ENV BUILD_TIMESTAMP=$BUILD_TIMESTAMP

WORKDIR /build

# Copy pom.xml first for better layer caching
COPY --link pom.xml .

# Download dependencies with BuildKit cache mount for Maven repository
# This significantly speeds up builds by persisting dependencies between builds
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:resolve dependency:resolve-plugins -B -Pdocker

# Copy source code
COPY --link src ./src

# Build the application with BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2/repository \
    if [ -n "$BUILD_TIMESTAMP" ]; then \
      mvn clean package -DskipTests -B -Pdocker -Dproject.build.outputTimestamp="$BUILD_TIMESTAMP"; \
    else \
      mvn clean package -DskipTests -B -Pdocker; \
    fi


# Stage 2: Create custom minimal JRE using jlink
# Using noble (Ubuntu 24.04) for glibc 2.39 compatibility with native libraries
FROM eclipse-temurin:25-jdk-noble@sha256:95e350ad1eb738f943e141640f7e3d797e483359a3b33c3864255bfd1ad71f8a AS jre-builder

# Create a minimal JRE with only the modules Jasmine needs
# Modules determined by: jdeps --print-module-deps --ignore-missing-deps <jar>
# Plus runtime-loaded modules that jdeps can't detect:
#   java.base         - Core Java classes
#   java.compiler     - Annotation processing (used by some libraries)
#   java.desktop      - GUI support (Swing/AWT, even for headless mode)
#   java.logging      - SLF4J/Logback logging
#   java.naming       - JNDI (required by Logback)
#   java.net.http     - HTTP client API
#   java.scripting    - ScriptEngine for EvalCmd (Rhino)
#   java.security.jgss - Kerberos/GSS-API security
#   java.sql          - JDBC (used by some dependencies)
#   jdk.crypto.ec     - Elliptic curve crypto for TLS/SSL (Discord API) - runtime loaded
#   jdk.management    - JMX management extensions
#   jdk.unsupported   - Native library access (jdave, udpqueue)
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.compiler,java.desktop,java.logging,java.naming,java.net.http,java.scripting,java.security.jgss,java.sql,jdk.crypto.ec,jdk.management,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=zip-6 \
    --output /javaruntime


# Stage 3: Runtime image
# Using Bitnami minideb:trixie (Debian 13) for minimal size with glibc 2.41
# Required for jdave/udpqueue native libraries which need glibc >= 2.38
FROM bitnami/minideb:trixie@sha256:7e02677d255280f9bcdf4fe057f0a17abbc6e55ba33c9ac5e599fba425dc51ff

# OCI image labels for better traceability and management
LABEL org.opencontainers.image.title="Jasmine" \
      org.opencontainers.image.description="A cross-platform Discord music bot with a clean interface" \
      org.opencontainers.image.url="https://github.com/marioatwired/Jasmine" \
      org.opencontainers.image.source="https://github.com/marioatwired/Jasmine" \
      org.opencontainers.image.vendor="Jasmine" \
      org.opencontainers.image.licenses="Apache-2.0"

# Copy custom JRE from jre-builder stage
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-builder /javaruntime $JAVA_HOME

# Create non-root user and application directories in single layer
RUN groupadd --gid 10001 jasmine && \
    useradd --uid 10001 --gid 10001 --shell /bin/false jasmine && \
    mkdir -p /app /musicbot && \
    chown -R jasmine:jasmine /app /musicbot

# Copy the built JAR from builder stage
COPY --from=builder --chown=jasmine:jasmine /build/target/jasmine-*-All.jar /app/app.jar

# Copy and set permissions for entrypoint script
COPY --chown=jasmine:jasmine --chmod=755 docker/entrypoint.sh /app/entrypoint.sh

WORKDIR /musicbot

# Switch to non-root user for security
USER jasmine

ENTRYPOINT ["/app/entrypoint.sh"]
