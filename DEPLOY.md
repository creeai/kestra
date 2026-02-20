# Como subir esta aplicação (creeai/kestra)

Comandos para subir a aplicação **igual ao Kestra original**: descarregar ficheiro e correr.  
Repositório: **https://github.com/creeai/kestra**

O `docker-compose.yml` do repositório usa por defeito **kestra/kestra:latest**, para que **os dois comandos abaixo funcionem logo** (sem precisar de construir imagem). Para usar a **versão do fork** (RBAC, Audit Logs, Secrets UI, etc.), construa a imagem `creeai/kestra:local` e altere no compose a linha da imagem — ver secção **«Usar a imagem do fork»**.

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
