# Casos de teste do Unnia, para execução no navegador (onda 2)

Roteiro de testes de ponta a ponta do Unnia, para ser seguido por um agente (Claude no navegador) ou por uma pessoa:
cada caso diz **onde ir**, **o que fazer** e **o que deve acontecer**. Os textos entre aspas são os que aparecem na tela.

Esta é a **segunda onda**. A primeira rodou só como administrador (relatório de 14/11/2026) e deixou casos sem
executar. Aqui estão **apenas** o que ainda não foi provado, o que falhou e o que foi corrigido. Os casos que já
passaram estão listados na seção "Já testado" e **não devem ser repetidos**.

## Como usar (instruções para o agente)

1. Faça o **Preparo do ambiente** abaixo antes de qualquer caso.
2. Execute os casos **na ordem dos grupos**: A, B, C, D, E, F, G, H, I. Alguns criam dados que os seguintes usam.
3. Para cada caso, registre **PASSOU**, **FALHOU** ou **BLOQUEADO** (quando algo de que ele depende não foi possível), com uma frase de evidência. Em falha, anote o que apareceu, a URL e a mensagem de erro.
4. Não pule a verificação do resultado esperado: um clique que "deu certo" sem a mensagem esperada é falha.
5. Se aparecer **"Muitas requisições"** ou **429**, espere o tempo indicado na tela; é o limite de 10 requisições por minuto nas rotas de login e cadastro, não é defeito. Evite repetir logins em sequência: prefira **trocar de conta com Sair e Entrar** só quando o caso pedir.
6. **Nunca apague dados de outras pessoas.** Os casos que removem conteúdo (H01) só mexem no que o próprio teste criou.
7. Ao final, entregue o **Resumo** no formato da última seção.

Prioridade: **P0** = fluxo essencial (se falhar, a entrega não está pronta); **P1** = importante; **P2** = complementar.

## Já testado na onda 1 (não repetir)

| Caso | Resultado | O que ficou provado |
|---|---|---|
| A02 | PASSOU | Login de conta não confirmada leva a `/confirme-seu-email` |
| A04 | PASSOU | Campo do código só aceita números (máx. 6); sem "Já confirmei" |
| A05 | PASSOU | "A confirmação precisa ser igual à senha." nos dois cadastros |
| A06 | PASSOU | CEP vazio, CEP incompleto e e-mail repetido mostram o erro certo |
| A07 | PASSOU | CEP inexistente avisa e deixa preencher à mão |
| B01 | PASSOU | Login e logout (cabeçalho do administrador e do visitante) |
| B02 | PASSOU | Senha errada: "E-mail ou senha incorretos." |
| B03 | PASSOU | Visitante nas páginas protegidas vai para `/entrar?next=...` |
| B05 | PASSOU | Sessão sobrevive ao recarregar |
| C05 | PASSOU | Suspender, login recusado e reativar conta |
| E01 | PASSOU | Publicar, editar ("editado") e excluir postagem |
| I02 | PASSOU | Foco por teclado ("Ir para o conteúdo") e envio com Enter |
| parte de A03 | PASSOU | Código `000000` mostra "Código inválido ou expirado" |
| parte de C03 | PASSOU | O painel mostra CNPJ, cidade, e-mail e descrição; "ONG verificada." |
| parte de C04 | PASSOU | "Recusar" pede "Confirmar recusa"; "Voltar" cancela; "ONG recusada." |
| parte de D07 | PASSOU | O administrador não vê botão de inscrição, só a nota explicativa |
| parte de H02 | PASSOU | "Descartar denúncia" funciona e mostra "Denúncia descartada." |

## O que mudou desde a onda 1 (corrigido, a validar de novo)

- **Falha TC-C04:** em **Recusadas**, "Verificar" dava "Verificação já foi decidida". Agora uma ONG recusada **pode ser
  verificada depois** (a equipe reconsidera). Uma ONG **verificada** continua definitiva, e **"Recusar" só aparece para
  ONGs pendentes**. Validar nos casos C03 e C04.
- **Falha TC-C03/F01 (cidade sem UF):** os registros antigos não tinham estado; foram preenchidos com `PE`. Cidades devem
  aparecer como "Recife - PE". Validar em C03 e F01.
- **Denúncias de postagem** ganharam o link **"Ver postagem"** (abre em outra aba). Antes o administrador não via a
  postagem no próprio feed. Usar em H02.
- **E-mail:** o código **chega no Gmail** configurado no `.env` (não no Mailpit). Veja "Lendo o código" abaixo.
- **Contas de teste** já criadas e confirmadas (tabela abaixo), para os casos não dependerem de e-mail.
- **Tempo real** (SSE) e **imagens em postagens** entraram depois da onda 1: casos G04 e E06.

## Preparo do ambiente

| Item | Valor |
|---|---|
| Site | http://localhost:8081 |
| API | http://localhost:8080 (o site repassa `/api`; não precisa abrir) |
| Administrador | `admin@unnia.com`; a senha é o valor de `UNNIA_ADMIN_PASSWORD` no `.env` do backend |
| Caixa de e-mail de teste (Mailpit) | http://localhost:8025 (**vazia** com a configuração atual: o envio vai para o Gmail) |

**Contas prontas** (todas com e-mail confirmado e senha `senha1234`):

| Papel | E-mail | Nome | Situação |
|---|---|---|---|
| Voluntária | `voluntaria.teste@exemplo.com` | Voluntária Teste | ativa |
| Voluntário | `voluntario.dois@exemplo.com` | Voluntário Dois | ativo |
| ONG verificada | `ong.teste@exemplo.com` | ONG Teste | **verificada** (pode publicar eventos) |
| ONG pendente | `ong.pendente@exemplo.com` | ONG Pendente Teste | **aguardando verificação** (use em C02 e C03) |

**Lendo o código de confirmação.** O envio usa o Gmail `jhonalizan@gmail.com`. Para os casos que **criam contas novas**
(A01, A03 e C01) use endereços com "+" que caem nessa mesma caixa:
`jhonalizan+maria_HHMM@gmail.com`, `jhonalizan+ong_HHMM@gmail.com` (troque `HHMM` pela hora atual, por exemplo `1432`).
O e-mail tem assunto `Seu código do Unnia: NNNNNN` e o mesmo número de 6 dígitos no corpo.
- Se o navegador estiver com o Gmail aberto e logado, leia lá (busque por `Seu código do Unnia`).
- Se não houver acesso ao Gmail, peça o código à pessoa que está acompanhando o teste, ou marque o caso **BLOQUEADO**
  informando que faltou acesso à caixa de entrada. Não é falha do produto.
- Alternativa sem Gmail: trocar no `.env` as variáveis `UNNIA_MAIL_*` pelos valores do `.env.example` (Mailpit),
  rodar `docker compose --profile app up -d` e ler os códigos em http://localhost:8025 (e-mails `@exemplo.com` servem).

CEPs reais: `50030-230` (Recife, PE, "Cais do Apolo") e `01310-100` (São Paulo, SP, "Avenida Paulista", bairro "Bela Vista").
CNPJs válidos (um por ONG nova; se der "CNPJ já cadastrado", use o seguinte): `39.781.893/0001-95`, `39.886.622/0001-02`,
`39.991.351/0001-47`, `40.096.080/0001-45`, `40.200.809/0001-81`, `40.305.538/0001-29`.
(Os dois primeiros da lista antiga, `39.572.435/0001-46` e `39.677.164/0001-93`, já são das contas prontas.)

Eventos devem começar **no futuro** (por exemplo, daqui a 7 dias), a não ser nos casos que pedem um evento já iniciado.

**Conta de apoio descartável.** Para casos destrutivos use contas criadas pelo próprio teste (A01, C01), nunca as
quatro contas prontas, que serão reaproveitadas pelas próximas ondas.

---

## Grupo A. Cadastro e confirmação de e-mail

### TC-A01 (P0). Cadastro de voluntária com CEP e confirmação por código  *(bloqueado na onda 1)*
**Onde:** http://localhost:8081/cadastro
1. Clique em "Criar conta de voluntário →" (`/cadastro/voluntario`).
2. Preencha **Nome** = `Maria Teste`, **E-mail** = `jhonalizan+maria_HHMM@gmail.com`, **Senha** e **Confirmar senha** = `senha1234`.
3. No **CEP** digite `50030230` e aguarde ~2 s.

**Esperado:** CEP vira `50030-230`; **Cidade** = `Recife`; **Estado** = `PE`; dica "Endereço encontrado. Confira e complete o número."

4. Clique em **Criar conta**.

**Esperado:** aviso "Conta criada. Falta só confirmar o e-mail."; URL `/confirme-seu-email?email=...`; texto "Enviamos um código de 6 números para **<o e-mail>**".

5. Leia o código (veja "Lendo o código"), digite em **Código de confirmação** e clique em **Confirmar e-mail**.

**Esperado:** aviso "E-mail confirmado. Agora é só entrar."; vai para `/entrar`. Entrar com a nova conta funciona.

### TC-A03 (P0). Limite de tentativas e reenvio do código  *(parcial: o código errado simples já passou)*
Use uma conta **não confirmada** (a do TC-A01 antes do passo 5, ou crie outra).
**Onde:** `/confirme-seu-email?email=<a conta>`
1. Digite códigos errados **5 vezes** (por exemplo `000001` a `000005`), clicando em **Confirmar e-mail** a cada vez.

**Esperado:** cada vez, "Código inválido ou expirado".

2. Digite agora o código **correto** do e-mail.

**Esperado:** continua recusado ("Código inválido ou expirado"): o código foi invalidado após 5 erros.

3. Clique em **Reenviar código**.

**Esperado:** aviso "Pronto. Se houver uma conta com esse e-mail, enviamos um código novo."; o botão vira "Reenviar em N s" (contagem de ~60 s) e o campo do código é limpo.

4. Leia o código **novo**, digite e confirme.

**Esperado:** confirma e vai para `/entrar`. (O código antigo, se tentado, é recusado.)

---

## Grupo B. Proteção por papel

### TC-B04 (P0). Páginas restritas por papel  *(não executado na onda 1)*
1. Entre como **Voluntária Teste**. Abra `/admin` e depois `/ong/eventos`.

**Esperado:** ambas voltam para a página inicial (`/`), sem mostrar o conteúdo restrito. O cabeçalho **não** tem "Meus eventos" nem "Painel da equipe", e tem "Minhas inscrições".

2. Saia e entre como **ONG Teste**. Abra `/inscricoes` e `/admin`.

**Esperado:** ambas voltam para `/`. O cabeçalho tem "Meus eventos" e **não** tem "Minhas inscrições".

---

## Grupo C. ONG: cadastro, verificação e eventos

### TC-C01 (P0). Cadastro de ONG e confirmação  *(bloqueado na onda 1)*
**Onde:** `/cadastro/ong` ("Cadastrar minha ONG")
1. Preencha **Nome da ONG** = `ONG Nova HHMM`, **CNPJ** = um da lista, **E-mail de acesso** = `jhonalizan+ong_HHMM@gmail.com`, **Senha** e **Confirmar senha** = `senha1234`, **CEP** = `50030230`. Clique em **Cadastrar ONG**.

**Esperado:** "ONG cadastrada. Falta só confirmar o e-mail."; vai para `/confirme-seu-email`. Confirmar com o código leva a `/entrar`. Essa ONG fica **pendente** e é usada em C04.

### TC-C02 (P0). ONG não verificada cria rascunho, mas não publica  *(bloqueado na onda 1)*
1. Entre como **ONG Pendente Teste** (`ong.pendente@exemplo.com`). Em "Meus eventos" aparece o aviso de que a ONG aguarda a verificação.
2. Clique em **Criar evento** e crie um rascunho como no TC-D01 (título `Rascunho de teste`).
3. Na página do evento, observe "GERENCIAR EVENTO".

**Esperado:** a nota "Você pode publicar este rascunho assim que a equipe do Unnia verificar sua ONG." e o botão **Publicar evento** **desabilitado**.

### TC-C03 (P0). Administrador verifica a ONG  *(parcial: o painel e a verificação já passaram; falta a UF e o efeito na ONG)*
1. Entre como **administrador** → **Painel da equipe** → **ONGs** → **Pendentes**. Localize "ONG Pendente Teste".

**Esperado:** a cidade aparece com o estado: **"Recife - PE"** (e o mesmo nos demais cartões, por exemplo "Olinda - PE"). Aparecem CNPJ, e-mail e descrição.

2. Clique em **Verificar**.

**Esperado:** "ONG verificada."; a ONG some de Pendentes e aparece em **Verificadas**.

3. Entre como **ONG Pendente Teste** e abra o rascunho do TC-C02.

**Esperado:** o botão **Publicar evento** agora está **habilitado**. As notificações da ONG (`/notificacoes`) trazem "Sua ONG foi verificada e já pode publicar eventos".

### TC-C04 (P1). Reconsiderar uma ONG recusada  *(falha na onda 1, corrigida)*
1. Como administrador, em **Pendentes**, escolha a ONG criada no TC-C01 e clique em **Recusar** → **Confirmar recusa**. Ela vai para **Recusadas**.
2. Na aba **Recusadas**, a ONG mostra **Verificar** e **não** mostra **Recusar**.
3. Clique em **Verificar**.

**Esperado:** "ONG verificada." (antes dava "Verificação já foi decidida"); a ONG sai de Recusadas e aparece em Verificadas.

4. Na aba **Verificadas**, nenhuma ONG tem **Verificar** nem **Recusar** (só as ações de conta).

---

## Grupo D. Eventos

(Use **ONG Teste** e as duas voluntárias.)

### TC-D01 (P0). Criar evento com endereço pelo CEP
**Onde:** `/ong/eventos/novo` (logada como **ONG Teste**)
1. **Título** = `Feira de doações`. No **CEP** digite `01310100` e aguarde.

**Esperado:** **Endereço** = `Avenida Paulista`, **Bairro** = `Bela Vista`, **Cidade** = `São Paulo`, **Estado** = `SP`.

2. **Número** = `1578`, **Complemento** = `galpão 2`, **Início** e **Fim** no futuro, **Vagas** = `1`.
3. Clique em **Salvar rascunho**.

**Esperado:** "Rascunho criado. Publique quando estiver pronto."; a página mostra "Avenida Paulista, 1578, galpão 2 - Bela Vista", "São Paulo - SP", status "Rascunho" e "0/1".

### TC-D02 (P0). Publicar e ver na lista pública
1. Clique em **Publicar evento**.

**Esperado:** "Evento publicado. Já aceita inscrições."

2. Saia e abra `/eventos` como visitante.

**Esperado:** "Feira de doações" aparece, com "São Paulo - SP" e a fileira de vagas.

3. Filtre por cidade `Recife`.

**Esperado:** o evento some, com "Nada em “Recife” por enquanto"; limpar o filtro o traz de volta.

### TC-D03 (P0). Inscrição, lotação e cancelamento
1. Como **visitante**, abra o evento: aparece **Entrar para se inscrever**.
2. Entre como **Voluntária Teste**, abra o evento e clique em **Inscrever-se**.

**Esperado:** "Inscrição confirmada. Sua vaga está reservada."; vagas "1/1"; o botão vira **Cancelar inscrição**; o evento aparece em "Minhas inscrições".

3. Entre como **Voluntário Dois** e abra o evento.

**Esperado:** aparece **Evento lotado**, sem opção de inscrição.

4. Volte como **Voluntária Teste**, clique em **Cancelar inscrição** → **Sim, cancelar minha vaga**.

**Esperado:** "Inscrição cancelada. A vaga foi liberada."; vagas "0/1". O **Voluntário Dois** agora consegue **Inscrever-se** (faça isso: ele fica inscrito para D04).

### TC-D04 (P0). ONG vê os inscritos
1. Entre como **ONG Teste** e abra o evento.

**Esperado:** a seção **Inscritos** lista **Voluntário Dois** (nome, cidade, quando se inscreveu) com "1 inscritos · 0 presenças". A Voluntária Teste (que cancelou) **não** aparece.

2. Entre como **Voluntária Teste** e abra o mesmo evento.

**Esperado:** a seção **Inscritos** **não** aparece para voluntários.

### TC-D05 (P1). Editar, remarcar e mudar vagas
**Onde:** página do evento → **Editar evento** (`/ong/eventos/:id/editar`)
1. Em **Detalhes**, mude o **Título** e clique em **Salvar detalhes**: "Detalhes atualizados."
2. Em **Horário**, mude para outro dia e clique em **Remarcar**: "Horário atualizado."; o formulário diz quantas pessoas inscritas serão avisadas.
3. Em **Vagas**, informe `0` ou um número menor que o de inscritos e salve.

**Esperado:** erro ("Informe pelo menos 1 vaga." ou "Já há N inscritos: as vagas não podem ser menos que isso."); nada é salvo. Com um número maior: "Vagas atualizadas.".

4. Em **Horário**, ponha o **Fim** antes do **Início**: "O fim precisa ser depois do início."

### TC-D06 (P1). Presença, encerramento e cancelamento
Crie um evento publicado que comece em poucos minutos (ou aguarde o início), com **Voluntário Dois** inscrito.
1. Antes do início, em **Inscritos** o botão **Registrar presença** está desabilitado, com a nota "A presença pode ser registrada quando o evento começar."
2. Depois do início, clique em **Registrar presença**.

**Esperado:** "Presença registrada."; selo "Presença registrada" e "1 presenças".

3. Em **outro** evento publicado, clique em **Cancelar evento** → **Sim, cancelar o evento**.

**Esperado:** "Evento cancelado. Os inscritos foram avisados."; os inscritos recebem notificação (G02).

4. Depois do horário de término de um evento, clique em **Encerrar evento**: "Evento encerrado.".

### TC-D07 (P1). Regras restantes do evento  *(parte do administrador já passou)*
- **Rascunho** aberto por URL como visitante: não é exibido (não encontrado).
- Voluntária diante de evento que **já começou**: "O evento já começou, então as inscrições estão encerradas." e nenhum botão de inscrição.
- **ONG Teste** abrindo um evento de **outra** ONG: não vê **Inscrever-se** (nota "Contas de ONG e de administração não se inscrevem em eventos.") nem os botões de gerenciar.

---

## Grupo E. Feed e interações

### TC-E02 (P0). Reações
1. Como **Voluntária Teste**, em uma postagem de **outra pessoa** clique em **Curtir**.

**Esperado:** o botão fica amarelo e o contador sobe ("1 reação").

2. Clique em **Adorei**: a reação troca (só **Adorei** marcado), contador continua em 1.
3. Clique de novo em **Adorei**: a reação some ("0 reações").

### TC-E03 (P0). Comentar e responder
1. Em uma postagem, clique em "N comentários", escreva em "Escreva um comentário" e clique em **Enviar**.

**Esperado:** o comentário aparece com o nome do autor; o contador sobe.

2. Com **outra conta**, abra a mesma postagem, clique em **Responder** no comentário, escreva e envie.

**Esperado:** a resposta aparece aninhada. Respostas **não** têm "Responder".

3. O autor do comentário vê **Excluir** e **não** vê "Denunciar" nos seus; quem não é o autor vê **Denunciar** e não vê **Excluir**.

### TC-E04 (P1). ONG divulga um evento no feed
1. Como **ONG Teste**, com um evento **publicado**, em `/inicio` o seletor **Divulgar um evento** lista esse evento (rascunhos e cancelados **não** aparecem).
2. Escolha o evento, escreva o texto e **Publicar**.

**Esperado:** a postagem mostra o atalho "Ver o evento", que leva à página do evento.

3. Como voluntária, o formulário **não** tem esse seletor.

### TC-E05 (P1). Feed vazio e feed dos seguidos
1. Entre com uma conta que não segue ninguém e não publicou (use a do TC-A01 recém-criada).

**Esperado:** "Seu feed está vazio" com o botão **Conhecer ONGs** (leva a `/ongs`).

2. Depois de seguir uma ONG que publicou (TC-F01), as postagens dela aparecem no feed.

### TC-E06 (P2). Imagens em postagens  *(novo)*
O envio de imagens depende do Supabase; por padrão **não está configurado** neste ambiente.
1. Em `/inicio`, clique em **Adicionar imagem** e escolha uma imagem JPEG ou PNG pequena.

**Esperado se o Supabase não estiver configurado:** aviso "O envio de imagens ainda não está configurado." e nenhuma miniatura; o resto do formulário continua funcionando e dá para publicar só o texto.
**Esperado se estiver configurado:** aparece a miniatura; ao publicar, a postagem mostra a imagem, e clicar nela abre a original em outra aba.

2. Tente anexar um arquivo que não é imagem (por exemplo `.txt`): aviso "Use uma imagem JPEG, PNG, WebP ou GIF." (ou o seletor nem o oferece).

---

## Grupo F. Perfis, seguir e mensagens

### TC-F01 (P0). Vitrine de ONGs e seguir  *(parcial: a vitrine filtrar por verificadas já passou; falta a UF e o seguir)*
**Onde:** `/ongs`
1. **Esperado:** só ONGs **verificadas** (a pendente e as recusadas não aparecem); cada cartão mostra nome, **"Cidade - UF"** (por exemplo "Recife - PE") e descrição.
2. Abra uma ONG (`/ongs/:id`) como **visitante**: aparece "Entrar para seguir e conversar" e **nenhuma** publicação.
3. Logada como **Voluntária Teste**, clique em **Seguir**.

**Esperado:** "Agora você segue."; o botão vira **Seguindo** e o contador de seguidores sobe. Clique em **Seguindo**: "Você deixou de seguir.".

### TC-F02 (P0). Mensagens entre voluntária e ONG
1. No perfil da **ONG Teste**, como **Voluntária Teste**, clique em **Enviar mensagem** (`/mensagens?para=...`).

**Esperado:** "Nova conversa com ONG Teste". Escreva e **Enviar**: a conversa é criada e a mensagem fica alinhada à direita.

2. Entre como **ONG Teste**.

**Esperado:** o item **Mensagens** mostra um selo vermelho "1" e a conversa mostra o selo de não lidas. Ao abrir a conversa o selo some. Responda.

3. Volte como a voluntária: a resposta aparece na conversa.

### TC-F03 (P1). Editar o próprio perfil
1. Em `/perfil`, mude **Nome** e **Sobre você** e clique em **Salvar perfil**: "Perfil atualizado."; o nome novo aparece no cabeçalho.
2. Mude o **CEP** para `01310100` e aguarde: **Cidade** = `São Paulo`, **Estado** = `SP`. Salve; no perfil público aparece "São Paulo - SP".
3. No perfil público de **outra** pessoa aparecem **Seguir**, **Enviar mensagem** e **Denunciar perfil**; no **próprio** aparece **Editar meu perfil** e nenhum desses três.

### TC-F04 (P1). Trocar a senha  *(use a conta criada no TC-A01, não as contas prontas)*
**Onde:** `/perfil`, seção "Senha"
1. **Senha atual** errada + nova + confirmação: erro "Senha atual incorreta"; continua logada.
2. Senha atual certa, nova e confirmação **diferente**: "A confirmação precisa ser igual à nova senha."
3. Tudo correto: "Senha alterada. Entre novamente com a nova senha."; vai para `/entrar`. O login com a senha antiga falha; com a nova funciona.

---

## Grupo G. Notificações e tempo real

### TC-G01 (P0). Sino, contagem e leitura
1. Gere atividade para a **ONG Teste**: com a **Voluntária Teste**, **siga** a ONG, **reaja** e **comente** em uma postagem dela e **inscreva-se** em um evento dela.
2. Entre como **ONG Teste**.

**Esperado:** o sino mostra um selo vermelho com a quantidade. Em `/notificacoes`: "Você tem um novo seguidor", "Alguém reagiu ao seu conteúdo", "Alguém comentou na sua postagem" e "Um voluntário se inscreveu no seu evento", os não lidos em amarelo.

3. Clique em uma notificação de comentário.

**Esperado:** abre a postagem (`/posts/:id`); depois, essa notificação aparece como lida e o selo do sino diminuiu em 1.

4. Clique em **Marcar todas como lidas**: "Tudo marcado como lido."; o selo some e o botão desaparece.

### TC-G02 (P1). Destinos das notificações
- Evento cancelado pela ONG: a voluntária inscrita recebe notificação que leva à página do evento.
- ONG verificada: "Sua ONG foi verificada e já pode publicar eventos" leva a `/ong/eventos`.
- "Novo seguidor" **não** leva a lugar nenhum (é botão que só marca como lida).

### TC-G03 (P2). Sem notificações
Uma conta nova abre `/notificacoes`: "Nada por aqui ainda" e a explicação.

### TC-G04 (P1). Atualização em tempo real  *(novo)*
Exige **duas sessões ao mesmo tempo** (por exemplo, uma janela normal e uma janela anônima).
1. Na janela 1, entre como **ONG Teste** e fique em `/inicio`, **sem recarregar**. Anote o selo de "Mensagens" e o do sino.
2. Na janela 2, entre como **Voluntária Teste** e envie uma mensagem à ONG (como no TC-F02).

**Esperado:** em poucos segundos, **sem recarregar**, o selo de "Mensagens" da janela 1 sobe e o sino também. Se a conversa estiver aberta na janela 1, a mensagem aparece nela sozinha.

Se não for possível abrir duas sessões, marque **BLOQUEADO**.

---

## Grupo H. Denúncias e moderação

### TC-H01 (P0). Denunciar e moderar
Use uma postagem **criada pelo teste** (por exemplo, a da **Voluntária Teste** com o texto `Postagem para denunciar HHMM`).
1. Como **Voluntário Dois**, em uma postagem **de outra pessoa** clique em **Denunciar**: abre o formulário com **Motivo** (padrão "Spam") e "Detalhes (opcional)".
2. Escolha "Golpe", escreva um detalhe e clique em **Enviar denúncia**.

**Esperado:** "Denúncia enviada. A equipe vai analisar."

3. Denuncie a **mesma** postagem de novo com a mesma conta: erro dizendo que já foi denunciada.
4. Como **administrador** → **Painel da equipe** → **Denúncias**.

**Esperado:** a denúncia aparece com "Golpe", "Postagem", a data, o **trecho do texto**, o link **"Ver postagem"** e o detalhe escrito. O selo do menu conta as pendências.

5. Clique em **Remover postagem** → **Confirmar remoção** (**somente** na postagem criada pelo teste).

**Esperado:** "Denúncia resolvida."; a denúncia sai da fila; `/posts/<id>` mostra "Postagem indisponível".

### TC-H02 (P1). Descartar mantém o conteúdo, e tipos de alvo  *(parcial: descartar já passou)*
1. Denuncie outra postagem (criada pelo teste) e confira o botão **Ver postagem** no painel.
2. Clique em **Descartar denúncia**.

**Esperado:** "Denúncia descartada."; ao abrir a postagem pelo link ou por `/posts/<id>`, ela **continua visível**.

3. Denuncie um **comentário** e um **perfil** ("Denunciar perfil"): os botões do painel mudam para "Remover comentário" e "Suspender conta"; só postagens têm "Ver postagem".

### TC-H03 (P2). Limites do que se pode denunciar
- Em uma postagem **sua** não existe **Denunciar**.
- Visitante não vê botões de denúncia.

---

## Grupo I. Responsividade (P2)

### TC-I01. Celular (390 px de largura)  *(não executado na onda 1: a janela não redimensionou)*
Tente, nesta ordem: (a) emulação de dispositivo do navegador (DevTools, "Toggle device toolbar", 390 px); (b) redimensionar a janela;
(c) abrir `http://localhost:8081` no celular na mesma rede. Se nenhuma funcionar, marque **BLOQUEADO**.
Percorra `/`, `/eventos`, um evento, `/cadastro/voluntario`, `/inicio` e `/mensagens`.

**Esperado:** nenhuma página com rolagem horizontal; o menu rola na horizontal dentro da barra; o nome sai do cabeçalho e aparece "Meu perfil" na barra de links; em `/mensagens` aparece a lista **ou** a conversa, nunca as duas; os botões de ação dos cartões quebram para baixo do nome.

---

## Resumo a entregar

```
Ambiente: <site e API respondendo? código chegou no Gmail ou Mailpit? Supabase configurado?>
Total: <N casos> | PASSOU: <n> | FALHOU: <n> | BLOQUEADO: <n>

FALHAS (mais graves primeiro)
- TC-XXX (P0/P1/P2): <esperado> / <o que aconteceu> / <URL> / <mensagem de erro, se houver>

BLOQUEADOS
- TC-XXX: <qual caso anterior falhou ou o que faltou no ambiente>

DADOS ALTERADOS
- <contas, ONGs, eventos, postagens criados ou modificados>

OBSERVAÇÕES
- <qualquer comportamento estranho que não seja falha de um caso>
```

Casos **P0 desta onda**: A01, A03, B04, C01, C02, C03, D01, D02, D03, D04, E02, E03, F01, F02, G01 e H01. Se algum falhar,
destaque no início do resumo.
