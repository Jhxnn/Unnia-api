# Imagens das postagens no Supabase Storage

As imagens das postagens ficam num **bucket público** do Supabase. O navegador manda a imagem para a API do Unnia;
a API confere o arquivo (tipo real e tamanho) e o guarda no bucket com a **chave de serviço**, que nunca sai do
servidor. A leitura é direta pela URL pública do bucket.

```
navegador --(POST /api/media, JWT)--> API Unnia --(service_role)--> Supabase Storage
navegador <------------- URL pública da imagem (leitura) ------------------- bucket
```

## 1. Criar o projeto e o bucket

1. Em https://supabase.com crie um projeto (o plano gratuito serve para começar).
2. Abra **SQL Editor**, cole o script abaixo e execute. Ele cria o bucket público com limite de 5 MB e só aceita
   imagens (o mesmo que a API já valida; é uma segunda trava):

```sql
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'unnia-media', 'unnia-media', true, 5242880,
  array['image/jpeg', 'image/png', 'image/webp', 'image/gif']
)
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;
```

Não é preciso criar políticas (RLS): o envio usa a chave `service_role`, que as ignora, e a leitura de bucket público
é aberta por natureza. **Não** crie política de escrita para `anon`.

## 2. Pegar as credenciais

Em **Project Settings > API**:

| Dado | Onde está | Variável |
|---|---|---|
| Project URL | "Project URL" | `UNNIA_SUPABASE_URL` (por exemplo `https://abcd1234.supabase.co`) |
| Chave `service_role` | "Project API keys", a **secret** | `UNNIA_SUPABASE_SERVICE_KEY` |
| Nome do bucket | o do passo 1 | `UNNIA_SUPABASE_BUCKET` (padrão `unnia-media`) |

> A chave `service_role` dá acesso total ao projeto. Ela só pode estar no `.env` do servidor (que não vai para o
> git) ou nos segredos da hospedagem. Nunca a coloque no front-end, em commits ou em conversas. Se vazar, gere outra
> em **Project Settings > API**.

## 3. Configurar e subir

Preencha no `.env` (copie de `.env.example`):

```
UNNIA_SUPABASE_URL=https://abcd1234.supabase.co
UNNIA_SUPABASE_SERVICE_KEY=<chave service_role>
UNNIA_SUPABASE_BUCKET=unnia-media
```

Depois: `docker compose --profile app up -d`. Sem essas variáveis o envio de imagens fica desligado e responde
`503` ("O envio de imagens ainda não está configurado."); o resto do aplicativo funciona normalmente.

## 4. Como funciona

- `POST /api/media` (autenticado, `multipart/form-data`, campo `file`) devolve `{ "url": "...", "type": "IMAGE" }`.
- A API descobre o formato pelos **primeiros bytes** do arquivo (JPEG, PNG, WebP ou GIF), não pelo nome nem pelo
  `Content-Type` enviado. Outros formatos e arquivos acima de 5 MB são recusados.
- Os arquivos ficam em `posts/<id da conta>/<id aleatório>.<extensão>`.
- Ao publicar, `media` só aceita URLs desse bucket e do tipo `IMAGE`: não dá para anexar imagem de terceiros.
- Cada postagem aceita no máximo 4 imagens.

## 5. Limites conhecidos

- Excluir uma postagem **não** apaga os arquivos do bucket (ficam órfãos). Uma rotina de limpeza é um próximo passo.
- O bucket é público: quem tem a URL vê a imagem. Não use para conteúdo privado.
- Não há redimensionamento: a imagem é guardada como enviada (até 5 MB).
- Imagens enviadas e não publicadas também ficam no bucket.
