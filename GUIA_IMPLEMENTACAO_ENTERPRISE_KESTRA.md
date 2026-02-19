# Guia de Implementação – Funcionalidades tipo Enterprise no Kestra OSS

**Documento para o agente Antgravity**  
**Objetivo:** Implementar funcionalidades equivalentes às da Kestra Enterprise no fork/clone do repositório Open Source (Apache 2.0).

---

## 1. Esclarecimento importante (legal e técnico)

### 1.1 O que é possível e o que não é

| Ação | Possível? | Observação |
|------|-----------|------------|
| **Clonar o repositório** `kestra-io/kestra` | ✅ Sim | Licença Apache 2.0 permite uso, modificação e distribuição. |
| **"Desbloquear" funcionalidades Enterprise** | ❌ Não aplicável | O código Enterprise **não está** no repositório público. É produto comercial (licença proprietária) da Kestra Technologies. |
| **Implementar equivalentes no seu fork** | ✅ Sim | Desenvolver suas próprias features inspiradas na documentação pública da Enterprise é desenvolvimento independente e permitido. |

O repositório público contém **apenas** a edição Open Source. As features Enterprise (RBAC, multi-tenancy, audit logs, etc.) estão em código fechado. Este guia é um **roteiro para implementar funcionalidades equivalentes** no seu fork, usando a documentação pública como especificação do comportamento desejado.

### 1.2 Repositórios e licença

- **Repositório principal (OSS):** https://github.com/kestra-io/kestra  
- **Licença:** Apache 2.0  
- **Organização:** https://github.com/kestra-io  
- **Documentação oficial:** https://kestra.io/docs  
- **Comparativo OSS vs Enterprise (referência):** https://kestra.io/docs/oss-vs-paid  

---

## 2. Arquitetura OSS – onde implementar

Componentes principais (todos no repo OSS):

| Componente | Função | Caminho provável no repo |
|------------|--------|---------------------------|
| **Webserver** | API + UI | `webserver/`, `ui/` |
| **Executor** | Orquestração de execuções | `executor/` |
| **Scheduler** | Agendamento e triggers | `scheduler/` |
| **Worker** | Execução de tasks | `worker/` |
| **Repository** | Persistência (JDBC) | `jdbc/`, `jdbc-postgres/`, etc. |
| **Queue** | Fila de jobs (JDBC no OSS) | Integrado ao executor/worker |

Links de arquitetura:

- https://kestra.io/docs/architecture  
- https://kestra.io/docs/architecture/server-components  
- https://kestra.io/docs/architecture/main-components  
- https://kestra.io/docs/performance/sizing-and-scaling-infrastructure  

---

## 3. Funcionalidades a implementar – especificação e links

Cada item abaixo descreve **o que** implementar e **onde** pesquisar (docs e, quando relevante, código). A ordem considera dependências lógicas (ex.: RBAC antes de permissões por namespace).

---

### 3.1 Users Management (Gestão de usuários)

**O que é:** CRUD de usuários, atribuição a grupos/roles, bloqueio/desbloqueio, reset de senha.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/auth/rbac (Users, Groups, Roles)  
- Documentação de auth básica OSS: https://kestra.io/docs/configuration#basic-authentication  

**Como implementar (resumo):**  
- Estender o modelo de autenticação atual (basic auth no OSS).  
- Adicionar entidades: `User`, opcionalmente `Group`.  
- Persistir em JDBC (novas tabelas ou uso de `repository-*`).  
- API em `webserver`: endpoints para criar/editar/listar usuários.  
- UI em `ui/`: página de administração de usuários (lista, formulário, reset de senha).  

**Caminhos de pesquisa no código:**  
- `webserver/` – controllers de API e autenticação  
- `core/` ou `model/` – modelos de domínio  
- Configuração de segurança: procurar por `basic-auth`, `security`, `authentication`  

---

### 3.2 Role-Based Access Control (RBAC)

**O que é:** Permissões por recurso (FLOW, EXECUTION, NAMESPACE, SECRET, etc.) e ação (CREATE, READ, UPDATE, DELETE), atribuídas a usuários/grupos, com opção de escopo por namespace.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/auth/rbac  
- https://kestra.io/docs/enterprise/auth/rbac/permissions-reference  

**Como implementar (resumo):**  
- Definir entidades: `Role` (conjunto de permissões), `Binding` (Role + User/Group + opcionalmente namespace).  
- Middleware/filter no Webserver: antes de cada endpoint, resolver o usuário e verificar se tem a permissão necessária (ex.: FLOW:READ no namespace X).  
- Políticas: Admin (todas as permissões), roles custom (ex.: Developer, Viewer) conforme a tabela de permissões da doc.  
- UI: página IAM (roles, bindings, atribuição a usuários/grupos).  

**Caminhos de pesquisa:**  
- `webserver/` – filtros de segurança, anotações de permissão  
- Lista de permissões e ações na doc (Permissions Reference)  

---

### 3.3 Multi-Tenancy Support

**O que é:** Isolamento por “tenant”. Cada tenant tem seus flows, namespaces, execuções, secrets, etc. Usuários têm acesso a um ou mais tenants.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/governance/tenants  
- API tenants: https://kestra.io/docs/api-reference/enterprise (POST/GET tenants)  

**Como implementar (resumo):**  
- Entidade `Tenant` (id, name, opcionalmente config de storage/secret por tenant).  
- Todas as entidades que hoje são “globais” ou por namespace passam a ter `tenantId` (ou equivalente).  
- Repository/Queries: filtrar todas as consultas por `tenantId`.  
- Resolução de tenant: header, subdomínio ou path (ex.: `/api/v1/{tenantId}/flows`).  
- UI: seletor de tenant no header; URLs incluem tenant (ex.: `/ui/{tenantId}/flows`).  

**Caminhos de pesquisa:**  
- `repository-*`, `jdbc-*` – como flows/executions são buscados  
- `webserver/` – rotas atuais da API (adicionar tenant no path ou contexto)  
- Doc: “Multi-Tenancy Architecture” – https://kestra.io/docs/architecture (seção multi-tenancy)  

---

### 3.4 Namespace-Level Permissions

**O que é:** Restringir acesso por namespace dentro do RBAC (ex.: usuário só vê/edita namespaces `team-a.*`).

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/auth/rbac (Bindings com namespace)  

**Como implementar (resumo):**  
- Estender `Binding` com campo opcional `namespace` ou `namespaces` (lista).  
- Na checagem de permissão: além de (Role, Action), verificar se o recurso (flow/execution) está em um namespace permitido pelo binding.  
- Herança: binding em `prod` pode dar acesso a `prod.engineering` (doc Enterprise).  

**Caminhos de pesquisa:**  
- Mesmo código de RBAC; modelo de namespace em `core/` ou `model/`  

---

### 3.5 Audit Logs & Revision History

**O que é:** Registrar todas as ações (CREATE/UPDATE/DELETE) em recursos (flows, secrets, etc.) com quem fez, quando e diff (revision history).

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/governance/audit-logs  
- Purge: https://kestra.io/docs/administrator-guide/purge (PurgeAuditLogs)  
- Log Shipper: https://kestra.io/docs/enterprise/governance/logshipper  

**Como implementar (resumo):**  
- Tabela (ou índice) `audit_log`: timestamp, actor (user/service account), action, resource_type, resource_id, namespace, tenant, detalhes/diff (JSON ou texto).  
- Interceptar writes no Repository (ou na camada de serviço): após cada create/update/delete, inserir registro de audit.  
- API e UI: listar/filtrar audit logs (por data, usuário, recurso, tipo).  
- Opcional: tarefa de purge por idade (equivalente a PurgeAuditLogs).  
- Revision: para recursos editáveis (ex.: flow), guardar versões (before/after) ou link para snapshot.  

**Caminhos de pesquisa:**  
- `repository-*` – onde flows/triggers/secrets são salvos  
- `executor/` ou `webserver/` – camada que chama o repository  

---

### 3.6 Secret Manager Integrations (externos)

**O que é:** Backends externos para secrets: HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, GCP Secret Manager, Doppler, 1Password, etc.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/governance/secrets-manager  
- https://kestra.io/docs/enterprise/governance/secrets  

**Como implementar (resumo):**  
- Abstração `SecretRepository` ou `SecretBackend`: interface read/get/list (e write se necessário).  
- Implementações por provedor: cliente para Vault, AWS, Azure, GCP, etc. (usar SDKs oficiais).  
- Configuração em YAML (ex.: `kestra.secret.type: vault` + `vault.address`, `vault.token`).  
- Workers: ao executar task que usa secret, obter valor do backend configurado (não persistir em claro no seu DB).  
- Prefix/tenant: suporte a prefixo por tenant/namespace na chave do secret (doc Enterprise).  

**Caminhos de pesquisa:**  
- No OSS, secrets podem estar em JDBC ou arquivo; procurar por “secret” em `core/`, `webserver/`, `worker/`  
- https://kestra.io/docs/configuration (secret)  

---

### 3.7 Namespace-Level Secrets Management

**O que é:** Secrets escopados por namespace (e por tenant se multi-tenancy existir); UI/API por namespace.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/governance/secrets  

**Como implementar (resumo):**  
- Modelo: secret pertence a (tenant +) namespace.  
- Backend interno (JDBC): tabela de secrets com `tenant_id`, `namespace`, `key`, valor cifrado.  
- Backend externo: usar prefixo por tenant/namespace na chave (ex.: `tenant/namespace/key`).  
- API: listar/criar/atualizar/deletar secrets por namespace; respeitar RBAC (SECRET:CREATE/READ/UPDATE/DELETE).  

**Caminhos de pesquisa:**  
- Código atual de secrets no OSS (namespace já pode existir parcialmente)  

**Habilitar a página global "Secrets" na UI (OSS):**  
Para que a página "Secrets" do menu mostre o botão "Adicionar" e a tabela de gestão (sem bloco Enterprise), é obrigatório definir a chave de encriptação. No `application.yml` ou por variável de ambiente:

```yaml
kestra:
  encryption:
    secret-key: "<uma-chave-secreta-forte-e-longa>"
```

Ou variável de ambiente: `KESTRA_ENCRYPTION_SECRET_KEY`. Sem esta chave, o endpoint `/api/v1/configs` não devolve `secretsEnabled: true` e a UI mantém a experiência "bloqueada" (instruções de env vars + promo Enterprise). Após definir a chave, reiniciar o Kestra e recarregar a UI.

---

### 3.8 Worker Security Isolation / Worker Groups

**O que é:** Grupos de workers dedicados (ex.: GPU, rede isolada, por tenant). Tasks/triggers podem ser atribuídos a um `workerGroup.key`.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/scalability/worker-group  

**Como implementar (resumo):**  
- Entidade `WorkerGroup` (key, descrição, opcionalmente allowed tenants).  
- Worker no startup: registrar-se com `--worker-group <key>`.  
- Fila (queue): task run ou polling trigger carrega `workerGroup.key`; apenas workers desse grupo podem pegar o job.  
- Flow/task/trigger: propriedade `workerGroup.key` (e opcionalmente `fallback`: WAIT | FAIL | CANCEL).  
- Default worker group por namespace/tenant (config).  

**Caminhos de pesquisa:**  
- `worker/` – como o worker consome da fila e se identifica  
- `executor/` ou queue – como task runs são enfileirados e com que metadados  
- `scheduler/` – polling triggers  

---

### 3.9 Task Runners (remotos)

**O que é:** Executar tarefas de script (Python, Shell, etc.) em ambientes remotos: Kubernetes, AWS Batch, GCP Batch, Azure Batch, Cloud Run.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/scalability/task-runners  
- Tipos: https://kestra.io/docs/task-runners/types/kubernetes-task-runner, etc.  
- OSS já tem Process e Docker: https://kestra.io/docs/task-runners  

**Como implementar (resumo):**  
- No OSS já existe o conceito de task runner (Process, Docker). Estender com runners remotos.  
- Por tipo (K8s, AWS Batch, etc.): plugin que submete job ao serviço externo, poll até concluir, recupera logs e resultado.  
- Configuração: credenciais e opções por runner (namespace, cluster, fila, etc.).  
- Worker: ao executar task que usa esse runner, delegar ao cliente do runner em vez de executar localmente.  

**Caminhos de pesquisa:**  
- `runner-*` no repo (ex.: `runner-memory`)  
- Plugins de script: `io.kestra.plugin.scripts.*` (Python, Shell, etc.) – como eles usam runner  
- https://kestra.io/docs/task-runners  

---

### 3.10 Service Accounts & API Tokens

**O que é:** Contas de serviço (sem login UI) e tokens de API para acesso programático (CI/CD, Terraform, etc.).

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/auth/service-accounts  
- https://kestra.io/docs/enterprise/auth/api-tokens (se existir doc dedicada)  

**Como implementar (resumo):**  
- Entidade `ServiceAccount` (nome, descrição, opcionalmente grupo).  
- Entidade `ApiToken`: associado a User ou ServiceAccount, hash do token, expiração, optional extended renewal.  
- Autenticação na API: além de basic auth, aceitar header (ex.: `Authorization: Bearer <token>`). Resolver token → user ou service account e aplicar RBAC.  
- UI: gestão de service accounts e criação de tokens (exibir token uma vez).  
- CLI: `--api-token` para comandos (doc Enterprise).  

**Caminhos de pesquisa:**  
- `webserver/` – filtro de autenticação e resolução de usuário  
- CLI: `cli/` – opções de auth  

---

### 3.11 User Invitations

**O que é:** Admin convida usuário por e-mail; link de convite com validade (ex.: 7 dias); usuário define senha ou usa SSO ao aceitar.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/auth/invitations  

**Como implementar (resumo):**  
- Entidade `Invitation`: email, roles/groups, namespaces opcionais, token único, expiry.  
- Endpoint: criar convite (envio de e-mail se configurado); endpoint público para aceitar (token na URL).  
- Aceitar: validar token e expiry; criar usuário (ou vincular a existente) e aplicar bindings; invalidar convite.  
- Configuração: SMTP para e-mail; `expireAfter` (ex.: P7D).  

**Caminhos de pesquisa:**  
- Configuração de e-mail (se houver) em `application.yml` ou docs  
- `webserver/` – novos endpoints de invite e accept  

---

### 3.12 Encryption & Fault Tolerance

**O que é:** Dados em repouso cifrados; HA sem single point of failure (múltiplos executor/worker/scheduler); backend de fila e repositório resiliente.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/overview/enterprise-edition (HA, Kafka, Elasticsearch)  
- https://kestra.io/docs/configuration#encryption  
- HA OSS: https://kestra.io/docs/administrator-guide/high-availability  

**Como implementar (resumo):**  
- Encryption: chave de cifração em config; cifrar campos sensíveis (secrets no DB, backups).  
- Fault tolerance: rodar múltiplas instâncias de executor, worker, scheduler; fila JDBC suporta concorrência; evitar locks longos.  
- Opcional (maior esforço): backend Kafka + Elasticsearch como na Enterprise (replicar arquitetura da doc).  

**Caminhos de pesquisa:**  
- https://kestra.io/docs/administrator-guide/high-availability  
- Configuração de queue e repository no OSS  

---

### 3.13 Apps (formulários e aprovações)

**O que é:** “Apps” como frontends para flows: formulários que disparam execuções com inputs; aprovações para execuções pausadas.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/scalability/apps  
- Exemplos (repositório de exemplos EE): https://github.com/kestra-io/enterprise-edition-examples (apps em YAML)  

**Como implementar (resumo):**  
- Modelo: `App` (id, namespace, flowId, tipo FORM | APPROVAL, definição de blocos de UI, acesso PUBLIC/PRIVATE).  
- Form App: UI que renderiza inputs do flow; submit → POST execution com inputs.  
- Approval App: lista execuções em estado “waiting”; ações Approve/Reject que chamam API de resumption.  
- API: CRUD de apps; endpoint público (ou com token) para renderizar app e submeter.  
- UI: página “Apps” e visualização/embed de app (iframe ou rota dedicada).  

**Caminhos de pesquisa:**  
- `ui/` – novos componentes de app  
- `webserver/` – endpoints de execution (trigger, resume)  
- enterprise-edition-examples (apps): https://github.com/kestra-io/enterprise-edition-examples  

---

### 3.14 Customizable UI Links (links customizáveis)

**O que é:** Links customizados na UI (ex.: links para docs, dashboards externos, por tenant ou global).

**Referência Enterprise:**  
- Mencionado na lista; pode ser “Customizable UI” ou “Custom Links” nas configurações de tenant/instance.  

**Como implementar (resumo):**  
- Config ou tabela: lista de { label, url, ícone, escopo (instance/tenant) }.  
- UI: renderizar no menu ou header conforme permissão/tenant.  

**Caminhos de pesquisa:**  
- `ui/` – menu, sidebar, configuração de tenant  

---

### 3.15 Backup & Restore (metadados)

**O que é:** Backup e restauração de metadados (flows, namespaces, triggers, secrets internos, usuários, roles, etc.) com possível inclusão de dados de execução.

**Referência Enterprise:**  
- https://kestra.io/docs/administrator-guide/backup-and-restore  
- CLI: `kestra backups create FULL`, `kestra backups restore <uri>`  

**Como implementar (resumo):**  
- Comando CLI (ou API admin): exportar entidades relevantes para um arquivo (JSON/YAML) ou formato binário, com opção de cifração.  
- Restore: ler arquivo e reimportar (criar/atualizar) com cuidado para conflitos e ordem de dependências.  
- Incluir opção `--include-data` (executions, logs) se o tamanho for gerenciável.  
- Integração com internal storage: backup de arquivos de namespace se necessário.  

**Caminhos de pesquisa:**  
- Estrutura de dados de flows, triggers, etc. em `model/` ou `core/`  
- CLI: `cli/`  

---

### 3.16 Maintenance Mode

**O que é:** Modo de manutenção: novas execuções são enfileiradas mas não processadas; workers terminam tarefas atuais e não pegam novas; permite upgrade seguro.

**Referência Enterprise:**  
- https://kestra.io/docs/enterprise/instance/maintenance-mode  

**Como implementar (resumo):**  
- Flag global (em DB ou config dinâmica): `maintenanceMode: true/false`.  
- Webserver/Scheduler: continuam aceitando execuções e agendamentos (gravação na fila).  
- Executor: se em maintenance, não despacha novos task runs (ou os marca como “queued”).  
- Workers: não pegam novos jobs da fila quando em maintenance (ou apenas terminam os já atribuídos).  
- UI: botão “Enter/Exit maintenance” (admin only) e banner informativo.  

**Caminhos de pesquisa:**  
- `executor/` – onde execuções são despachadas  
- `worker/` – polling da fila  
- `webserver/` – endpoint para setar flag e UI  

---

### 3.17 AI Copilot (Gemini)

**O que é:** Assistente na UI do editor de flows que gera/edita YAML a partir de linguagem natural (ex.: modelo Gemini).

**Referência Enterprise:**  
- https://kestra.io/docs/ai-tools/ai-copilot  
- OSS suporta Gemini; Enterprise adiciona outros LLMs.  

**Como implementar (resumo):**  
- Configuração: `kestra.ai.type: gemini`, `api-key`, `model-name`.  
- Endpoint no Webserver (ou chamada direta do front): enviar prompt + contexto (flow atual); chamar API Gemini; retornar YAML sugerido.  
- UI: painel no editor (ex.: canto superior direito) com input de prompt e botão “Apply” no código gerado.  

**Caminhos de pesquisa:**  
- https://kestra.io/docs/ai-tools  
- Plugin Gemini (flows): https://kestra.io/plugins/plugin-gemini  
- `ui/` – editor de flow e componente do copilot  

---

### 3.18 Multi-instância por cliente/time (isolamento forte)

**O que é:** Cada cliente ou time com instância Kestra própria (deploy separado), em vez de um único multi-tenant.

**Como implementar (resumo):**  
- Não é uma feature única de código; é estratégia de deploy.  
- Por instância: um deploy Kestra (VM/container) por cliente/time; banco e fila separados (ou schemas/databases separados no mesmo cluster).  
- Orquestração: Docker Swarm, Kubernetes (um namespace ou helm release por cliente), ou EasyPanel (ver abaixo).  
- Autenticação e domínios: um subdomínio ou path por instância (ex.: `cliente-a.seudominio.com`).  

**Links úteis:**  
- https://kestra.io/docs/installation  
- Docker Compose: https://github.com/kestra-io/kestra/blob/develop/docker-compose.yml  
- Helm (K8s): repositório helm da organização kestra-io  

---

### 3.19 Docker Swarm + EasyPanel

**O que é:** Rodar Kestra em Docker Swarm e gerenciar (ou provisionar) instâncias via EasyPanel para multi-instância.

**Como implementar (resumo):**  
- **Docker Swarm:** definir stack (compose v3) com serviços: webserver, executor, scheduler, worker(s), Postgres (ou MySQL), internal storage (volume ou S3). Rede e secrets do Swarm para senhas.  
- **EasyPanel:** usar como painel para deploy de aplicações em servidores; criar “template” ou “app” que instancia um stack Kestra por cliente (cada um com seu DB e volumes).  
- Documentação: EasyPanel (https://easypanel.io ou similar) para multi-app; documentação Docker do Kestra para variáveis de ambiente e backend JDBC.  

**Links úteis:**  
- https://github.com/kestra-io/kestra/tree/develop/docker  
- https://kestra.io/docs/installation/docker  
- EasyPanel: buscar documentação oficial atual (deploy apps, templates)  

---

## 4. Ordem sugerida de implementação (para o agente)

1. **Fundação:** Users Management + RBAC (incl. namespace-level permissions).  
2. **Segurança e governança:** Audit Logs, Service Accounts & API Tokens, Secret Manager (interno por namespace).  
3. **Multi-tenant:** Multi-Tenancy; depois Namespace-Level Secrets e integrações externas de secrets.  
4. **Escalabilidade:** Worker Groups, Task Runners (um por vez: K8s, depois AWS/GCP/Azure se necessário).  
5. **Produtividade:** User Invitations, Apps (Form + Approval), AI Copilot (Gemini).  
6. **Operação:** Backup & Restore, Maintenance Mode, Encryption & HA.  
7. **Customização:** Customizable UI Links.  
8. **Deploy:** Multi-instância (Docker Swarm + EasyPanel) como receita de deploy, não como feature única no core.

---

## 5. Links consolidados – pesquisa e referência

### Documentação Kestra (oficial)

| Tema | URL |
|------|-----|
| OSS vs Enterprise | https://kestra.io/docs/oss-vs-paid |
| Enterprise overview | https://kestra.io/docs/enterprise/overview/enterprise-edition |
| Arquitetura | https://kestra.io/docs/architecture |
| Server components | https://kestra.io/docs/architecture/server-components |
| RBAC | https://kestra.io/docs/enterprise/auth/rbac |
| Permissions reference | https://kestra.io/docs/enterprise/auth/rbac/permissions-reference |
| Multi-tenancy | https://kestra.io/docs/enterprise/governance/tenants |
| Audit logs | https://kestra.io/docs/enterprise/governance/audit-logs |
| Secrets | https://kestra.io/docs/enterprise/governance/secrets |
| Secrets manager (externos) | https://kestra.io/docs/enterprise/governance/secrets-manager |
| Service accounts | https://kestra.io/docs/enterprise/auth/service-accounts |
| Invitations | https://kestra.io/docs/enterprise/auth/invitations |
| Worker groups | https://kestra.io/docs/enterprise/scalability/worker-group |
| Task runners | https://kestra.io/docs/enterprise/scalability/task-runners |
| Apps | https://kestra.io/docs/enterprise/scalability/apps |
| Backup & Restore | https://kestra.io/docs/administrator-guide/backup-and-restore |
| Maintenance mode | https://kestra.io/docs/enterprise/instance/maintenance-mode |
| AI Copilot | https://kestra.io/docs/ai-tools/ai-copilot |
| Configuração | https://kestra.io/docs/configuration |
| API reference | https://kestra.io/docs/api-reference |
| API Enterprise | https://kestra.io/docs/api-reference/enterprise |
| HA | https://kestra.io/docs/administrator-guide/high-availability |

### Repositórios GitHub

| Repositório | URL |
|-------------|-----|
| Kestra OSS (principal) | https://github.com/kestra-io/kestra |
| Organização Kestra | https://github.com/kestra-io |
| Exemplos Enterprise (apps, etc.) | https://github.com/kestra-io/enterprise-edition-examples |
| Documentação (repositório) | https://github.com/kestra-io/docs |
| Deployment templates | https://github.com/kestra-io/deployment-templates |

### Caminhos no código (repo kestra-io/kestra)

| Área | Caminho |
|-----|---------|
| API e auth | `webserver/` |
| UI | `ui/` |
| Orquestração | `executor/` |
| Execução de tasks | `worker/` |
| Agendamento | `scheduler/` |
| Modelos e core | `core/`, `model/` |
| Persistência JDBC | `jdbc/`, `jdbc-postgres/`, `jdbc-mysql/`, `jdbc-h2/` |
| CLI | `cli/` |
| Docker | `docker/`, `Dockerfile`, `docker-compose.yml` |

---

## 6. Notas finais para o agente Antgravity

- Use a **documentação pública** da Enterprise apenas como **especificação de comportamento**; não há código Enterprise no repositório público para reutilizar.  
- Mantenha **compatibilidade** com a API e modelos OSS onde fizer sentido, para upgrades futuros do upstream.  
- Para cada feature, considerar **configuração** (feature flag ou config) para ativar/desativar sem recompilar.  
- Testes: **unitários** para regras de RBAC e resolução de tenant; **integração** para API e fluxos de execução.  
- Este documento pode ser atualizado conforme você avança (links quebrados, novos endpoints, descobertas no código).

---

*Documento gerado para implementação de funcionalidades tipo Enterprise no fork Kestra OSS (Apache 2.0). Todas as referências apontam para documentação e repositórios públicos.*
