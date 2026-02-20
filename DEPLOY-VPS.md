# Deploy em produção na VPS (passo a passo)

Este documento descreve **exatamente** como colocar a aplicação Kestra (fork creeai) em produção na tua VPS usando o repositório GitHub.

**Repositório:** https://github.com/creeai/kestra

---

## Pré-requisitos na VPS

- Docker e Docker Compose instalados
- Porta 8080 (e opcionalmente 9000, 9001) livre
- Acesso SSH à VPS

---

## Comandos (copiar e colar)

Executa na VPS, na ordem.

### 1. Clonar o repositório

```bash
git clone --depth 1 --branch main https://github.com/creeai/kestra.git /opt/kestra
cd /opt/kestra
```

### 2. Construir a imagem da aplicação

```bash
docker compose build --no-cache kestra
```

(O primeiro build demora vários minutos: compila o código com Gradle dentro do Docker.)

### 3. Subir todos os serviços

```bash
docker compose up -d
```

### 4. Verificar

```bash
docker compose ps
curl -I http://127.0.0.1:8080
```

Todos os serviços (postgres, redis, minio, kestra) devem estar **Up**. O `curl` deve devolver `HTTP/1.1 200` ou `302` quando o Kestra estiver pronto (pode demorar ~1 minuto no primeiro arranque).

### 5. Aceder à aplicação

No browser: **http://\<IP-DA-TUA-VPS\>:8080**

---

## Parar a aplicação

```bash
cd /opt/kestra
docker compose down
```

---

## Atualizar (depois de alterações no GitHub)

```bash
cd /opt/kestra
git pull origin main
docker compose build --no-cache kestra
docker compose up -d --force-recreate kestra
```

---

## Ficheiros do repositório usados no deploy

| Ficheiro / pasta     | Uso |
|----------------------|-----|
| `Dockerfile`         | Build da imagem da aplicação (multi-stage: compila + runtime). |
| `docker-compose.yml` | Define postgres, redis, minio e kestra; o serviço `kestra` usa `build: context: . dockerfile: Dockerfile`. |
| `kestra/`            | Código fonte; o Dockerfile copia esta pasta e compila. |
| `kestra/docker/usr/local/bin/docker-entrypoint.sh` | Script de arranque do contentor. |

Não é necessário criar ficheiros à mão na VPS. O clone do repositório contém tudo.
