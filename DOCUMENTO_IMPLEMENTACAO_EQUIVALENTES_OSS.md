# Documento de Implementação – Equivalentes OSS e Multi-instância

**Documento para o agente Antgravity**  
**Objetivo:** Roteiro único para implementar equivalentes OSS de todas as funcionalidades Enterprise referenciadas na documentação pública, com blueprint de multi-instância (Docker Swarm + EasyPanel).

---

## Parte 1 – Enquadramento e referências

### 1.1 Princípio

- **Clonar o repositório** [kestra-io/kestra](https://github.com/kestra-io/kestra) é permitido (licença Apache 2.0).
- **"Desbloquear"** funcionalidades Enterprise (contornar licença ou flags) **não se aplica**: o código Enterprise não está no repositório público.
- **Implementar equivalentes no seu fork** é o caminho correto: desenvolver as suas próprias features inspiradas na documentação pública da Enterprise, como desenvolvimento independente e permitido.

### 1.2 Especificação detalhada

O **[GUIA_IMPLEMENTACAO_ENTERPRISE_KESTRA.md](GUIA_IMPLEMENTACAO_ENTERPRISE_KESTRA.md)** é a especificação detalhada por feature:

- **Secções 3.x:** o que é cada funcionalidade, referência Enterprise, como implementar (resumo), caminhos de pesquisa no código.
- **Secção 4:** ordem sugerida de implementação (fundação → governança → multi-tenant → escalabilidade → produtividade → operação → customização → deploy).

Use o guia como fonte de requisitos; este documento fornece o checklist de equivalentes e o blueprint de multi-instância.

### 1.3 Links oficiais (pesquisa)

| Tipo | URL |
|------|-----|
| Documentação geral | https://kestra.io/docs |
| OSS vs Enterprise | https://kestra.io/docs/oss-vs-paid |
| Configuração | https://kestra.io/docs/configuration |
| Repositório principal | https://github.com/kestra-io/kestra |
| Organização | https://github.com/kestra-io |
| Deployment templates | https://github.com/kestra-io/deployment-templates |

---

## Parte 2 – Tabela de equivalentes por feature (checklist para o agente)

Para cada funcionalidade Enterprise, implementar o **equivalente OSS** conforme a linha abaixo. Doc = documentação pública de referência; Caminho = onde implementar no repo; Equivalente = o que construir em uma frase.

| # | Feature | Doc de referência | Caminho no repositório OSS | Equivalente OSS (resumo) |
|---|--------|-------------------|----------------------------|---------------------------|
| 1 | AI Copilot (Gemini) | [AI tools](https://kestra.io/docs/ai-tools) | `kestra/ui/`, `kestra/webserver/` | Manter Gemini; UI com prompt + Apply no editor. |
| 2 | Users Management | [RBAC](https://kestra.io/docs/enterprise/auth/rbac) | `kestra/webserver/`, `kestra/jdbc/` | User CRUD, grupos opcionais, persistência JDBC, UI admin. |
| 3 | RBAC | [RBAC](https://kestra.io/docs/enterprise/auth/rbac), [Permissions reference](https://kestra.io/docs/enterprise/auth/rbac/permissions-reference) | `kestra/webserver/` (filters), `kestra/core/` | Roles + Bindings; filter por permissão (FLOW/EXECUTION/SECRET, CRUD); UI IAM. |
| 4 | Multi-Tenancy | [Tenants](https://kestra.io/docs/enterprise/governance/tenants) | `kestra/` (repository, jdbc, webserver) | Tenant id em entidades; API `/api/v1/{tenant}/...`; UI seletor tenant. |
| 5 | Audit Logs & Revision History | [Audit logs](https://kestra.io/docs/enterprise/governance/audit-logs) | repository/service layer, `kestra/jdbc/` | Tabela audit_log; interceptar writes; API/UI listar; purge por idade. |
| 6 | Secret Manager (externos) | [Secrets manager](https://kestra.io/docs/enterprise/governance/secrets-manager) | `kestra/core/secret`, `kestra/worker/` | Interface SecretBackend; impl Vault/AWS/etc.; config YAML; worker resolve em runtime. |
| 7 | Namespace-Level Permissions | [RBAC](https://kestra.io/docs/enterprise/auth/rbac) | Binding + namespace; mesmo filter RBAC | Binding com namespace/namespaces; checagem no filter. |
| 8 | Worker Security / Worker Groups | [Worker group](https://kestra.io/docs/enterprise/scalability/worker-group) | `kestra/worker/`, `kestra/executor/`, queue | WorkerGroup entity; worker `--worker-group`; queue por group; flow task `workerGroup.key`. |
| 9 | User Invitations | [Invitations](https://kestra.io/docs/enterprise/auth/invitations) | `kestra/webserver/`, SMTP config | Invitation entity; endpoints create/accept; e-mail opcional; token + expiry. |
| 10 | Encryption & Fault Tolerance | [Encryption](https://kestra.io/docs/configuration#encryption) | config + DB encryption at rest | `kestra.encryption.secret-key`; HA via múltiplas instâncias + fila/repo resiliente. |
| 11 | Namespace-Level Secrets | [Secrets](https://kestra.io/docs/enterprise/governance/secrets) | `kestra/core/`, `kestra/jdbc/`, `kestra/webserver/` (já parcial) | Secrets por namespace (JDBC + cifra); API por namespace; UI já habilitável com secret-key. |
| 12 | Apps | [Apps](https://kestra.io/docs/enterprise/scalability/apps) | externo ou `kestra/ui/` | Front externo (Next/WeWeb) a chamar API; ou módulo UI "apps". |
| 13 | Customizable UI Links | Enterprise (custom links) | `kestra/ui/`, config | Config `kestra.ui.custom-links` ou equivalente; menu dinâmico. |
| 14 | Backup & Restore | [Backup and restore](https://kestra.io/docs/administrator-guide/backup-and-restore) | scripts + DB + storage | pg_dump + backup bucket S3/MinIO; scripts restore; sem CLI EE. |
| 15 | Worker Groups (distributed) | (igual 8) | (igual 8) | (igual 8). |
| 16 | Task Runners | [Task runners](https://kestra.io/docs/task-runners) | `kestra/runner-*`, plugins | OSS Process/Docker; extensão para K8s/Batch se necessário (plugin). |
| 17 | Service Accounts & API Tokens | [Service accounts](https://kestra.io/docs/enterprise/auth/service-accounts) | `kestra/webserver/` auth, `kestra/jdbc/` | ServiceAccount + ApiToken; Bearer auth; UI gestão tokens. |
| 18 | Maintenance Mode | [Maintenance mode](https://kestra.io/docs/enterprise/instance/maintenance-mode) | executor/scheduler config | Flag ou endpoint que pausa novas execuções; doc procedimento manual OSS. |

---

## Parte 3 – Multi-instância por cliente/time (Docker Swarm + EasyPanel)

Estratégia de deploy **sem EE**: uma instância Kestra por tenant (cliente/time), com isolamento real de dados e rede. O agente deve usar esta secção para implementar a infraestrutura e os templates.

### 3.1 Decisão de arquitetura

- **1 stack por tenant:** Postgres + Kestra (API+UI) + executor por tenant; banco e fila isolados.
- **Storage:** MinIO global com um bucket por tenant (recomendado) ou MinIO por tenant (isolamento total).
- **Roteamento:** um subdomínio por tenant (ex.: `tenant1.kestra.seudominio.com`).

Referências: [Kestra installation](https://kestra.io/docs/installation), [Configuration](https://kestra.io/docs/configuration).

### 3.2 Convenções obrigatórias

| Recurso | Padrão | Exemplo |
|---------|--------|---------|
| Stack | `kestra_<tenant>` | `kestra_tenant1` |
| Network privada | `net_<tenant>_private` | `net_tenant1_private` |
| Volume Postgres | `pg_<tenant>_data` | `pg_tenant1_data` |
| Bucket S3/MinIO | `kestra-<tenant>` | `kestra-tenant1` |
| Domínio | `<tenant>.kestra.seudominio.com` | `tenant1.kestra.seudominio.com` |

### 3.3 Infra global

**Rede overlay (uma vez):**

```bash
docker network create --driver=overlay --attachable network_swarm_public
```

**Stack proxy (Traefik):**

- Provider Docker em modo Swarm; labels em `deploy.labels` (não em `labels` do serviço).
- Para cada serviço Kestra exposto: `traefik.http.services.<name>.loadbalancer.server.port=8080` (obrigatório no Swarm).
- Entrypoints 80/443; `exposedByDefault=false`.

Documentação: [Traefik Swarm mode](https://doc.traefik.io/traefik/routing/providers/docker/#swarm-mode).

**Stack MinIO global (opcional):**

- Um MinIO para todos os tenants; criar um bucket por tenant (`kestra-tenant1`, `kestra-tenant2`, …).
- Criar Access Key/Secret por tenant com policy restrita ao respetivo bucket.

### 3.4 Stack por tenant (template)

**Serviços:**

1. **postgres** – base de dados do tenant (healthcheck, volume `pg_<tenant>_data`, network privada).
2. **kestra** – API + UI; depende do Postgres; config com repository + queue Postgres, storage S3 (endpoint MinIO), `kestra.server.base-url` = `https://${DOMAIN}`; ligado à network privada e a `network_swarm_public` (para o proxy).
3. **executor** – `command: ["server", "executor"]`; mesma config de repository, queue e storage; apenas network privada.

**Variáveis de ambiente por tenant:**

- `DOMAIN` – subdomínio (ex.: `tenant1.kestra.seudominio.com`).
- `POSTGRES_PASSWORD`
- `S3_ENDPOINT` – URL do MinIO global (ex.: `http://infra_minio:9000`).
- `S3_BUCKET` – bucket do tenant (ex.: `kestra-tenant1`).
- `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`

**Config Kestra (resumo):**

- `kestra.repository.type`: postgres  
- `kestra.queue.type`: postgres  
- `kestra.jdbc.url`: `jdbc:postgresql://postgres:5432/kestra` (e credenciais).  
- `kestra.storage.type`: s3; endpoint, bucket, accessKey, secretKey, path-style-access conforme MinIO.  
- `kestra.server.base-url`: `https://${DOMAIN}`  

**Labels Traefik (serviço kestra):**

- `traefik.enable=true`
- `traefik.http.routers.kestra.rule=Host(\`${DOMAIN}\`)`
- `traefik.http.routers.kestra.entrypoints=websecure` (ou web)
- `traefik.http.services.kestra.loadbalancer.server.port=8080`

Links: [Kestra Docker](https://kestra.io/docs/installation/docker), [docker no repo](https://github.com/kestra-io/kestra/tree/develop/docker).

### 3.5 Segurança "tipo Enterprise"

- **Não expor** a UI/API sem controlo de acesso.
- Colocar **Cloudflare Access** (ou outro IdP) à frente: proteger subdomínios ou usar wildcard (ex.: `*.kestra.seudominio.com`); políticas por e-mail/grupo/domínio.
- Postgres e executor **não** devem estar na rede pública; apenas o serviço Kestra (API+UI) é exposto via proxy.

### 3.6 Backups (OSS)

- **Postgres (por tenant):** pg_dump diário (ou contínuo com WAL); retenção (ex.: 7/30 dias); envio para storage externo (S3/MinIO/Backblaze).
- **Bucket (por tenant):** `mc mirror` (ou equivalente) do bucket do tenant para destino de backup; retenção e versionamento se disponível.
- **Restore:** restaurar Postgres (psql + dump) e bucket (mirror de volta); redeploy do stack do tenant.

Contexto EE e boas práticas: [Backup and restore](https://kestra.io/docs/administrator-guide/backup-and-restore).

### 3.7 Runbooks

**Tenant inacessível (UI não abre):**

1. Verificar router Traefik (labels no serviço).
2. Verificar se o serviço `kestra_<tenant>_kestra` tem task running.
3. Verificar saúde do Postgres (healthcheck).
4. Verificar DNS (subdomínio a apontar para o nó correto).
5. Verificar política Cloudflare Access (não estar a bloquear).

**Execuções não processadas:**

1. Verificar se o executor está a correr.
2. Verificar conectividade do executor ao Postgres e ao storage (MinIO).
3. Verificar pool de conexões e carga no Postgres.

### 3.8 Entrega esperada (checklist)

O agente deve entregar (num repo ou pasta de infraestrutura):

- `stack-proxy-traefik.yml` – proxy Swarm.
- `stack-minio-global.yml` – (opcional) MinIO global.
- `stack-kestra-tenant.yml` – template da stack por tenant (variáveis por env).
- `.env.example` – lista de variáveis por tenant (DOMAIN, POSTGRES_PASSWORD, S3_*).
- Scripts: `create-tenant.sh` (criar bucket + keys + deploy stack), `backup-tenant.sh`, `restore-tenant.sh`.
- Runbook em texto: incidentes comuns + comandos de verificação (resumo da secção 3.7).

---

## Parte 4 – Ordem de implementação e notas para o agente

### 4.1 Ordem (alinhada ao guia, secção 4)

1. **Fundação:** Users Management + RBAC (incl. namespace-level permissions).  
2. **Segurança e governança:** Audit Logs, Service Accounts & API Tokens, Secret Manager (interno por namespace).  
3. **Multi-tenant:** Multi-Tenancy; depois Namespace-Level Secrets e integrações externas de secrets.  
4. **Escalabilidade:** Worker Groups, Task Runners (um por vez: K8s, depois AWS/GCP/Azure se necessário).  
5. **Produtividade:** User Invitations, Apps (Form + Approval), AI Copilot (Gemini).  
6. **Operação:** Backup & Restore, Maintenance Mode, Encryption & HA.  
7. **Customização:** Customizable UI Links.  
8. **Deploy:** Multi-instância (Docker Swarm + EasyPanel) como receita de deploy, não como feature única no core.

### 4.2 Notas finais

- Use **apenas documentação pública** como especificação; não há código Enterprise no repositório público para reutilizar.  
- Mantenha **compatibilidade** com a API e modelos OSS onde fizer sentido, para upgrades futuros do upstream.  
- Para cada feature, considerar **configuração** (feature flag ou config) para ativar/desativar sem recompilar.  
- **Testes:** unitários para regras de RBAC e resolução de tenant; integração para API e fluxos de execução.  
- Este documento e o [GUIA_IMPLEMENTACAO_ENTERPRISE_KESTRA.md](GUIA_IMPLEMENTACAO_ENTERPRISE_KESTRA.md) podem ser atualizados conforme o avanço (links quebrados, novos endpoints, descobertas no código).

---

*Documento gerado para implementação de equivalentes OSS no fork Kestra (Apache 2.0). Todas as referências apontam para documentação e repositórios públicos.*
