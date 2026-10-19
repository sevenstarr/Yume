# Progresso do projeto

## Sessão 3 — Proposta de arquitetura após a Etapa A

**19/10/2026 — projeto documentado para revisão, sem implementação.** Base `4fdc01724`, branch `docs/session-3-analysis`. O pedido atual autoriza projetar o sistema e fazer commit; não equivale a aprovar o conteúdo proposto nem iniciar as sessões de implementação.

- **Mudanças:** [proposed-architecture.md](metadata/proposed-architecture.md) define componentes/contratos, alternativas de provedores, identidade e ambiguidades, proteção contra falsos positivos, estados, persistência/concorrência, cache/limpeza, migração/backup, capas/fallback, integração Animiru, sync, limites, erros e plano de testes. [ADR 0002](DECISIONS/0002-metadata-enrichment-architecture.md) permanece proposto. `ARCHITECTURE.md` aponta para a proposta canônica, preservando o histórico.
- **Evidências:** leitura da Etapa A e requisitos da Sessão 3 do prompt mestre; inspeção dos pontos centrais de código/SQL/Coil/backup; consulta a fontes oficiais AniList, Kitsu, TMDB e Coil 3.6.3. Acesso direto a páginas AniList retornou 403, mas conteúdo indexado oficial foi apresentado pela pesquisa; nenhum probe API/CDN novo foi executado.
- **Verificação documental:** revisão de cobertura dos requisitos; checagem PowerShell de 43 links locais em quatro documentos (34 destinos distintos), todos existentes, e 13 âncoras do sumário, todas válidas; blocos cercados balanceados. `git diff --check` passou e a última migração foi reconfirmada como `147.sqm`. Não foram repetidos build, testes Android, migração, restore, benchmark ou calibração do matcher porque só há documentação.
- **Riscos e pendências:** proposta/ADR/contratos/migração aguardam revisão; associação textual automática depende de calibração e validação separadas; allowlist precisa de confirmação por respostas oficiais; adequação aos termos antes de escala/distribuição. Pendências herdadas de backup `custom*`, capas manuais, emulação e lint continuam abertas.
- **Próximo passo:** revisar a arquitetura antes de autorizar cliente/matcher/persistência. Nenhum contrato Kotlin, SQL, dependência, UI, worker ou comportamento alterado. Somente commit local; sem push, PR, tag ou release.

## Sessão 2 — Build de referência e regressão

**19/10/2026 — baseline executado; instalação/regressão em Android pendentes por bloqueio do emulador.** Branch `chore/session-2-baseline`, derivada de `4d67e3a0e0df2b7bf964370ee1e47b2bdfa34d8e`. Código funcional upstream preservado na referência auditada `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`. O pedido explícito do proprietário iniciou esta etapa; pendências visuais/publicação da sessão 1 permanecem abertas e não impediram o baseline local.

Escopo: preparar ferramentas, build debug, testes herdados, verificações de formato/lint/migração, AVD isolado e checklist manual. Sem implementação de metadados, mudança de identidade Android, push, PR remoto ou release. Termos SDK aceitos explicitamente pelo proprietário antes da instalação.

Documentos e execução reproduzível: [BUILD_BASELINE.md](BUILD_BASELINE.md), [REGRESSION_CHECKLIST.md](REGRESSION_CHECKLIST.md), [run-baseline.ps1](../scripts/run-baseline.ps1), [comandos](evidence/SESSION_2_COMMANDS.md), [snapshot de evidências](evidence/SESSION_2.json) e [rascunho local de PR](SESSION_2_PR.md). As menções abaixo a “sessão 2 não iniciada” pertencem aos registros históricos anteriores a esta autorização.

### Resultados e arquivos

- JDK 21 já instalado selecionado por processo; Gradle 9.7.1 baixado e executado. SDK 37.2, Build Tools 37/36, Platform Tools, emulador e imagem API 36 preparados em `.tooling`, fora do Git. Nenhuma alteração global de PATH ou instalação de driver.
- `:app:assembleDebug`: exit 0, 25min54s; cinco APKs locais. Assinaturas debug universal/x86_64 verificadas; pacote herdado `.dev`, versão `0.20.0.1-8244`. Hashes preservados nas evidências, sem APK/chave no Git.
- `testDebugUnitTest`: exit 0, 219,373 s; **52 testes em seis suítes**, zero falhas, erros ou ignorados nos XML. Cobertura de linhas/branches não medida.
- `spotlessCheck`: exit 0, 91,225 s. `verifySqlDelightMigration`: exit 0, 12,005 s, com zero snapshots `.db`; não comprova migração histórica.
- `:app:lintDebug`: exit 0, 769,230 s, mas **3 erros, 201 avisos e 2 hints** no relatório. Os erros `NonObservableLocale` já estão no código herdado; lint permissivo não foi classificado como limpo.
- AVD Pixel 6/API 36 criado; aceleração indisponível (exit 6, driver ausente). Boot de software permaneceu offline; nova tentativa com o host liberado falhou com `0xC0000005`. APK não instalado, sem testes manuais aprovados. Daemons de compilação e emulador da sessão encerrados.
- README, CHANGELOG, arquitetura/progresso atualizados; adicionados baseline, checklist, evidências, script e rascunho de PR. Trabalho assistido pelo Codex. Nenhuma mudança em fontes Android, recursos, SQL, dependências ou workflows.

### Riscos, pendências e próximo passo

Instalação requer dispositivo de teste utilizável ou correção da emulação. Checklist de biblioteca/player/extensões/downloads/progresso e backup/restauração permanece sem execução; não há garantia de dados preservados. Possível omissão de `custom*` continua evidência estática, sem correção ou round-trip comprovado. Migração histórica precisa de fixtures válidas; os três erros de lint precisam de triagem em escopo apropriado. Identidade Android/visual e publicação da sessão 1 seguem pendentes.

**Ponto de parada:** baseline de build/testes entregue com limitações explícitas. Sem push, PR remoto, tag ou release. Sessão 3 (contratos/arquitetura de metadados) aguarda autorização explícita separada; nenhum componente dessa etapa foi implementado.

## Sessão 1 — Fundação do fork e identidade

### Atualização posterior — criação do fork autorizada e executada

Em 19/10/2026 o proprietário respondeu **“autorizo”** à criação do fork público `sevenstarr/Yume`. Repositório criado pela interface GitHub e verificado na API (ID `1426233623`, visibilidade pública) e na página, que mostra `forked from animiruorg/Animiru`. URL: https://github.com/sevenstarr/Yume.

`origin` configurado localmente para `https://github.com/sevenstarr/Yume.git`; `upstream` preservado. Branch padrão remota atual: `animiru-new-main`. A autorização recebida e executada foi para criar o fork; os commits da fundação continuam locais. Nenhum push, PR, tag, release ou alteração de configurações GitHub. Evidência: [FORK_CREATION.json](evidence/FORK_CREATION.json).

Pendências atuais: revisão da identidade visual/Android e autorização de envio dos commits/configuração remota quando aplicável. Sessão 2 não iniciada. O estado inicial abaixo e o JSON da primeira verificação da fundação são históricos, anteriores à criação do fork.

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
