# Sessão 12 — comandos, evidências e limites

22/10/2026, America/Sao_Paulo. Sessão documental autorizada pelo proprietário,
executada com assistência do Codex. Este registro descreve operações reais;
comandos dos guias públicos são instruções e não uma alegação de clone limpo novo.

## Continuidade e base

- Lidos contexto/resumo externos e consultado somente subgrafo por
  `query_graph.py 'MetadataProvider MetadataRepository CoverResolver MetadataSyncCoordinator'`.
  Snapshot `25d50a506`, anterior às correções das sessões seguintes; fontes atuais
  e documentos canônicos prevaleceram. Nenhum artefato do mapa adicionado ao Git.
- Alias `python` do WindowsApps não executou; usada a instalação Python 3 do
  runtime local. Lidos prompt mestre, AGENTS do workspace/repositório, progresso,
  arquitetura, guias e fontes de contratos/schema/testes/build/workflows.
- `git status --short --branch`, `git log -5 --oneline`, `git show --stat HEAD`
  executados. HEAD inicial `91a378cee`, branch da sessão 11. Somente
  `docs/prompt.md` não rastreado preexistia; preservado e excluído do commit.
- Primeiro `git fetch origin` restrito falhou por conexão; repetição autorizada
  passou. Integração confirmada em `25d50a506`.
- Executados `git switch -c feature/session-12-public-docs origin/yume-develop`
  e, após autorização humana específica, `git merge --ff-only 91a378cee`.
  Quatro commits preservados, sem conflitos. Ancestralidade da integração passou.
- Parte comum dos dois AGENTS conferida; única diferença continua sendo a
  orientação local externa prevista pelo proprietário. AGENTS não foi alterado.

## Conferência remota da sessão anterior

Conector GitHub, apenas leitura:

- PR #5 aberta, não integrada, base `25d50a506`, cabeça `91a378cee`; mergeable true
  é somente condição Git, não aprovação dos checks/requisitos.
- `fetch_commit_workflow_runs` retornou run **39877855871**, concluído com falha.
  Jobs/steps/log do job **120167752077** conferidos. Checkout/Dependency Review/
  JDK/Gradle passaram; instalação Android SDK falhou com **`sdkmanager: command
  not found`, exit 127**. Testes, migrações, lint, build e upload de APK ignorados.
  Upload de relatórios terminou sem arquivos; não comprova artifact ou testes.
- O checkout de PR era o merge sintético `c7a9f7fd328c061cf9ac8fb53c6f8cc5587088bc`,
  não um merge efetivo em `yume-develop`. Falha registrada em README/distribuição.
  Nenhuma configuração, workflow remoto, comentário ou estado da PR alterado.

## Revisão documental e capturas

- Validação local temporária em `build/session12/check_docs.py`, ignorado:
  destinos relativos Markdown/HTML, code fences, alt text, comparação do histórico
  de arquitetura com `git show HEAD:docs/ARCHITECTURE.md` e LICENSE com HEAD.
  Isso verifica arquivos existentes, não disponibilidade de todos os links web,
  renderização Mermaid ou acessibilidade de uma UI Android.
- Duas imagens inspecionadas visualmente antes de copiar; somente configurações,
  estados de lote e status bar. Sem biblioteca/títulos/capas de obras/contas ou
  notificações pessoais. Cópias byte a byte conferidas com originais.
- PNGs 1080×2400, RGBA, integridade verificada com Pillow; apenas metadado `srgb`.
  Hashes em [screenshots](../screenshots/README.md). Sem edição/geração de imagens.
- LICENSE e histórico de arquitetura preservados byte a byte; avisos/copyright
  do README e changelog upstream mantidos. Dados brutos anteriores não incluídos.

## Comandos de demonstração e scripts

JDK 21.0.11 e caches/SDK já preparados no host. Variáveis de ambiente apenas no
processo; nenhuma configuração global alterada. Gradle com dois workers:

```powershell
./gradlew.bat :domain:testDebugUnitTest --tests 'tachiyomi.domain.metadata.*' :data:testDebugUnitTest --tests 'tachiyomi.data.metadata.*' :app:testDebugUnitTest --tests 'eu.kanade.tachiyomi.data.coil.*' --tests 'eu.kanade.tachiyomi.data.metadata.anilist.*' --tests 'eu.kanade.tachiyomi.data.backup.MetadataBackupTest' -Pdist=ci --console=plain --max-workers=2
python -m unittest discover -s scripts/tests -v
python scripts/verify-metadata-migration.py
python scripts/verify-metadata-sync-migration.py
git diff --check
```

- Nove testes Python existentes aprovados. Fixtures de APK são texto sintético,
  não comprovação criptográfica de assinatura Android.
- Migrações sintéticas aprovadas: 148→149 preservou 11 tabelas herdadas;
  149→150 preservou 18 tabelas existentes, FKs, defaults/opt-ins e rollback.
- Primeira tentativa Gradle falhou em **3m06s** em
  `:app:compileDebugJavaWithJavac`: AccessDenied no JAR local de
  `presentation-widget`. Também registrou AccessDenied no daemon Kotlin.
  Domain/data chegaram às tarefas de testes; app não chegou aos seus testes.
  Log completo apenas em `build/session12/demo-tests.log`, ignorado.
- Repetição com acesso autorizado aos caches/artefatos, mesmo comando e nenhum
  ajuste de fonte: **BUILD SUCCESSFUL em 3m58s**, exit 0, 293 tarefas acionáveis
  (10 executadas, 283 atualizadas), configuration cache reutilizado.
  **249 testes selecionados em 14 suítes**, zero falhas/erros/ignorados:
  domain 116/5 suítes e data 54/2 executados na primeira tentativa e
  `UP-TO-DATE` na repetição; app 79/7 executados na repetição. Não é a suíte
  completa de 306 testes da sessão 11. Log local:
  `build/session12/demo-tests-retry.log`, ignorado.

Validação documental final: 18 arquivos Markdown, 230 destinos locais de
links/imagens, zero ausências após gerar o snapshot. A primeira conferência
anterior ao snapshot apontou sua ausência esperada; arquivo criado e checagem
repetida. `git diff --check` e conferência do conteúdo staged aprovados.
Somente documentos/avisos e duas imagens selecionadas entram no commit.

Não houve build de APK/lint/SQLDelight integral novo, instalação, consulta real
AniList, teste UI/TalkBack, restore completo, bateria ou benchmark nesta sessão.
Os 306 testes/build/lint da sessão 11 são evidências históricas identificadas,
não resultado novo desta revisão. Não foi medida cobertura de linhas/branches
nem repetido o build numa máquina limpa. Capturas atuais de seleção/capas,
aceite remoto da sessão 11 e gates de distribuição continuam pendentes.
