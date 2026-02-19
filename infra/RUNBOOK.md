# Runbook – Multi-instância Kestra (Swarm)

## Tenant inacessível (UI não abre)

1. **Router Traefik**  
   Verificar se o serviço do proxy está ativo e se as labels do router para o domínio do tenant estão corretas:
   ```bash
   docker service ls
   docker service ps proxy_traefik
   ```

2. **Serviço Kestra do tenant**  
   Confirmar que o serviço da stack do tenant está a correr:
   ```bash
   docker service ps kestra_<tenant>_kestra
   ```

3. **Postgres**  
   Verificar saúde do contentor Postgres do tenant:
   ```bash
   docker ps -f "name=kestra_<tenant>_postgres"
   docker exec <container_id> pg_isready -U kestra -d kestra
   ```

4. **DNS**  
   Confirmar que o subdomínio (ex.: `tenant1.kestra.seudominio.com`) resolve para o IP do nó onde o Traefik está.

5. **Cloudflare Access**  
   Se estiver a usar Cloudflare Access, confirmar que a política não está a bloquear o subdomínio ou o utilizador.

---

## Execuções não processadas (filas a crescer)

1. **Executor**  
   Verificar se o executor do tenant está a correr:
   ```bash
   docker service ps kestra_<tenant>_executor
   ```

2. **Conexão ao Postgres**  
   O executor e o Kestra usam o mesmo Postgres. Verificar logs do executor:
   ```bash
   docker service logs kestra_<tenant>_executor --tail 100
   ```

3. **Storage (MinIO)**  
   Confirmar que o bucket do tenant existe e que as credenciais S3 (access key/secret) estão correctas na config do stack. Testar acesso ao MinIO a partir da rede do stack.

4. **Pool Postgres**  
   Em carga alta, verificar número de conexões e limites no Postgres (ex.: `max_connections`).

---

## Comandos úteis

- Listar stacks: `docker stack ls`
- Listar serviços de um stack: `docker stack services kestra_<tenant>`
- Logs de um serviço: `docker service logs kestra_<tenant>_kestra`
- Rede overlay: `docker network inspect network_swarm_public`

---

## Referências

- [Kestra installation](https://kestra.io/docs/installation)
- [Kestra configuration](https://kestra.io/docs/configuration)
- [Traefik Swarm](https://doc.traefik.io/traefik/routing/providers/docker/#swarm-mode)
- [Backup and restore](https://kestra.io/docs/administrator-guide/backup-and-restore)
