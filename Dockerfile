# Dockerfile da aplicação Kestra modificada (creeai/kestra)
# Build: docker compose build kestra   ou   docker build -t jhonatancreeai/kestra:latest .
# Uso na VPS: docker compose up -d (com build ou image conforme DEPLOY.md)

# -----------------------------------------------------------------------------
# Stage 1: construir o JAR executável (aplicação modificada)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /workspace

COPY kestra /workspace

# .git ausente no archive do GitHub (ex.: EasyPanel); criar mínimo para o Gradle generateGitProperties
RUN mkdir -p /workspace/.git/refs/heads && \
    echo "ref: refs/heads/main" > /workspace/.git/HEAD && \
    echo "0000000000000000000000000000000000000000" > /workspace/.git/refs/heads/main

RUN chmod +x gradlew && \
    ./gradlew executableJar --no-daemon

# -----------------------------------------------------------------------------
# Stage 2: imagem final para executar na VPS
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-jammy

ARG KESTRA_PLUGINS=""
ARG APT_PACKAGES="python3 python-is-python3 python3-pip curl jattach"
ARG PYTHON_LIBRARIES="kestra"

WORKDIR /app

RUN groupadd kestra && \
    useradd -m -g kestra kestra

# Binário da aplicação (fork com RBAC, Audit Logs, Secrets, etc.)
COPY --from=builder /workspace/build/executable/ /app/
RUN mv /app/kestra-* /app/kestra 2>/dev/null || true && \
    chmod +x /app/kestra

# Entrypoint
COPY kestra/docker/usr /usr

RUN apt-get update -y && \
    apt-get upgrade -y && \
    apt-get install curl -y && \
    if [ -n "${APT_PACKAGES}" ]; then apt-get install -y --no-install-recommends ${APT_PACKAGES}; fi && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/tmp/* /tmp/* && \
    curl -LsSf https://astral.sh/uv/0.6.17/install.sh | sh && mv /root/.local/bin/uv /bin && mv /root/.local/bin/uvx /bin && \
    if [ -n "${KESTRA_PLUGINS}" ]; then /app/kestra plugins install ${KESTRA_PLUGINS} && rm -rf /tmp/*; fi && \
    if [ -n "${PYTHON_LIBRARIES}" ]; then uv pip install --system ${PYTHON_LIBRARIES}; fi && \
    chown -R kestra:kestra /app

USER kestra

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["--help"]
