# Deploy em produção (com plugins)

Para subir esta aplicação em produção **com todas as tuas alterações (Users, RBAC, IAM) e com os plugins open-source** já incluídos na imagem, usa uma das opções abaixo.

## Opção recomendada: build da imagem com plugins

### 1. No teu PC (Windows)

```powershell
cd c:\Users\micha\Desktop\Kestra\kestra
.\build-docker-with-plugins.ps1
```

Ou, se usares Git Bash / WSL (Linux):

```bash
./build-docker-with-plugins.sh
```

Isto vai:

- Ler a versão do projeto
- Obter a lista de plugins compatíveis na API da Kestra
- Construir o JAR executável
- Construir a imagem Docker **com esses plugins instalados**
- A imagem fica com o tag `kestra/kestra:<versão>-with-plugins`

Para correr em produção:

```bash
docker run -p 8080:8080 -v kestra_data:/app/storage kestra/kestra:1.3.0-with-plugins server local
```

(Substitui `1.3.0` pela versão que aparecer no script.)

### 2. No GitHub (CI/CD)

Foi adicionado o workflow **Build Docker with plugins** (`.github/workflows/build-docker-with-plugins.yml`):

- **Push para `main`** ou **criação de um release** → a imagem é construída com plugins e publicada no **GitHub Container Registry (GHCR)**.
- **Execução manual** → em Actions, escolhe "Build Docker with plugins" e "Run workflow".

A imagem fica em:

```
ghcr.io/<teu-user>/<teu-repo>:<versão>-with-plugins
ghcr.io/<teu-user>/<teu-repo>:latest-with-plugins
```

Para fazer pull e correr em produção (ex.: no teu servidor ou Kubernetes):

```bash
docker pull ghcr.io/<teu-user>/<teu-repo>:latest-with-plugins
docker run -p 8080:8080 -v kestra_data:/app/storage ghcr.io/<teu-user>/<teu-repo>:latest-with-plugins server local
```

## Resumo

| Onde      | Comando / Ação |
|----------|----------------|
| Local (Windows) | `.\build-docker-with-plugins.ps1` |
| Local (Linux/Mac) | `./build-docker-with-plugins.sh` |
| GitHub   | Push em `main` ou novo release → imagem em GHCR com plugins |

Assim, a aplicação em produção já vem com **código (Users, RBAC, IAM) + plugins open-source** na mesma imagem.
