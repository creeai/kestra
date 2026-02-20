# Kestra (fork creeai)

Fork do [Kestra](https://kestra.io) com equivalentes OSS de funcionalidades Enterprise e documentação de implementação.

**Repositório da aplicação:** [https://github.com/creeai/kestra](https://github.com/creeai/kestra)

## Deploy (produção)

- **Guia de deploy**: **[`DEPLOY.md`](./DEPLOY.md)** (Compose/Swarm + build da imagem do fork)

## Documentação do projeto

| Documento | Descrição |
|-----------|-----------|
| **[GUIA_IMPLEMENTACAO_ENTERPRISE_KESTRA.md](./GUIA_IMPLEMENTACAO_ENTERPRISE_KESTRA.md)** | Guia de implementação: equivalentes OSS das features Enterprise (Secrets, RBAC, Audit Logs, multi-tenant, maintenance mode, etc.) com referências e caminhos no código. |
| **[DOCUMENTO_IMPLEMENTACAO_EQUIVALENTES_OSS.md](./DOCUMENTO_IMPLEMENTACAO_EQUIVALENTES_OSS.md)** | Tabela de equivalentes por feature, multi-instância (Docker Swarm + EasyPanel), stacks e runbook. |

## Estrutura do repositório

- **`kestra/`** – Código do Kestra (core, webserver, UI, plugins, etc.).
- **`infra/`** – Stacks e scripts para deploy multi-tenant (Traefik, MinIO, Kestra por tenant, backup/restore).
- **`GUIA_IMPLEMENTACAO_ENTERPRISE_KESTRA.md`** – Guia principal.
- **`DOCUMENTO_IMPLEMENTACAO_EQUIVALENTES_OSS.md`** – Documento de equivalentes e infra.

## O que este fork inclui (equivalentes OSS já implementados)

- **Secrets** – Gestão de secrets na UI (com `kestra.encryption.secret-key`).
- **Users, Roles, Bindings (RBAC)** – IAM na UI.
- **Audit Logs** – Tabela, API e página de audit logs.
- **Multi-instância** – Stacks Docker Swarm, scripts de tenant e runbook em `infra/`.

## Como correr em local

```bash
cd kestra
./gradlew runLocal   # Windows: .\gradlew.bat runLocal
```

Aceder a http://localhost:8080.

## Licença

Conforme o projeto [Kestra](https://github.com/kestra-io/kestra) (Apache 2.0). O código em `kestra/` é baseado no repositório oficial.
