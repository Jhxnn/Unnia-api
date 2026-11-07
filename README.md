# Unnia API

Rede social para ONGs divulgarem e administrarem eventos solidários, e para voluntários se inscreverem
neles e conversarem com as ONGs. Backend em Java 21 e Spring Boot 4, com PostgreSQL.

## Arquitetura

DDD com um módulo por *bounded context*. Cada módulo tem as camadas `domain`, `application` e
`infrastructure`, e a regra de dependência aponta sempre para dentro.

| Contexto | Responsabilidade |
|---|---|
| `identity` | Contas (`NGO`, `VOLUNTEER`, `ADMIN`), perfis, login, sessões e verificação de ONGs |
| `events` | Eventos, ciclo de vida, vagas e inscrições |
| `social` | Postagens, comentários, reações e seguidores |
| `messaging` | Conversas entre ONG e voluntário, e notificações |
| `moderation` | Denúncias e remoção de conteúdo |
| `shared` | Kernel compartilhado: `AggregateRoot`, ids tipados, value objects, exceções |

- **Domínio** em Java puro, sem anotações de framework. Os agregados protegem os próprios invariantes
  (por exemplo, `Event` guarda as inscrições para nunca ultrapassar as vagas) e emitem eventos de domínio.
- **Aplicação**: um caso de uso por classe (`...UseCase`), também sem anotações. Os contextos conversam
  entre si por portas (`NgoDirectory`, `ContentAuthors`, ...) implementadas na camada anticorrupção em
  `shared/infrastructure/acl`.
- **Infraestrutura**: entidades JPA separadas do domínio, com mappers; Flyway para o schema; JWT com
  Spring Security; controllers REST. Os casos de uso são descobertos por convenção e ficam transacionais
  por um *post processor*. Os eventos de domínio são entregues aos handlers (notificações) **depois do commit**,
  cada handler na sua transação.
- **Concorrência**: `events.version` (bloqueio otimista) impede que duas inscrições simultâneas
  estourem as vagas; a segunda recebe `409`.

## Como rodar

Pré-requisitos: JDK 21 e Docker.

```bash
docker compose up -d                       # PostgreSQL na porta 5433
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

O perfil `local` lê `src/main/resources/application-local.yaml`, que **não é versionado**. Crie-o com:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/unnia
    username: unnia
    password: unnia
unnia:
  jwt:
    secret: <string aleatória com 32 ou mais caracteres>
  admin:
    email: admin@unnia.com
    password: <senha do administrador>
```

Em produção, use as variáveis de ambiente `UNNIA_DB_URL`, `UNNIA_DB_USER`, `UNNIA_DB_PASSWORD`,
`UNNIA_JWT_SECRET`, `UNNIA_ADMIN_EMAIL` e `UNNIA_ADMIN_PASSWORD`. Sem `UNNIA_JWT_SECRET` a aplicação não sobe.
O administrador é criado na inicialização, se ainda não existir.

### Com Docker

A API também roda em contêiner (imagem multi-stage, JRE 21, processo sem root):

```bash
export UNNIA_JWT_SECRET=<32 ou mais caracteres> UNNIA_ADMIN_PASSWORD=<senha do admin>
docker compose --profile app up -d     # PostgreSQL + API em http://localhost:8080
```

## Testes

```bash
./mvnw verify
```

São testes unitários, sem Spring nem banco: domínio, casos de uso (com repositórios em memória), mappers,
JWT e controllers. O CI (GitHub Actions) roda a mesma suíte e confere se a imagem Docker constrói.

## Documentação da API

Com a aplicação rodando, a especificação OpenAPI fica em `/v3/api-docs` e a interface interativa em
`/swagger-ui.html`. Para testar rotas protegidas, faça login em `POST /api/auth/login`, copie o `token`
e informe-o no botão **Authorize**.

## Autenticação e sessões

O login devolve dois tokens:

| Token | Duração | Uso |
|---|---|---|
| `token` (JWT) | 15 minutos | Enviado em `Authorization: Bearer <token>` |
| `refreshToken` (opaco) | 30 dias | Trocado por um novo par em `POST /api/auth/refresh` |

- **Rotação**: cada refresh token funciona **uma única vez**; a renovação devolve um par novo.
- **Detecção de roubo**: reapresentar um token já usado revoga a sessão inteira, inclusive o sucessor legítimo,
  e o titular precisa fazer login de novo.
- **Armazenamento**: o banco guarda só o hash SHA-256 do refresh token, nunca o token em si.
- **Encerramento**: `POST /api/auth/logout` revoga a sessão; trocar a senha revoga todas as sessões da conta;
  suspender a conta impede qualquer renovação.

As durações são configuráveis em `unnia.jwt.expiration` e `unnia.refresh.expiration`.

## API

| Área | Rotas |
|---|---|
| Auth | `POST /api/auth/register/ngo`, `/register/volunteer`, `/login`, `/refresh`, `/logout`; `PUT /api/auth/password` |
| Perfis | `GET /api/me`, `/api/ngos/{id}`, `/api/volunteers/{id}`; `PUT /api/ngos/me`, `/api/volunteers/me` |
| Eventos | `POST/GET /api/events`, `GET /api/events/{id}`, `/mine`, `/registered`; `PUT /{id}`, `/{id}/schedule`, `/{id}/capacity`; `POST /{id}/publish`, `/cancel`, `/finish`, `/registrations`, `/attendance/{volunteerId}`; `DELETE /{id}/registrations`; `GET /{id}/registrations` (inscritos, só a ONG dona) |
| Social | `POST /api/posts`, `GET /api/posts/feed`, `/{id}`, `/author/{accountId}`, `/{id}/comments`; `PUT/DELETE /api/posts/{id}`; `POST /api/comments/{id}/replies`; `PUT/DELETE /api/reactions`; `POST/DELETE /api/follows/{accountId}` |
| Mensagens | `POST /api/messages`; `GET /api/conversations`, `/{id}/messages`; `POST /api/conversations/{id}/read` |
| Notificações | `GET /api/notifications`, `/unread-count`; `POST /api/notifications/{id}/read`, `/read-all` |
| Moderação | `POST /api/reports`; `GET /api/admin/reports` (com resumo do alvo); `POST /api/admin/reports/{id}/resolve`, `/dismiss` |
| Admin | `GET /api/admin/ngos?status=PENDING` (fila de revisão); `POST /api/admin/ngos/{id}/verify`, `/reject`; `POST /api/admin/accounts/{id}/suspend`, `/activate` |

Erros seguem o formato Problem Details: `400` entrada inválida, `401` não autenticado, `403` sem permissão,
`404` não encontrado, `409` conflito (inclusive concorrência) e `422` regra de negócio violada.

## Regras de negócio relevantes

- Só ONG **verificada** (por um administrador) publica eventos.
- Rascunhos só são visíveis para a ONG dona.
- Conversas só existem entre uma ONG e um voluntário.
- Suspender uma conta invalida seus tokens de acesso imediatamente: o filtro JWT confere a conta a cada requisição.

## Limite de requisições

Os contadores ficam em memória (token bucket, com cache limitado e expiração), atrás da porta `RateLimiter`:
trocar por Redis para várias réplicas não exige mexer nas regras. Ao exceder, a API responde `429` com o
cabeçalho `Retry-After` (segundos). Todas as respostas trazem `X-RateLimit-Limit` e `X-RateLimit-Remaining`.

| Escopo | Limite padrão | Chave |
|---|---|---|
| Login, refresh, cadastros e troca de senha | 10 por minuto | IP |
| Demais rotas | 120 por minuto | conta autenticada (ou IP, se anônimo) |
| Falhas de login | 5 em 15 minutos | e-mail normalizado |

- E-mail inexistente conta igual a senha errada, para não revelar quais contas existem.
- O bloqueio por e-mail vale mesmo com a senha certa, até a janela acabar. Isso freia força bruta, mas
  permite que alguém bloqueie temporariamente o login de uma conta alheia; é a troca consciente.
- O IP é o `remoteAddr`. Atrás de um proxy reverso, configure `server.forward-headers-strategy`; ativá-lo
  sem proxy permitiria forjar o IP e fugir do limite.
- Configuração em `unnia.rate-limit.*` (`enabled`, `auth-per-minute`, `general-per-minute`,
  `login-max-failures`, `login-window`).

## Front-end

O cliente web fica no projeto irmão `Unnia-web` (React, TypeScript, Vite). Em desenvolvimento ele encaminha
`/api` para esta API (porta 8080), então não precisa de CORS; em produção, sirva os dois na mesma origem.

**Toda mudança aqui que afete o contrato (rotas, DTOs, status HTTP, headers, regras visíveis) deve ser refletida
no front**, em `Unnia-web/src/lib/api/types.ts`, `endpoints.ts` e `client.ts` e, quando for regra de negócio,
em `src/features/events/eventPolicy.ts`.
