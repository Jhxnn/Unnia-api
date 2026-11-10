# Casos de teste do Unnia, para execução no navegador

Este documento é o roteiro de testes de ponta a ponta do Unnia. Foi escrito para ser seguido por um agente
(Claude no navegador) ou por uma pessoa: cada caso diz **onde ir**, **o que fazer** e **o que deve acontecer**.
Os textos entre aspas são os que aparecem na tela.

## Como usar (instruções para o agente)

1. Faça o **Preparo do ambiente** abaixo antes de qualquer caso.
2. Execute os casos na ordem dos grupos. Os grupos **A a D** criam as contas e os dados que os demais usam.
3. Para cada caso, registre **PASSOU**, **FALHOU** ou **BLOQUEADO** (quando um caso anterior de que ele depende falhou), com uma frase de evidência. Em caso de falha, anote o que apareceu na tela, a URL e a mensagem de erro.
4. Não pule a verificação do resultado esperado: um clique que "deu certo" sem a mensagem esperada é falha.
5. Se aparecer **"Muitas requisições"** ou **429**, espere o tempo indicado na tela; é o limite de requisições (10 por minuto nas rotas de login e cadastro) e não é defeito.
6. Ao final, entregue o **Resumo** no formato da última seção.

Prioridade: **P0** = fluxo essencial do produto (se falhar, a entrega não está pronta); **P1** = importante;
**P2** = complementar.

## Preparo do ambiente

| Item | Valor |
|---|---|
| Site | http://localhost:8081 |
| API | http://localhost:8080 (o site repassa `/api` para ela; não precisa abrir) |
| Caixa de e-mail de teste (Mailpit) | http://localhost:8025 |
| Administrador | `admin@unnia.com`; a senha é o valor de `UNNIA_ADMIN_PASSWORD` no arquivo `.env` |

**Condição obrigatória para os grupos A, B e C:** o e-mail de confirmação precisa chegar no Mailpit. No `.env` as
variáveis devem estar assim (valores do `.env.example`) e a API reiniciada com `docker compose --profile app up -d`:

```
UNNIA_MAIL_HOST=mailpit
UNNIA_MAIL_PORT=1025
UNNIA_MAIL_USER=
UNNIA_MAIL_PASSWORD=
UNNIA_MAIL_AUTH=false
UNNIA_MAIL_STARTTLS=false
```

Se o `.env` estiver apontando para o Gmail e a rede bloquear o envio, o cadastro cria a conta mas **nenhum código
chega**: os grupos A a C ficam **BLOQUEADOS** e isso deve ser dito no resumo (não é falha do produto).

**Lendo o código de confirmação:** abra http://localhost:8025, clique na mensagem mais recente para o e-mail
cadastrado. O assunto é `Seu código do Unnia: NNNNNN` e o mesmo número de 6 dígitos aparece em destaque no corpo.

**Dados de teste.** Use e-mails únicos para não colidir com execuções anteriores. Convenção: troque `HHMM` pela hora
atual (por exemplo `1432`).

| Papel | E-mail | Senha |
|---|---|---|
| Voluntária 1 | `maria.HHMM@exemplo.com` (nome "Maria Teste") | `senha1234` |
| Voluntário 2 | `joao.HHMM@exemplo.com` (nome "João Teste") | `senha1234` |
| ONG | `ong.HHMM@exemplo.com` (nome "ONG Teste HHMM") | `senha1234` |

CEPs reais para os testes de endereço:

| CEP | Resultado esperado do preenchimento |
|---|---|
| `50030-230` | Cidade **Recife**, estado **PE** (rua "Cais do Apolo") |
| `01310-100` | Cidade **São Paulo**, estado **SP**, rua "Avenida Paulista", bairro "Bela Vista" |
| `99999-999` | "CEP não encontrado" (não existe) |

CNPJs válidos (use um por ONG; se aparecer "CNPJ já cadastrado", use o próximo):
`39.572.435/0001-46`, `39.677.164/0001-93`, `39.781.893/0001-95`, `39.886.622/0001-02`, `39.991.351/0001-47`,
`40.096.080/0001-45`, `40.200.809/0001-81`, `40.305.538/0001-29`.

Dica para campos de data e hora: use o seletor do navegador ou digite `dd/mm/aaaa hh:mm` conforme o campo aceitar.
Eventos devem começar **no futuro** (por exemplo, daqui a 7 dias).

---

## Grupo A. Cadastro e confirmação de e-mail

### TC-A01 (P0). Cadastro de voluntário com CEP e confirmação por código
**Onde:** http://localhost:8081/cadastro
1. Clique em "Criar conta de voluntário →" (cartão "Sou voluntário"). A URL passa a ser `/cadastro/voluntario`.
2. Preencha **Nome** = `Maria Teste`, **E-mail** = `maria.HHMM@exemplo.com`, **Senha** = `senha1234`, **Confirmar senha** = `senha1234`.
3. No campo **CEP** digite `50030230`.
4. Aguarde ~2 segundos.

**Esperado:** o CEP vira `50030-230`; **Cidade** = `Recife`; **Estado** = `PE`; aparece a dica "Endereço encontrado. Confira e complete o número."

5. Clique em **Criar conta**.

**Esperado:** aviso "Conta criada. Falta só confirmar o e-mail."; a URL vira `/confirme-seu-email?email=...`; a página mostra "Enviamos um código de 6 números para **maria.HHMM@exemplo.com**".

6. Abra o Mailpit em outra aba e leia o código da mensagem mais recente.
7. Volte à aba do site, digite o código em **Código de confirmação** e clique em **Confirmar e-mail**.

**Esperado:** aviso "E-mail confirmado. Agora é só entrar."; redireciona para `/entrar`.

### TC-A02 (P0). Login é bloqueado até confirmar o e-mail
Use uma conta recém-criada **sem** confirmar (crie outra voluntária como no TC-A01, parando no passo 5).
**Onde:** http://localhost:8081/entrar
1. Informe o e-mail e a senha da conta não confirmada e clique em **Entrar**.

**Esperado:** o app leva para `/confirme-seu-email?email=<o e-mail digitado>` (não entra na conta e não mostra erro genérico).

### TC-A03 (P0). Código errado, código reaproveitado e limite de tentativas
**Onde:** `/confirme-seu-email?email=<conta não confirmada>`
1. Digite `000000` e clique em **Confirmar e-mail**.

**Esperado:** mensagem **"Código inválido ou expirado"** no campo; continua na mesma página.

2. Repita com códigos errados mais 4 vezes (5 erros no total).
3. Agora digite o código **correto** do e-mail e confirme.

**Esperado:** continua recusado ("Código inválido ou expirado"), pois o código foi invalidado após 5 erros.

4. Clique em **Reenviar código**. Leia o código **novo** no Mailpit, digite e confirme.

**Esperado:** aviso "Pronto. Se houver uma conta com esse e-mail, enviamos um código novo."; o botão vira "Reenviar em N s" com contagem de ~60 s; o código novo confirma e leva para `/entrar`.

### TC-A04 (P1). Campo do código aceita só números, 6 no máximo
**Onde:** `/confirme-seu-email?email=qualquer@exemplo.com`
1. Digite `48a29-13999` no campo do código.

**Esperado:** o campo mostra exatamente `482913`. O botão **Confirmar e-mail** só fica habilitado com 6 números.

2. Confirme que **não existe** na página nenhum link ou botão "Já confirmei".

### TC-A05 (P1). Confirmação de senha no cadastro
**Onde:** `/cadastro/voluntario` e `/cadastro/ong`
1. Preencha tudo corretamente, mas com **Senha** = `senha1234` e **Confirmar senha** = `senha9999`. Clique no botão de cadastro.

**Esperado:** erro vermelho "A confirmação precisa ser igual à senha." no campo de confirmação; **nenhum** cadastro é criado (continua na mesma página).

### TC-A06 (P1). Validações do cadastro
**Onde:** `/cadastro/voluntario`
1. Deixe o **CEP** vazio e clique em **Criar conta**.

**Esperado:** "Informe o CEP para preencher o endereço."

2. Digite o CEP `5003` (incompleto).

**Esperado:** "O CEP tem 8 números."

3. Use um e-mail já cadastrado (o do TC-A01) com os demais campos válidos.

**Esperado:** erro de e-mail já cadastrado exibido na tela (cadastro não é criado).

### TC-A07 (P1). CEP inexistente e preenchimento manual
**Onde:** `/cadastro/voluntario`
1. Digite o CEP `99999999`.

**Esperado:** "CEP não encontrado. Confira os números ou preencha o endereço à mão." Os campos **Cidade** e **Estado** continuam editáveis.

2. Digite `Recife` em **Cidade** e escolha `PE` em **Estado**; o formulário volta a aceitar o envio (desde que CEP seja válido de 8 números).

---

## Grupo B. Login, sessão e proteção de páginas

### TC-B01 (P0). Login e logout
**Onde:** `/entrar`
1. Entre com a voluntária confirmada no TC-A01.

**Esperado:** vai para a página inicial; o cabeçalho mostra "Início", "Eventos", "ONGs", "Minhas inscrições", "Mensagens", o sino de notificações, o nome da pessoa e **Sair**.

2. Clique em **Sair**.

**Esperado:** volta à página inicial de visitante, com "Entrar" e "Criar conta" no cabeçalho.

### TC-B02 (P0). Credenciais inválidas
**Onde:** `/entrar`
1. Informe um e-mail existente com senha errada e clique em **Entrar**.

**Esperado:** mensagem de erro no formulário ("Credenciais inválidas" ou equivalente); continua em `/entrar`.

### TC-B03 (P0). Páginas protegidas para visitante
Sem estar logado, abra cada URL:
- `/inicio`, `/mensagens`, `/notificacoes`, `/perfil`, `/inscricoes`, `/ong/eventos`, `/admin`.

**Esperado:** todas redirecionam para `/entrar?next=...` (a rota pedida fica codificada no `next`). Após fazer login, volta para a página que foi pedida.

### TC-B04 (P0). Páginas restritas por papel
1. Logada como **voluntária**, abra `/admin` e `/ong/eventos`.
2. Logada como **ONG**, abra `/inscricoes` e `/admin`.

**Esperado:** em todos os casos o app volta para a página inicial (`/`), sem mostrar o conteúdo restrito. O cabeçalho do voluntário não tem "Meus eventos" nem "Painel da equipe".

### TC-B05 (P1). Sessão sobrevive a recarregar a página
1. Logada, recarregue a página (F5) em `/inicio`.

**Esperado:** continua logada (não volta ao login). Depois de **Sair**, recarregar `/inicio` leva ao login.

---

## Grupo C. ONG: cadastro, verificação pela equipe e eventos

### TC-C01 (P0). Cadastro de ONG e confirmação de e-mail
**Onde:** `/cadastro/ong` ("Cadastrar minha ONG")
1. Preencha **Nome da ONG** = `ONG Teste HHMM`, **CNPJ** = um da lista, **E-mail de acesso** = `ong.HHMM@exemplo.com`, **Senha** e **Confirmar senha** = `senha1234`, **CEP** = `50030230`, e **Sobre a ONG** (opcional).
2. Clique em **Cadastrar ONG**.

**Esperado:** "ONG cadastrada. Falta só confirmar o e-mail."; vai para `/confirme-seu-email`; o código chega no Mailpit; confirmar com o código leva a `/entrar`.

### TC-C02 (P0). ONG não verificada cria rascunho, mas não publica
1. Entre com a ONG. Clique em "Meus eventos" → **Criar evento** (ou "Criar o primeiro evento").
2. Preencha o evento (veja TC-D01) e clique em **Salvar rascunho**.
3. Na página do evento (status "Rascunho"), observe o painel "GERENCIAR EVENTO".

**Esperado:** a mensagem "Você pode publicar este rascunho assim que a equipe do Unnia verificar sua ONG." aparece e o botão **Publicar evento** está **desabilitado**. Em "Meus eventos" aparece o aviso de que a ONG aguarda verificação.

### TC-C03 (P0). Administrador verifica a ONG
1. Saia e entre como **administrador**. O cabeçalho deve mostrar **Painel da equipe** (com selo numérico se houver pendências).
2. Abra **Painel da equipe**. Na aba **ONGs** → **Pendentes**, localize "ONG Teste HHMM": devem aparecer CNPJ, cidade com estado ("Recife - PE"), e-mail e descrição.
3. Clique em **Verificar**.

**Esperado:** aviso "ONG verificada."; a ONG some de "Pendentes" e aparece em **Verificadas**.

4. Entre de novo como a ONG e abra o rascunho.

**Esperado:** o botão **Publicar evento** agora está habilitado (amarelo).

### TC-C04 (P1). Administrador recusa ONG (com confirmação)
1. Cadastre e confirme uma segunda ONG (outro CNPJ).
2. Como administrador, em **Pendentes** clique em **Recusar**.

**Esperado:** o botão vira **Confirmar recusa** (nada acontece ainda). Clique em **Voltar** e a recusa é cancelada. Clique em **Recusar** e depois **Confirmar recusa**: aviso "ONG recusada."; a ONG aparece em **Recusadas** (com opção **Verificar** de novo).

### TC-C05 (P1). Suspender e reativar conta
1. No painel, em uma ONG, clique em **Suspender conta** → **Confirmar suspensão**.

**Esperado:** aviso "Conta suspensa."; a ONG ganha a etiqueta "Conta suspensa" e o botão vira **Reativar conta**.

2. Tente entrar com a conta suspensa.

**Esperado:** o login é recusado (mensagem de conta suspensa ou credenciais inválidas).

3. Clique em **Reativar conta**.

**Esperado:** aviso "Conta reativada."; o login volta a funcionar.

---

## Grupo D. Eventos

(Use a ONG **verificada** do TC-C03 e as duas voluntárias.)

### TC-D01 (P0). Criar evento com endereço pelo CEP
**Onde:** `/ong/eventos/novo` (logada como ONG verificada)
1. Preencha **Título** = `Feira de doações`, **Descrição** (opcional).
2. No **CEP** digite `01310100` e aguarde.

**Esperado:** **Endereço** = `Avenida Paulista`, **Bairro** = `Bela Vista`, **Cidade** = `São Paulo`, **Estado** = `SP`.

3. Preencha **Número** = `1578`, **Complemento** = `galpão 2`, **Início** e **Fim** no futuro (fim depois do início), **Vagas** = `2`.
4. Clique em **Salvar rascunho**.

**Esperado:** aviso "Rascunho criado. Publique quando estiver pronto."; a página do evento mostra "Avenida Paulista, 1578, galpão 2 - Bela Vista" e "São Paulo - SP", status "Rascunho", e as vagas "0/2".

### TC-D02 (P0). Publicar evento e vê-lo na lista pública
1. Na página do rascunho, clique em **Publicar evento**.

**Esperado:** aviso "Evento publicado. Já aceita inscrições."; o status muda para aberto.

2. Saia e abra `/eventos` como visitante.

**Esperado:** o evento "Feira de doações" aparece na lista, com a cidade "São Paulo - SP" e a fileira de vagas.

3. Digite `Recife` no filtro "Filtrar por cidade".

**Esperado:** o evento (que é de São Paulo) some da lista, com o aviso "Nada em “Recife” por enquanto"; limpar o filtro o traz de volta.

### TC-D03 (P0). Inscrição de voluntário, lotação e cancelamento
1. Como **visitante**, abra o evento.

**Esperado:** o painel mostra **Entrar para se inscrever** (não **Inscrever-se**).

2. Entre como **Maria** e abra o evento. Clique em **Inscrever-se**.

**Esperado:** aviso "Inscrição confirmada. Sua vaga está reservada."; vagas "1/2"; o botão vira **Cancelar inscrição**; o evento aparece em "Minhas inscrições".

3. Entre como **João** e inscreva-se também (vagas "2/2").
4. Entre com uma **terceira** conta voluntária (ou crie outra) e abra o evento.

**Esperado:** aparece **Evento lotado** (sem opção de inscrição).

5. Como **Maria**, clique em **Cancelar inscrição** → **Sim, cancelar minha vaga**.

**Esperado:** aviso "Inscrição cancelada. A vaga foi liberada."; vagas voltam a "1/2".

### TC-D04 (P0). ONG vê os inscritos
1. Entre como a ONG dona e abra o evento.

**Esperado:** a seção **Inscritos** lista os inscritos ativos (nome, cidade e quando se inscreveram) com o resumo "N inscritos · 0 presenças". Maria (que cancelou) **não** aparece.

2. Entre como Maria ou João e abra o mesmo evento.

**Esperado:** a seção **Inscritos** **não** aparece para voluntários.

### TC-D05 (P1). Editar, remarcar e mudar vagas
**Onde:** página do evento → botão **Editar evento** (`/ong/eventos/:id/editar`)
1. Em **Detalhes**, mude o **Título** e clique em **Salvar detalhes**.

**Esperado:** aviso "Detalhes atualizados."; a página do evento mostra o título novo.

2. Em **Horário**, mude início e fim para outro dia e clique em **Remarcar**.

**Esperado:** aviso "Horário atualizado."; se houver inscritos, o formulário diz quantas pessoas serão avisadas.

3. Em **Vagas**, informe um número **menor** que o de inscritos e clique em **Salvar vagas**.

**Esperado:** erro "Já há N inscritos: as vagas não podem ser menos que isso." (nada é salvo). Com um número maior, aviso "Vagas atualizadas.".

4. Em **Horário**, ponha o **Fim** antes do **Início**.

**Esperado:** "O fim precisa ser depois do início."

### TC-D06 (P1). Presença, encerramento e cancelamento do evento
Crie um evento publicado que comece em poucos minutos (ou aguarde o início de um existente), com Maria inscrita.
1. Depois do horário de início, abra o evento como a ONG.

**Esperado:** na seção **Inscritos** o botão **Registrar presença** está habilitado. Antes do início ele fica desabilitado com a nota "A presença pode ser registrada quando o evento começar."

2. Clique em **Registrar presença** de Maria.

**Esperado:** aviso "Presença registrada."; Maria ganha o selo "Presença registrada" e o resumo mostra "1 presenças".

3. Em outro evento publicado, clique em **Cancelar evento** → **Sim, cancelar o evento**.

**Esperado:** aviso "Evento cancelado. Os inscritos foram avisados."; os inscritos recebem uma notificação (ver TC-G04).

4. Depois do horário de término de um evento, clique em **Encerrar evento**.

**Esperado:** aviso "Evento encerrado.".

### TC-D07 (P1). Regras do evento
- Evento em **rascunho** aberto por URL como visitante: não deve ser exibido (não encontrado).
- Voluntária tenta inscrever-se em evento que **já começou**: o painel mostra "O evento já começou, então as inscrições estão encerradas." sem botão de inscrição.
- ONG e administrador não veem botão de inscrição (nota "Contas de ONG e de administração não se inscrevem em eventos." ou ausência do botão).

---

## Grupo E. Feed, postagens e interações

### TC-E01 (P0). Publicar, editar e excluir uma postagem
**Onde:** `/inicio` (logada)
1. Em "O que você quer compartilhar?" escreva `Primeira publicação de teste` e clique em **Publicar**.

**Esperado:** aviso "Publicado."; a postagem aparece no topo do feed com o nome e a inicial da pessoa e "agora".

2. Na postagem, clique em **Editar**, mude o texto, clique em **Salvar**.

**Esperado:** aviso "Postagem atualizada."; o texto novo aparece com a marca "· editado" junto do horário.

3. Clique em **Excluir** → **Confirmar exclusão**.

**Esperado:** aviso "Postagem excluída."; some do feed.

### TC-E02 (P0). Reações
1. Em uma postagem de **outra pessoa**, clique em **Curtir**.

**Esperado:** o botão fica amarelo (marcado) e o contador sobe ("1 reação").

2. Clique em **Adorei**.

**Esperado:** a reação troca (só **Adorei** marcado); contador continua em 1.

3. Clique de novo em **Adorei**.

**Esperado:** a reação é removida ("0 reações").

### TC-E03 (P0). Comentar e responder
1. Em uma postagem, clique em "N comentários" para abrir a seção. Digite em "Escreva um comentário" e clique em **Enviar**.

**Esperado:** o comentário aparece com o nome do autor; o contador sobe.

2. Com **outra conta**, abra a mesma postagem, clique em **Responder** no comentário, escreva e envie.

**Esperado:** a resposta aparece aninhada abaixo do comentário. Respostas **não** têm botão "Responder".

3. O autor do comentário vê **Excluir** nos seus comentários e **não** vê "Denunciar" neles; quem não é o autor vê **Denunciar** e não vê **Excluir**.

### TC-E04 (P1). ONG divulga um evento no feed
1. Como ONG verificada com um evento **publicado**, em `/inicio` o formulário mostra o seletor **Divulgar um evento** com esse evento (rascunhos e cancelados **não** aparecem).
2. Escolha o evento, escreva o texto e **Publicar**.

**Esperado:** a postagem mostra o atalho "Ver o evento", que leva à página do evento.

3. Como voluntária, o formulário **não** tem o seletor "Divulgar um evento".

### TC-E05 (P1). Feed vazio e lista de seguidos
1. Entre com uma conta nova que não segue ninguém e não publicou.

**Esperado:** "Seu feed está vazio" com o botão **Conhecer ONGs** (leva a `/ongs`).

2. Depois de seguir uma ONG que publicou (TC-F01), as postagens dela aparecem no feed.

---

## Grupo F. Perfis, seguir e mensagens

### TC-F01 (P0). Vitrine de ONGs e seguir
**Onde:** `/ongs`
1. Abra a vitrine.

**Esperado:** só ONGs **verificadas** aparecem (a ONG pendente/recusada não). Cada cartão mostra nome, "Cidade - UF" e descrição.

2. Abra uma ONG (`/ongs/:id`). Como **visitante**, o perfil mostra o botão "Entrar para seguir e conversar" e **não** mostra publicações.
3. Logada como voluntária, clique em **Seguir**.

**Esperado:** aviso "Agora você segue."; o botão vira **Seguindo** e o contador de seguidores sobe. Clique em **Seguindo** para deixar de seguir ("Você deixou de seguir.").

### TC-F02 (P0). Mensagens entre voluntária e ONG
1. No perfil da ONG, como voluntária, clique em **Enviar mensagem**. A URL é `/mensagens?para=...&nome=...`.

**Esperado:** aparece "Nova conversa com <nome da ONG>". Escreva e clique em **Enviar**; a conversa é criada e aparece na lista com a mensagem enviada alinhada à direita.

2. Entre como a ONG.

**Esperado:** o item **Mensagens** do menu mostra um selo vermelho com "1"; a conversa mostra um selo de não lidas. Abra a conversa: o selo some (marcada como lida). Responda e envie.

3. Volte como a voluntária: a resposta aparece na conversa (pode levar até ~10 segundos para atualizar sozinha, ou recarregue).

### TC-F03 (P1). Perfil de voluntário e edição do próprio perfil
1. Em `/perfil` (logada), altere **Nome** e **Sobre você** e clique em **Salvar perfil**.

**Esperado:** aviso "Perfil atualizado."; o nome novo aparece no cabeçalho.

2. Mude o **CEP** para `01310100` e aguarde o preenchimento: **Cidade** = `São Paulo`, **Estado** = `SP`. Salve. Abra o perfil público e confira "São Paulo - SP".

3. Abra o perfil público de **outra** pessoa: aparecem os botões **Seguir**, **Enviar mensagem** e **Denunciar perfil**. No **próprio** perfil aparece **Editar meu perfil** e nenhum desses três.

### TC-F04 (P1). Trocar a senha
**Onde:** `/perfil`, seção "Senha"
1. Informe uma **Senha atual errada**, uma nova senha e a confirmação, e clique em **Trocar senha**.

**Esperado:** erro "Senha atual incorreta" no campo da senha atual; continua logada.

2. Informe a senha atual correta, `novaSenha456` e a confirmação **diferente**.

**Esperado:** "A confirmação precisa ser igual à nova senha." (nada é enviado).

3. Informe tudo correto.

**Esperado:** aviso "Senha alterada. Entre novamente com a nova senha."; vai para `/entrar` (sessão encerrada). O login com a senha **antiga** falha; com a **nova** funciona.

---

## Grupo G. Notificações

### TC-G01 (P0). Sino, contagem e leitura
1. Gere atividade para a ONG: com uma voluntária, **siga** a ONG, **reaja** e **comente** em uma postagem dela, e **inscreva-se** em um evento dela.
2. Entre como a ONG.

**Esperado:** o sino do cabeçalho mostra um selo vermelho com a quantidade. Abra `/notificacoes`: aparecem itens como "Você tem um novo seguidor", "Alguém reagiu ao seu conteúdo", "Alguém comentou na sua postagem" e "Um voluntário se inscreveu no seu evento", os não lidos com destaque amarelo.

3. Clique em uma notificação de comentário.

**Esperado:** abre a postagem (`/posts/:id`); ao voltar, essa notificação aparece como lida e o contador do sino diminuiu em 1.

4. Clique em **Marcar todas como lidas**.

**Esperado:** aviso "Tudo marcado como lido."; o selo do sino some e o botão desaparece.

### TC-G02 (P1). Notificação de evento cancelado e de ONG verificada
- A voluntária inscrita em um evento que a ONG cancelou recebe uma notificação, que leva à página do evento.
- A ONG verificada pelo administrador recebe "Sua ONG foi verificada e já pode publicar eventos", que leva a `/ong/eventos`.
- A notificação "novo seguidor" **não** leva a lugar nenhum (é um botão que só marca como lida).

### TC-G03 (P2). Sem notificações
- Uma conta nova abre `/notificacoes`.

**Esperado:** "Nada por aqui ainda" com a explicação do que aparece ali.

---

## Grupo H. Denúncias e moderação

### TC-H01 (P0). Denunciar e moderar
1. Como voluntária, em uma postagem de **outra** pessoa clique em **Denunciar**.

**Esperado:** abre o formulário com **Motivo** (padrão "Spam") e "Detalhes (opcional)".

2. Escolha "Golpe", escreva um detalhe e clique em **Enviar denúncia**.

**Esperado:** aviso "Denúncia enviada. A equipe vai analisar."

3. Denuncie a **mesma** postagem de novo com a mesma conta.

**Esperado:** erro informando que já foi denunciada (não cria duplicata).

4. Entre como **administrador** → **Painel da equipe** → aba **Denúncias**.

**Esperado:** a denúncia aparece com a etiqueta do motivo ("Golpe"), o tipo ("Postagem"), a data, o **trecho do texto denunciado** em destaque e o detalhe escrito pela pessoa. O selo do menu conta as pendências.

5. Clique em **Remover postagem** → **Confirmar remoção**.

**Esperado:** aviso "Denúncia resolvida."; a denúncia some da fila e a postagem **desaparece do feed** (acessar `/posts/:id` mostra "Postagem indisponível").

### TC-H02 (P1). Descartar denúncia e tipos de alvo
1. Denuncie outra postagem, um **comentário** e um **perfil** ("Denunciar perfil").
2. No painel, em cada denúncia os botões mudam conforme o alvo: "Remover postagem", "Remover comentário" e "Suspender conta".
3. Clique em **Descartar denúncia** em uma delas.

**Esperado:** aviso "Denúncia descartada."; o conteúdo **continua** visível.

### TC-H03 (P2). Limites do que se pode denunciar
- Em uma postagem **sua**, não existe botão **Denunciar**.
- Visitante não vê botões de denúncia.

---

## Grupo I. Responsividade e acessibilidade (P2)

### TC-I01. Celular (largura de 390 px)
Redimensione a janela para ~390 px de largura (ou use a emulação de dispositivo) e percorra: `/`, `/eventos`, um evento, `/cadastro/voluntario`, `/inicio`, `/mensagens`.

**Esperado:** nenhuma página tem rolagem horizontal; o menu rola na horizontal dentro da própria barra; o nome da pessoa some do cabeçalho e aparece o link "Meu perfil" na barra de links; em `/mensagens` aparece ou a lista de conversas ou a conversa, nunca as duas lado a lado; os botões de ação dos cartões (por exemplo "Registrar presença") quebram para baixo do nome.

### TC-I02. Teclado
1. Na página inicial, aperte **Tab** uma vez.

**Esperado:** o primeiro foco é o link "Ir para o conteúdo" (visível ao receber foco).

2. Percorra o formulário de login só com **Tab** e envie com **Enter**.

**Esperado:** todos os campos e botões recebem foco visível e o formulário envia.

---

## Resumo a entregar

Ao terminar, responda neste formato:

```
Ambiente: <site e API respondendo? e-mail em Mailpit ou Gmail?>
Total: <N casos> | PASSOU: <n> | FALHOU: <n> | BLOQUEADO: <n>

FALHAS (mais graves primeiro)
- TC-XXX (P0/P1/P2): <o que era esperado> / <o que aconteceu> / <URL> / <mensagem de erro, se houver>

BLOQUEADOS
- TC-XXX: <qual caso anterior falhou ou o que faltou no ambiente>

OBSERVAÇÕES
- <qualquer comportamento estranho que não seja falha de um caso>
```

Os casos **P0** são: A01, A02, A03, B01, B02, B03, B04, C01, C02, C03, D01, D02, D03, D04, E01, E02, E03, F01, F02,
G01 e H01. Se algum deles falhar, destaque no início do resumo.
