# Guia de setup com Git Bash VS Code e GitHub Secrets

Este guia configura o projeto em tres ambientes diferentes:

1. No seu computador, usando VS Code, Git Bash, Docker Desktop e um arquivo `.env` local.
2. No GitHub, usando GitHub Secrets e GitHub Actions.
3. Na AWS, usando Docker, PostgreSQL e as variaveis enviadas pelo pipeline.

Nunca coloque senhas, token do Docker Hub, chave `.pem` ou IP diretamente no codigo, no `application.properties` ou nos arquivos YAML.

## 1 Instalar e conferir os programas

Instale:

- Git for Windows, que inclui o Git Bash.
- VS Code.
- Java JDK 21.
- Maven 3.9 ou superior.
- Docker Desktop.

No VS Code, instale as extensoes `Extension Pack for Java` e `Spring Boot Extension Pack`.

Abra o VS Code, pressione `Ctrl+Shift+P`, procure `Terminal Select Default Profile` e selecione `Git Bash`. Abra um terminal novo e confira:

```bash
git --version
java -version
mvn -version
docker --version
docker compose version
```

O Java deve mostrar a versao 21. O Docker Desktop precisa estar aberto antes dos comandos Docker.

## 2 Clonar o seu repositorio

No terminal Git Bash do VS Code:

```bash
cd /c/Users/ezsan/Documents
git clone https://github.com/EnzoS-5/exercicio.git
cd exercicio
code .
```

Se o GitHub abrir uma janela de autenticacao, entre na sua conta e autorize o Git Credential Manager. A senha normal da conta nao funciona como senha de Git.

Copie para essa pasta o conteudo do projeto entregue, mantendo inclusive as pastas ocultas `.github` e os arquivos `.gitignore`, `.dockerignore` e `.env.example`.

Confira o resultado:

```bash
ls -la
find .github -maxdepth 2 -type f
git status
```

Voce deve encontrar:

```text
.github/workflows/tests.yml
.github/workflows/deploy.yml
src/
Dockerfile
docker-compose.yml
pom.xml
```

## 3 Configurar o ambiente local

GitHub Secrets nao ficam disponiveis no seu computador. Localmente, use `.env`:

```bash
cp .env.example .env
code .env
```

Preencha com valores apenas para desenvolvimento local:

```dotenv
DB_NAME=cursosdb
DB_USER=cursos
DB_PASSWORD=COLOQUE_UMA_SENHA_FORTE_AQUI
```

Confirme que o arquivo esta ignorado antes de continuar:

```bash
git check-ignore .env
git status
```

O primeiro comando deve imprimir `.env`, e o arquivo nao pode aparecer entre os arquivos que serao enviados ao GitHub.

## 4 Executar localmente com Docker

Com o Docker Desktop aberto:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

Quando aparecer que a aplicacao iniciou, pressione `Ctrl+C` apenas para sair da tela de logs. Os containers continuam executando.

Teste as rotas em outro terminal Git Bash:

```bash
curl -X POST "http://localhost:8080/cursos" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Java Basico","descricao":"Fundamentos de Java","cargaHoraria":20}'

curl "http://localhost:8080/cursos"
curl "http://localhost:8080/cursos?nome=jav"
curl -X DELETE "http://localhost:8080/cursos/1"
curl "http://localhost:8080/cursos"
```

Depois do `DELETE`, o registro continua no PostgreSQL, mas nao aparece mais no `GET`, pois a exclusao e logica.

Para executar os testes e conferir a cobertura:

```bash
mvn clean verify
```

O build deve terminar com `BUILD SUCCESS`. Abra `target/site/jacoco/index.html` pelo VS Code para consultar o relatorio. O `pom.xml` faz o build falhar se `CursoService` tiver menos de 100% de cobertura de linhas.

Para parar o ambiente local:

```bash
docker compose down
```

## 5 Preparar a instancia AWS pelo Git Bash

No Security Group da instancia:

- Libere a porta 22 apenas para o seu IP.
- Libere a porta 8080 para os enderecos que devem acessar a API.
- Nao libere a porta 5432 para a internet.

No Git Bash, proteja a chave e conecte. Troque `IP_DA_AWS` pelo IP atual da instancia:

```bash
chmod 400 /c/Users/ezsan/Downloads/projsoft26b.pem
ssh -i /c/Users/ezsan/Downloads/projsoft26b.pem ubuntu@IP_DA_AWS
```

Dentro da instancia, instale o Docker:

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
exit
```

Entre novamente por SSH para a permissao do grupo Docker entrar em vigor.

Crie a rede e o PostgreSQL. Use exatamente o mesmo nome de banco, usuario e senha que voce cadastrara nos GitHub Secrets:

```bash
export DB_NAME="cursosdb"
export DB_USER="cursos"
read -s -p "Senha do PostgreSQL: " DB_PASSWORD
echo

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

unset DB_PASSWORD
docker ps
docker logs postgres-cursos
```

O host interno do banco sera `postgres-cursos`. Nao use `localhost` como `DB_HOST` da aplicacao, pois a API e o banco executam em containers diferentes.

## 6 Criar o repositorio no Docker Hub

1. Entre no Docker Hub.
2. Crie um repositorio chamado `cursos-api`.
3. Deixe o repositorio publico para a AWS poder baixar a imagem sem outra credencial.
4. Abra `Account settings`, depois `Personal access tokens`.
5. Gere um token com permissao de leitura e escrita.
6. Guarde o token; ele sera o valor do secret `DOCKERHUB_TOKEN`.

O token do Docker Hub nunca deve ser salvo no projeto.

## 7 Criar os GitHub Secrets

Abra o repositorio `EnzoS-5/exercicio` no GitHub e acesse:

```text
Settings > Secrets and variables > Actions > Secrets > New repository secret
```

Crie um secret por vez:

| Nome | Valor |
| --- | --- |
| `DOCKERHUB_USERNAME` | Seu nome de usuario no Docker Hub |
| `DOCKERHUB_TOKEN` | O token criado no Docker Hub |
| `HOST_TEST` | IP publico ou DNS da instancia AWS, sem `ubuntu@` |
| `SSH_USER` | `ubuntu` |
| `KEY_TEST` | Todo o conteudo do arquivo `projsoft26b.pem` |
| `DB_HOST` | `postgres-cursos` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `cursosdb`, ou o nome usado na AWS |
| `DB_USER` | `cursos`, ou o usuario usado na AWS |
| `DB_PASSWORD` | Exatamente a senha usada ao criar o PostgreSQL na AWS |

Para visualizar a chave no Git Bash e copia-la para `KEY_TEST`:

```bash
cat /c/Users/ezsan/Downloads/projsoft26b.pem
```

Copie inclusive as linhas `BEGIN` e `END`. Nao envie essa saida em mensagem, commit, captura de tela ou arquivo do repositorio.

O `GITHUB_TOKEN` nao precisa ser criado. O proprio GitHub gera esse token temporario durante o workflow.

Depois de salvar um secret, o GitHub nao permite visualizar novamente seu valor. Ele apenas pode ser substituido.

## 8 Enviar a base para a branch main

Configure sua identidade do Git uma vez:

```bash
git config --global user.name "Enzo"
git config --global user.email "SEU_EMAIL_DO_GITHUB"
```

Antes do primeiro commit, confira que nenhum segredo foi incluido:

```bash
git status
git diff -- . ':!.env'
git check-ignore .env
```

Para cumprir literalmente a exigencia do professor, a branch `main` inicial deve conter apenas duas rotas. O projeto entregue ja traz a implementacao completa e o arquivo `ROTA-DELETE.patch`, que permite retirar temporariamente a rota DELETE da base e aplica-la novamente na branch do Pull Request.

Retire a rota DELETE da copia local antes de criar a `main`:

```bash
git apply -R ROTA-DELETE.patch
mvn clean verify
git add . ':!ROTA-DELETE.patch'
git commit -m "feat: cria API de cursos com testes e CI"
git branch -M main
git push -u origin main
```

## 9 Criar a rota DELETE por Pull Request

Crie a branch no Git Bash:

```bash
git checkout -b feature/delete-curso
git apply ROTA-DELETE.patch
git status
git diff
mvn clean verify
git add src
git commit -m "feat: adiciona exclusao logica de cursos"
git push -u origin feature/delete-curso
rm ROTA-DELETE.patch
```

O patch adiciona `@DeleteMapping("/{id}")` no controller, o metodo de exclusao logica no service, a consulta do repository e os testes unitarios e de integracao correspondentes.

No GitHub:

1. Clique em `Compare and pull request`.
2. Confirme `base: main` e `compare: feature/delete-curso`.
3. Crie o Pull Request.
4. Abra a aba `Checks` ou `Actions`.
5. Aguarde o workflow `Testes` terminar.
6. So faca o merge quando o job `tests` estiver verde.

Esse Pull Request executa `.github/workflows/tests.yml`. O comando `mvn clean verify` testa as rotas e valida a cobertura do service.

## 10 Entender o deploy automatico

Depois do merge, ocorre um `push` na `main`, que executa `.github/workflows/deploy.yml`:

1. O GitHub executa todos os testes.
2. Se os testes passarem, cria a imagem Docker.
3. Entra no Docker Hub usando `DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN`.
4. Publica `seu-usuario/cursos-api`.
5. Conecta na AWS usando `HOST_TEST`, `SSH_USER` e `KEY_TEST`.
6. Inicia a API usando `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD`.

Nenhum desses valores sensiveis fica gravado no repositorio.

## 11 Verificar o deploy

No GitHub, abra `Actions > Deploy AWS` e confirme que os jobs `tests` e `deploy` ficaram verdes.

Teste pelo Git Bash:

```bash
curl "http://IP_DA_AWS:8080/cursos"
```

Se houver erro, entre na instancia:

```bash
ssh -i /c/Users/ezsan/Downloads/projsoft26b.pem ubuntu@IP_DA_AWS
docker ps --all
docker logs cursos-api
docker logs postgres-cursos
```

## 12 Checklist final

- `.env` esta ignorado pelo Git.
- Nenhuma senha, chave ou IP aparece no codigo.
- Os nove GitHub Secrets foram cadastrados.
- O PostgreSQL esta na rede Docker `rede` com o nome `postgres-cursos`.
- `mvn clean verify` termina com sucesso.
- A rota DELETE entrou por Pull Request.
- O workflow `Testes` ficou verde antes do merge.
- O workflow `Deploy AWS` ficou verde depois do merge.
- A API responde pela porta 8080 da AWS.
