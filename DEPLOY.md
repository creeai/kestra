# Como subir esta aplicação (creeai/kestra)

Repositório: **https://github.com/creeai/kestra**

**Deploy em produção (VPS):** ver **[DEPLOY-VPS.md](./DEPLOY-VPS.md)** — clone do repo, `docker compose build kestra`, `docker compose up -d`. O repositório inclui o `Dockerfile` na raiz e toda a estrutura necessária.

Comandos para subir **igual ao Kestra original** (só ficheiro, sem clone): descarregar `docker-compose.yml` e correr.

O repositório inclui um **Dockerfile** na raiz que constrói a **aplicação modificada** (RBAC, Audit Logs, Secrets UI, etc.). O `docker-compose.yml` está configurado para usar esse build por defeito (`build: .` + `image: jhonatancreeai/kestra:latest`). Para usar só a imagem oficial sem construir, altere o compose para `image: kestra/kestra:latest` e remova ou comente a secção `build:` — ver **«Usar a imagem do fork»**.

---

## 1. Subir com Docker Compose (recomendado)

### Linux / macOS

**Descarregar o Docker Compose:**

```bash
curl -o docker-compose.yml \
  https://raw.githubusercontent.com/creeai/kestra/main/docker-compose.yml
```

**Subir a aplicação:**

```bash
docker compose up -d
```

**Verificar:**

```bash
docker compose ps
```

Aceder à UI: **http://localhost:8080** (ou http://\<IP-do-servidor\>:8080)

---

### Windows (PowerShell)

**Descarregar o Docker Compose:**

```powershell
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/creeai/kestra/main/docker-compose.yml" -OutFile "docker-compose.yml"
```

**Subir a aplicação:**

```powershell
docker compose up -d
```

**Verificar:**

```powershell
docker compose ps
```

Aceder à UI: **http://localhost:8080**

---

### Subir na VPS com a aplicação modificada (Dockerfile na raiz)

Na VPS, com o repositório clonado (ou com o `Dockerfile` e o `docker-compose.yml` na mesma pasta que a pasta `kestra/`):

```bash
# Clonar (ou já ter o repo)
git clone --depth 1 --branch main https://github.com/creeai/kestra.git ~/kestra-app
cd ~/kestra-app

# Construir a imagem e subir (Postgres, Redis, MinIO, Kestra modificado)
docker compose build kestra
docker compose up -d
```

O **Dockerfile** na raiz faz o build do código em `kestra/` (Gradle + JAR) e produz a imagem. O primeiro build pode demorar vários minutos. Para só levantar os serviços sem reconstruir: `docker compose up -d`.

---

### Usar a imagem do fork (creeai/kestra:local)

Para correr a **versão modificada** (RBAC, Audit Logs, Secrets, etc.) em vez da imagem oficial:

**No servidor (Linux) – construir a imagem e subir:**

```bash
git clone --depth 1 --branch main https://github.com/creeai/kestra.git /tmp/creeai-kestra
cd /tmp/creeai-kestra/kestra
./build-docker-with-plugins.sh creeai/kestra:local
cd -
curl -o docker-compose.yml https://raw.githubusercontent.com/creeai/kestra/main/docker-compose.yml
sed -i 's|image: kestra/kestra:latest|image: creeai/kestra:local|' docker-compose.yml
sed -i 's|pull_policy: always|pull_policy: if_not_present|' docker-compose.yml
docker compose up -d
```

**No Windows – construir a imagem:**

```powershell
cd C:\Users\micha\Desktop\Kestra\kestra
.\build-docker-with-plugins.ps1
docker tag kestra/kestra:1.3.0-with-plugins creeai/kestra:local
```

No servidor: descarregar o compose, alterar para `image: creeai/kestra:local` e `pull_policy: if_not_present`, fazer push da imagem para um registry acessível ao servidor (ou construir a imagem no servidor com os comandos Linux acima) e depois `docker compose up -d`.

---

### Publicar a imagem no Docker Hub

Para publicar a versão do fork (RBAC, Audit Logs, Secrets, etc.) no Docker Hub, na **VPS ou na tua máquina** (onde a imagem `creeai/kestra:local` já tiver sido construída):

**Repositório creeai/kestra:**
```bash
docker tag creeai/kestra:local creeai/kestra:latest
docker login
docker push creeai/kestra:latest
```

**Repositório jhonatancreeai/kestra (Docker Hub pessoal):**
```bash
docker tag creeai/kestra:local jhonatancreeai/kestra:latest
docker login
docker push jhonatancreeai/kestra:latest
```

- **`docker login`** – pede username e password do Docker Hub; faz login no registry.
- **`docker push`** – envia a imagem para hub.docker.com (ex.: hub.docker.com/r/jhonatancreeai/kestra).

Depois disso, em qualquer máquina podes usar `image: jhonatancreeai/kestra:latest` (ou `creeai/kestra:latest`) no compose com `pull_policy: always` ou `if_not_present`.

---

## 2. Subir com Docker Swarm (stack)

**Descarregar a stack**

Linux/macOS:

```bash
curl -o stack-complete.yml \
  https://raw.githubusercontent.com/creeai/kestra/main/infra/stack-complete.yml
```

Windows (PowerShell):

```powershell
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/creeai/kestra/main/infra/stack-complete.yml" -OutFile "stack-complete.yml"
```

**Inicializar o Swarm** (obrigatório na primeira vez; se tiver mais de um IP use `--advertise-addr <IP>`):

```bash
docker swarm init --advertise-addr <IP_DO_MANAGER>
```

**Fazer deploy:**

```bash
docker stack deploy -c stack-complete.yml kestra
```

**Verificar:**

```bash
docker stack services kestra
```

Aceder à UI: **http://\<IP-do-manager\>:8080**  
MinIO Console: **http://\<IP-do-manager\>:9001**

**Parar a stack:**

```bash
docker stack rm kestra
```

---

## 3. Subir com um único container (docker run)

Modo rápido, só Kestra (sem Postgres/Redis no compose). Usa H2 em modo local.

**Linux / macOS:**

```bash
docker run --pull=always -it -p 8080:8080 --user=root \
  --name kestra --restart=always \
  -v kestra_data:/app/storage \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /tmp:/tmp \
  kestra/kestra:latest server local
```

**Windows (PowerShell):**

```powershell
docker run --pull=always -it -p 8080:8080 --user=root `
  --name kestra --restart=always `
  -v "kestra_data:/app/storage" `
  -v "/var/run/docker.sock:/var/run/docker.sock" `
  -v "C:/Temp:/tmp" `
  kestra/kestra:latest server local
```

Para usar a **imagem do fork** (depois de a construíres): substituir `kestra/kestra:latest` por `creeai/kestra:local`.

Aceder à UI: **http://localhost:8080**

---

## 4. Parar a aplicação

**Docker Compose:**

```bash
docker compose down
```

**Docker Swarm:**

```bash
docker stack rm kestra
```

**Container único (docker run):**

```bash
docker stop kestra
docker rm kestra
```

---

## 5. O que sobe em cada modo

| Modo | Postgres | Redis | MinIO | Kestra |
|------|----------|-------|-------|--------|
| **Docker Compose** | ✅ | ✅ | ✅ | ✅ |
| **Docker Swarm (stack-complete.yml)** | ✅ | ✅ | ✅ | ✅ |
| **docker run (server local)** | ❌ (usa H2) | ❌ | ❌ | ✅ |

---

## 6. Referência: Kestra original

- Docker Compose oficial: `https://raw.githubusercontent.com/kestra-io/kestra/develop/docker-compose.yml`
- Instalação oficial: https://kestra.io/docs/installation

Este fork mantém a mesma lógica de deploy; os ficheiros estão em **https://github.com/creeai/kestra**.
