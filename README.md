# API de cursos online

Projeto do exercicio de 14/09/2026. A API cria e lista cursos, permite filtro por prefixo do nome e usa exclusao logica. O projeto inclui PostgreSQL, Docker, testes unitarios, testes de integracao, cobertura JaCoCo e pipelines de CI/CD com GitHub Actions.

Para configurar tudo usando VS Code e Git Bash, siga o [Guia de setup com Git Bash VS Code e GitHub Secrets](GUIA-GIT-BASH-VSCODE.md).

## Rotas

| Metodo | Rota | Resultado |
| --- | --- | --- |
| `GET` | `/cursos` | Lista apenas cursos nao deletados |
| `GET` | `/cursos?nome=jav` | Lista cursos cujo nome comeca com `jav`, sem diferenciar maiusculas e minusculas |
| `POST` | `/cursos` | Cria um curso |
| `DELETE` | `/cursos/{id}` | Marca o curso como deletado e retorna `204` |

Exemplo de criacao:

```json
{
  "nome": "Java Basico",
  "descricao": "Fundamentos de Java",
  "cargaHoraria": 20
}
```

## Executar testes

Requisitos: Java 17 ou superior e Maven 3.9 ou superior.

```bash
mvn clean verify
```

O comando tambem verifica a regra de 100% de cobertura de linhas da classe `CursoService`. O relatorio fica em `target/site/jacoco/index.html`.

## Executar localmente com Docker

1. Instale e inicie o Docker Desktop.
2. Copie `.env.example` para `.env`.
3. Troque `DB_PASSWORD` por uma senha forte. O arquivo `.env` e ignorado pelo Git.
4. Execute:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

A API ficara em `http://localhost:8080`. Para encerrar:

```bash
docker compose down
```

## Preparar a maquina AWS

Use uma instancia Ubuntu com Docker instalado. No Security Group, permita porta 22 apenas para o seu IP e porta 8080 para quem deve acessar a API. Nao exponha a porta 5432 publicamente.

Conecte por SSH:

```bash
chmod 400 projsoft26b.pem
ssh -i "projsoft26b.pem" ubuntu@IP_DA_INSTANCIA
```

Na instancia, crie a rede e o PostgreSQL. Substitua os valores antes de executar:

```bash
export DB_NAME="cursosdb"
export DB_USER="cursos"
export DB_PASSWORD="SENHA_FORTE"

docker network inspect rede >/dev/null 2>&1 || docker network create -d bridge rede
docker run -d \
  --name postgres-cursos \
  --restart unless-stopped \
  --network rede \
  -e POSTGRES_DB="$DB_NAME" \
  -e POSTGRES_USER="$DB_USER" \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -v postgres-cursos-data:/var/lib/postgresql/data \
  postgres:17-alpine
```

O banco nao precisa publicar a porta 5432. O container da API o encontra pelo nome `postgres-cursos` porque ambos usam a rede Docker `rede`.

## Configurar Docker Hub

1. Crie no Docker Hub o repositorio `cursos-api`.
2. Deixe-o publico para a instancia AWS conseguir executar `docker pull` sem armazenar outra credencial no servidor.
3. Em Docker Hub, abra **Account settings > Personal access tokens > Generate new token**.
4. Crie um token com permissao de leitura e escrita. Copie-o uma unica vez.

## Configurar GitHub Secrets

No GitHub, abra **Settings > Secrets and variables > Actions > New repository secret** e cadastre:

| Secret | Valor |
| --- | --- |
| `DOCKERHUB_USERNAME` | Seu usuario do Docker Hub |
| `DOCKERHUB_TOKEN` | Token do Docker Hub, nunca a senha da conta |
| `HOST_TEST` | IP publico ou DNS da instancia AWS |
| `SSH_USER` | `ubuntu`, se a instancia usa a imagem Ubuntu |
| `KEY_TEST` | Conteudo completo do arquivo `.pem`, incluindo BEGIN e END |
| `DB_HOST` | `postgres-cursos` |
| `DB_PORT` | `5432` |
| `DB_NAME` | Mesmo nome usado ao criar o PostgreSQL |
| `DB_USER` | Mesmo usuario usado ao criar o PostgreSQL |
| `DB_PASSWORD` | Mesma senha forte usada ao criar o PostgreSQL |

O secret automatico `GITHUB_TOKEN` e criado pelo GitHub e nao precisa ser cadastrado.

## Fluxo de Pull Request exigido pelo exercicio

Uma rota deve entrar por Pull Request. Use a rota DELETE como essa entrega:

```bash
git checkout -b feature/delete-curso
# implemente ou adicione os arquivos da rota DELETE e seus testes
git add src
git commit -m "feat: adiciona exclusao logica de cursos"
git push -u origin feature/delete-curso
```

No GitHub, abra o Pull Request de `feature/delete-curso` para `main`. O workflow `Testes` executara `mvn clean verify`. So faca o merge quando o pipeline estiver verde. O merge em `main` dispara `Deploy AWS`, publica a imagem no Docker Hub e atualiza o container na instancia.

Recomenda-se ainda habilitar a protecao de branch em **Settings > Branches > Add branch protection rule**, selecionar `main`, exigir Pull Request e exigir que o status check `tests` passe.

## Testar a API implantada

```bash
curl -X POST "http://IP_DA_INSTANCIA:8080/cursos" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Java Basico","descricao":"Fundamentos de Java","cargaHoraria":20}'

curl "http://IP_DA_INSTANCIA:8080/cursos"
curl "http://IP_DA_INSTANCIA:8080/cursos?nome=jav"
curl -X DELETE "http://IP_DA_INSTANCIA:8080/cursos/1"
```

Para diagnosticar a instancia:

```bash
docker ps --all
docker logs cursos-api
docker logs postgres-cursos
```
