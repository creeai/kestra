# Deploy (fork `creeai/kestra`)

Este repositório é um fork do Kestra com funcionalidades OSS adicionais (RBAC/IAM, Audit Logs, Secrets UI, etc.).  
Repo: `https://github.com/creeai/kestra`

## O que o Kestra oficial oferece (referência)

- **Docker Compose (Postgres + Kestra)**: `https://raw.githubusercontent.com/kestra-io/kestra/develop/docker-compose.yml`
- **Docker run (local/rápido)**: instruções no README oficial `https://raw.githubusercontent.com/kestra-io/kestra/develop/README.md`
- **Templates cloud**:
  - AWS CloudFormation (link no README oficial)
  - GCP Terraform module: `https://github.com/kestra-io/deployment-templates`
- **Guia de instalação**: `https://kestra.io/docs/installation`

## Equivalente para este fork (o que usar em produção)

### Opção A (recomendada): Docker Compose com a tua imagem

1) **Descarregar o compose deste fork** (igual ao comando do Kestra oficial):

```bash
curl -o docker-compose.yml \
  https://raw.githubusercontent.com/creeai/kestra/main/docker-compose.yml
```

2) **Editar a imagem** do serviço `kestra` para apontar para a tua build:

- Se builds localmente no servidor:
  - `image: creeai/kestra:local`
- Se publicas no GHCR:
  - `image: ghcr.io/creeai/kestra:latest-with-plugins` (exemplo)

3) **Subir**:

```bash
docker compose up -d
docker compose ps
```

### Opção B: Docker Swarm (stack)

1) **Descarregar a stack**:

```bash
curl -o stack-complete.yml \
  https://raw.githubusercontent.com/creeai/kestra/main/infra/stack-complete.yml
```

2) **Inicializar Swarm** (se tiver mais de um IP, escolhe um):

```bash
docker swarm init --advertise-addr <IP_DO_MANAGER>
```

3) **Deploy**:

```bash
docker stack deploy -c stack-complete.yml kestra
docker stack services kestra
```

> Nota: não corras `docker compose up` ao mesmo tempo no mesmo host — vais ter conflito de portas (8080/8081).

## Como gerar a imagem do fork (com plugins)

No código do Kestra (pasta `kestra/`), já existe script para buildar a imagem com plugins open-source instalados:

```bash
cd kestra
./build-docker-with-plugins.sh creeai/kestra:local
```

No Windows:

```powershell
cd c:\Users\micha\Desktop\Kestra\kestra
.\build-docker-with-plugins.ps1
```

O script:

- Compila o JAR executável
- Monta `docker/app/kestra`
- Faz `docker build` usando `kestra/Dockerfile`
- (Opcional) instala plugins OSS dentro da imagem via `kestra plugins install`

## Segurança (importante)

- **Não commits** chaves (OpenAI, Slack webhooks, tokens). O GitHub pode bloquear push (push protection).
- Para API keys, prefere:
  - `docker compose` + ficheiro local não versionado, ou
  - Docker secrets, ou
  - variáveis no ambiente do host (sem commitar no repo).

