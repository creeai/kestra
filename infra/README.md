# Infra – Stacks e scripts

Stacks Docker Swarm e scripts para deploy da aplicação [creeai/kestra](https://github.com/creeai/kestra).

## Subir com Docker Swarm

```bash
curl -o stack-complete.yml https://raw.githubusercontent.com/creeai/kestra/main/infra/stack-complete.yml
docker swarm init --advertise-addr <IP_DO_MANAGER>
docker stack deploy -c stack-complete.yml kestra
```

Ver: **[DEPLOY.md](../DEPLOY.md)** (secção Docker Swarm).

## Ficheiros

| Ficheiro | Uso |
|----------|-----|
| **stack-complete.yml** | Stack completa: Postgres, Redis, MinIO, Kestra (um comando de deploy). |
| stack-proxy-traefik.yml | Proxy Traefik (multi-tenant). |
| stack-minio-global.yml | MinIO global. |
| stack-kestra-tenant.yml | Um tenant Kestra (usa variáveis de ambiente). |
| env.complete.example | Exemplo de .env para stack-complete. |
| RUNBOOK.md | Procedimentos de operação. |

Deploy completo documentado em **[DEPLOY.md](../DEPLOY.md)**.
