# Progresso do projeto

## Sessão 1 — Fundação do fork e identidade

**19/10/2026 — fundação local preparada; aceite integral pendente das decisões do proprietário. Sessão 2 não iniciada.**

### Autorização, decisões e escopo

- Pedido explícito para iniciar a sessão 1 do prompt; nome **Yume** confirmado em resposta do proprietário.
- Conta conectada GitHub consultada somente para identificar um possível destino. Conta/organização do fork ainda não confirmada; nenhum fork remoto criado, push, PR, tag ou release.
- Base auditada mantida no SHA `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`. Histórico completo obtido por `git fetch --unshallow --tags origin`: clone raso passou de `true` para `false`; `main` tem **8.242 commits** alcançáveis.
- Criadas `main` na base e `chore/session-1-foundation` para a fundação. Branch `animiru-new-main` preservada. Remote da origem renomeado localmente para `upstream`; `origin` Yume não configurado por suposição.
- ADR 0001 adotado somente para organização local. Proposta de arquitetura de metadados permanece preliminar; contratos da sessão 3 não antecipados.
- Nome aplicado à documentação e aos templates. Runtime Android permanece herdado, com plano explícito de pacote, ícone, links, updater, assinatura e backup em `IDENTITY.md`.

### Arquivos e autoria

- Alterados README, CONTRIBUTING, CODE_OF_CONDUCT, CHANGELOG, `.gitignore` e quatro templates/configurações em `.github/`.
- Criados SECURITY e NOTICE; preservados LICENSE e os avisos originais.
- Versionados relatórios/evidências da sessão 0 e snapshots de README/CONTRIBUTING/CODE_OF_CONDUCT em `docs/upstream/`.
- Adicionados UPSTREAM, IDENTITY, LICENSE_REVIEW, ADR 0001, rascunho local de PR e evidências da sessão 1.
- Documentação produzida com assistência do Codex. Commits de automação usam identidade explícita `Codex (AI assistant) <codex@invalid>`; nenhuma atribuição humana foi presumida.
- Pasta de trabalho preservada como `Animiru/`; documentação canônica agora dentro do repositório em `Animiru/docs/`. Relatórios externos da sessão 0 permanecem como registro histórico, com índice atualizado no workspace.

### Verificações e evidências

Comandos e resultados estão em [evidence/SESSION_1_COMMANDS.md](evidence/SESSION_1_COMMANDS.md); captura estruturada em [evidence/SESSION_1.json](evidence/SESSION_1.json). A validação da fundação verifica linhagem e base Git, escopo do diff, igualdade da licença e dos snapshots, links locais novos, estrutura básica dos templates YAML, casos de `.gitignore` e indicadores comuns de segredo no conteúdo novo. Templates revisados estaticamente; parser YAML indisponível. A varredura de segredos é limitada ao diff/arquivos novos; não certifica todo o histórico herdado.

Build, lint Android/Spotless, testes de app, instrumentação, instalação, migração, backup e CI remoto **não executados**. São trabalhos de sessões seguintes; validação documental não os substitui.

Resultado: 102 checagens documentais passaram; 40 links locais e 33 casos de inclusão/exclusão verificados. Nenhum indicador comum de segredo no conteúdo novo; conectividade dos objetos Git validada com `git fsck --connectivity-only --no-dangling` (exit 0).

### Riscos, pendências e próximo passo

- Confirmar conta/organização e autorizar criação/publicação do fork remoto. O nome Yume está confirmado; a conta conectada não foi tomada como autorização.
- Revisar o plano de identidade: candidato de pacote condicionado à conta, ícone/paleta a definir, updater proposto desativado e callbacks OAuth com cadastro/validação próprios.
- Definir canais privados de segurança/conduta antes da abertura pública. Nenhuma configuração GitHub foi alterada.
- Revisão documental Apache-2.0 não certifica APK/dependências transitivas. Empacotamento herdado exclui avisos META-INF; inventário real e apresentação dos avisos ainda pendentes. Termos AniList/direitos de imagens seguem abertos para distribuição.
- Build de referência, coexistência e backup ainda sem validação. Não há APK Yume distribuível.

**Ponto de parada:** revisar a fundação local e resolver conta/identidade/publicação dentro da sessão 1. Não declarar o aceite integral antes disso. Iniciar a sessão 2 somente após autorização explícita separada.

O registro abaixo descreve a sessão 0 tal como encerrada; seus bloqueios e localização dos documentos são históricos.

## Sessão 0 — Reconhecimento e viabilidade

**19/10/2026 — diagnóstico concluído; aguardando aprovação da direção arquitetural. Sessões 1–13 não iniciadas.**

### Escopo executado e decisões de trabalho

- Leitura integral de `C:\Users\Administrator\Downloads\prompt.md` e execução exclusiva da Sessão 0.
- Workspace inicialmente vazio/sem Git; cópia local rasa do upstream em `Animiru/` apenas para inspeção. Não foi criado fork remoto.
- Origem canônica confirmada por links públicos e redirecionamento; branch `animiru-new-main`, SHA `0b9bb0a347aa7bcee226d8782d98d9abafc34b32` fixado como referência auditada, ainda não como escolha aprovada para implementação.
- Mapeados arquitetura, ferramentas, banco, cache/Coil, títulos/capas, refresh de fontes, AniList/tracking, workers, CI, backup e instalação.
- Termos/docs AniList lidos na origem oficial; probe público sem login confirmou campos e quota de 30/min. HEAD de capa confirmou formato PNG da amostra sem baixar imagem.
- Proposta redigida como preliminar; nenhum ADR estrutural aceito, contrato definitivo, migração ou componente implementado.

### Arquivos produzidos

Todos em `C:\Users\Administrator\Documents\Yume\docs`, fora da árvore Git do upstream:

- `UPSTREAM_AUDIT.md`: diagnóstico técnico e parecer condicionado.
- `INTEGRATION_MAP.md`: classes/arquivos existentes e novos propostos, consumidores e fronteiras de preservação.
- `RISK_MATRIX.md`: riscos, evidências e mitigação por etapa.
- `ARCHITECTURE.md`: direção preliminar para aprovação, sem execução da Sessão 3.
- `evidence/COMMANDS.md`: comandos e limitações efetivamente observados.
- `evidence/SESSION_0.json`: snapshot selecionado de refs/releases/CI, probe HTTP e ambiente.
- `PROGRESS.md`: este registro.

### Comandos e resultados principais

| Verificação executada | Resultado |
|---|---|
| `Get-Content ...\Downloads\prompt.md -Raw` | Leitura integral |
| `git clone --depth 1 --no-single-branch ... Animiru` | Primeira tentativa sem rede falhou; repetição com acesso autorizado terminou exit 0 |
| `git rev-parse HEAD`, branch/tag/log | Referência auditada confirmada; clone raso, sem histórico integral |
| `git diff --shortstat v0.20.0.1 HEAD` | 720 arquivos / +11312 / -9004; não confundir HEAD com release estável |
| `rg`/`Get-Content` sobre código/build/docs | Mapa baseado em arquivos reais; fontes e propostas distinguidas |
| API GitHub, refs/releases/workflows | Cinco runs recentes verdes em outros SHAs; zero runs retornados para o SHA exato |
| POST GraphQL público `Media(id:1,type:ANIME)` | HTTP 200; quota 30/min, restante 29; sem token/mutation |
| HEAD de capa retornada | HTTP 200, image/png, 489507 bytes; sem download de binário |
| `java -version` e descoberta de ferramentas | Java 25.0.4; SDK/ferramentas Android ausentes nos locais verificados; CI pede JDK 21 |
| `git diff --exit-code`, cached diff e porcelain | Upstream local sem alterações |

Registro detalhado em [evidence/COMMANDS.md](evidence/COMMANDS.md). Verificação de entrega: 54 caminhos exatos conferidos (36 existentes e 18 novos propostos), sem ausência inesperada; JSON válido. A checagem final encontrou diferença de proprietário do clone no sandbox, resolvida com `-c safe.directory` limitado à chamada/caminho, sem alterar configuração global. Status, diff e cached diff terminaram com exit 0 e o mesmo SHA: cópia Git limpa.

### Riscos e pendências

- Condição externa: esclarecer o enquadramento do novo uso nos termos AniList e os direitos/políticas de imagens antes de distribuição. Nenhum contato foi feito.
- Viabilidade técnica de código confirmada; **compilação, testes de app, instalação e migrações não executados**.
- Reproduzir defeito candidato de restore dos campos personalizados antes de usar backup real.
- Resolver identidade/appId/deep links/updater/assinatura nas sessões autorizadas.
- Não há branch/commit de desenvolvimento ou PR: cópia de evidência intacta e relatórios locais. Não houve publicação ou mudança de remoto.

### Próximo passo e ponto de parada

Solicitar aprovação da direção em `ARCHITECTURE.md` e da referência de partida. A recomendação é o HEAD auditado, sujeito ao baseline posterior. **Não iniciar a Sessão 1 sem autorização explícita do proprietário**, nem executar build de referência da Sessão 2 ou implementação das sessões seguintes.

Relatório: **Status** concluído como reconhecimento; **Mudanças** documentação e cópia local; **Testes e evidências** inspeção, Git e probe público; **Riscos** AniList, dados/backup e build não validado; **Pendências** aprovação/base e verificações futuras; **Próxima sessão** 1, somente autorizada.
