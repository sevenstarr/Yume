# Progresso do projeto

## Integração da sessão 8 em yume-develop — 20/10/2026

O proprietário autorizou o merge da [PR #1](https://github.com/sevenstarr/Yume/pull/1), de `feat/session-8-metadata-controls` para `yume-develop`. Base remota `787b275e8230a748661be1f7c50ebbdd49ea9533`, cabeça inicial `53bfa72c43b6b2e78ab8860f7229efeb136a709c`. O fetch mostrou reorganização remota prévia: branches antigas ausentes e `yume-develop`/`yume-release` presentes; nenhuma branch foi excluída ou renomeada nesta integração.

- Incorporada a base à branch da PR por merge, sem reescrever commits. Único conflito: `README.md`, entre a troca isolada de título em `787b275e8` e a reescrita acumulada das sessões. Preservado integralmente o README da sessão 8, que já identifica o projeto como Yume.
- Antes deste registro documental, `git diff --cached --exit-code 53bfa72c4 --` e `git diff --exit-code` passaram: árvore integrada exatamente igual à sessão 8. A integração não altera código, recursos, esquema, testes ou funcionalidades; somente este registro adicional difere da árvore já validada. Verificação de whitespace aprovada; build/testes não repetidos por não haver mudança funcional. Permanecem válidas as evidências e limitações registradas na sessão 8 (264 testes aprovados; Android/UI/acessibilidade pendentes).
- Destino autorizado: `yume-develop`; publicação da resolução na cabeça da PR e conclusão pelo GitHub com merge commit, preservando os históricos. Nenhuma alteração em `yume-release`, upstream ou branch local `animiru-new-main`; sem force-push, squash, rebase ou início da sessão 9.

## Sincronização GitHub — 20/10/2026

O proprietário solicitou enviar todo o progresso local ao GitHub. Árvore inicialmente limpa em `7e9712a835cf78ecd5a7259c83836cf5faa79a3f`; oito branches das sessões 1–8 contêm 11 commits além da base. Destino configurado: fork público `https://github.com/sevenstarr/Yume`. Consulta remota confirmou somente `animiru-new-main` em `787b275e8230a748661be1f7c50ebbdd49ea9533`.

- Revisados histórico, diff acumulado, arquitetura, evidência da criação do fork e gatilhos dos workflows. `git diff --check main HEAD` passou; pesquisa de padrões de chaves privadas/tokens GitHub/AWS no diff completo retornou zero ocorrências, sem constituir auditoria exaustiva. Nenhum arquivo de keystore/PEM/env/local.properties encontrado na listagem versionada consultada.
- Push atômico inicialmente bloqueado pela revisão automática por exigir confirmação do destino público. O proprietário respondeu explicitamente **“autorizo, publique”**, autorizando as oito branches e seu histórico em `sevenstarr/Yume`; a execução foi então permitida. Após login oficial do Git Credential Manager e confirmação final do proprietário, a conta `sevenstarr` ficou disponível e o **push atômico terminou com exit 0**, criando as oito branches e configurando seus upstreams. Nenhum token foi solicitado ao proprietário ou exposto no registro.
- Publicadas `chore/session-1-foundation`, `chore/session-2-baseline`, `docs/session-3-analysis` e `feat/session-{4-anilist-client,5-anime-matcher,6-metadata-persistence,7-cover-resolution,8-metadata-controls}`. A branch da sessão 8 contém todo o progresso cumulativo e os registros desta sincronização. Branch padrão remota `animiru-new-main` preservada; sem force-push, merge ou mudança de configuração GitHub.
- Sem alteração funcional; testes/build não repetidos nesta operação de Git. A última evidência da sessão 8 registra 264 testes aprovados e build debug, com validação Android/UI/acessibilidade ainda pendente. Nenhuma sessão de desenvolvimento seguinte iniciada, nem PR/tag/release criado.

## Sessão 8 — Configurações e escolha manual

**20/10/2026 — implementação local validada por testes/build; aceite UI/acessibilidade em Android pendente.** Pedido do proprietário: executar sessão 8. Base limpa `d8a60d7c9b25daf6166c13da902454a0786d6ef9`, branch `feat/session-8-metadata-controls`. Lidos prompt mestre, progresso, arquitetura/especificação e contratos anteriores; nenhuma alteração não commitada descartada. Gates visuais/performance da sessão 7 continuam pendentes e não são considerados aprovados pelo início desta etapa.

- **Interface:** configurações pesquisáveis de metadados, opt-in SQL inicialmente off com explicação, preferência móvel para futuro lote inicialmente off e limpeza cache/tudo com escopo confirmado. Painel nos detalhes (telefone/tablet) com catálogo/ID, estado/modo/data/falha, título editável, ID/link oficial restrito, páginas explícitas e candidatos com títulos/ano/formato traduzido/miniatura. Validação por ID gera proposta sem substituir o pin; caixa de reconhecimento desmarcada e confirmação gravam a escolha. Refresh usa o mesmo ID; rejeição, modo original e remoção permanecem distintos de apagar capa local. Pendências de backup são exibidas conservadoramente, com instrução de revisão na biblioteca.
- **Integridade e decisões:** `MetadataReviewActions` suspenso, `MetadataReviewModel` Metro/lifecycle e leases de revisão com CAS. Pesquisa explícita pode manter original-only até confirmação. Descartar/cancelar preserva o estado anterior, inclusive indisponível; lease ativa impede duplicata e somente ação explícita reinicia orçamento após expiração. Proposta exige mesma revisão/epoch/stamp da origem e menos de cinco minutos, inclusive se somente temporada/fetch type mudar. Off/clear cancelam transporte e invalidam propostas/resultados temporários; sair da composição cancela a ação. Miniaturas usam CDN neutro, sem persistir bytes; capas aceitas usam as barreiras existentes. Corrigido namespace do cliente `ANILIST` → `anilist`, incompatível com matcher/SQL/Coil; teste do mapper até validação manual acrescentado. Sem DDL/migração nova; schema 149.
- **Verificações:** **264 testes (220 anteriores + 31 parser + 13 integração SQL/serviço)** em 18 suítes; domínio 162, data 36 e app 66, zero falhas/erros/ignorados. Check conjunto de testes/build/migração/Spotless/lint data/widgets passou em **202,762 s** após a guarda do stamp; validação adicional de testes/build/Spotless do app passou em **146,794 s** nas fontes finais. Data lint sem issues; widgets com zero erros/quatro avisos herdados `UnusedAttribute`. Recursos: 62 strings novas em inglês e pt-BR, chaves/referências válidas. Cinco APKs debug locais, assinatura v2 do universal verificada. Hashes de fontes/APKs, tarefas/tempos e lista exata de arquivos em [SESSION_8.json](evidence/SESSION_8.json); tentativas e reprodução em [SESSION_8_COMMANDS.md](evidence/SESSION_8_COMMANDS.md).
- **Arquivos e autoria:** telas novas `SettingsMetadataScreen.kt`/`MetadataReviewScreen.kt`, `MetadataReviewModel.kt`, serviço `MetadataReviewActions.kt`, parser/preferência e seus testes, contratos/repositório de metadados, cliente AniList, navegação/menu/DI e recursos base/pt-BR; README/changelog/arquitetura/especificação, [contrato de uso e limites](metadata/manual-review.md), [ADR 0007](DECISIONS/0007-manual-metadata-review.md) e [rascunho local de PR](SESSION_8_PR.md). Trabalho novo assistido pelo Codex, preservando autoria herdada. Manifesto completo de arquivos no snapshot.
- **Limitações e aceite:** provider/transporte fake e SQLite real comprovam correção de falso positivo sem alteração da linha original/customizada, sem tráfego AniList/CDN real. ADB sem dispositivo conectado; sem instalação, UI/TalkBack/foco/teclado, screenshots, decoder Android ou benchmark. **Não há aceite integral visual/acessível da sessão 8 nem comprovação de ausência de regressão perceptível.** Lint integral do app não repetido, inconclusivo desde a sessão 4, sem supressões. Miniaturas podem ser baixadas novamente ao revisitar a busca; decisão de backup retida não é aplicada arbitrariamente e remapeamento assistido individual continua melhoria futura. Testes de restart em Android continuam pendentes.
- **Ponto de parada:** recurso começa off e somente ações explícitas o utilizam; automação textual gated. Reserva conservadora de até três emissões por operação não equivale a quota física/agregada com tracker/AniChart/AniSkip; integração durável, worker/fila, lote e gatilhos continuam na sessão 9, **não iniciada e dependente de autorização explícita**. Demais gates de termos, identidade e distribuição preservados. Somente commit local; sem push, PR remoto, tag ou release.

## Sessão 7 — Resolução e exibição de capas

**20/10/2026 — implementação local e checks JVM/build aprovados; aceite visual/performance em Android pendente.** Pedido do proprietário: executar sessão 7. A base anterior não estava commitada; o proprietário autorizou explicitamente finalizar a sessão 6 antes desta etapa. Base consolidada `bc56246928dcedfb4c92a402ba7898ff3b3924b1`; branch `feat/session-7-cover-resolution`. Nenhuma alteração anterior descartada.

- **Mudanças:** `CoverResolver`/`CoverPolicy` e índice compartilhado de IDs ativos, queries em lote/IO sem pesquisa no scroll; interceptor antes do keying Coil para `Anime`/`AnimeCover`, transporte CDN neutro, variantes seguras deduplicadas e fallback finito manual→externa→original. Cache-only, cancelamento por política/revisão, chaves explícitas separadas, locks por asset e barreira de oito writes com clear/prune. Source URL e arquivos manuais preservados. Sem DDL/migração nova; schema continua 149.
- **Consumidores e reversão:** componente Compose, pedidos diretos, notificações e widgets usam o resolver; widgets observam mudanças explicitamente e carregam por `execute` suspenso. Visualizador permite ver/usar original e reverter, inclusive antes de associação/com opt-in off; criação de política local exige ausência de decisão, demais mudanças usam CAS. Preferência original preservada após mudança da origem. Salvar/compartilhar capturam página/modo e respeitam background; visualizador cancela requests antigos e não reenfileira o mesmo modelo ao recompor menu. URLs diretas de Discord/receptor Cast, thumbnails de episódios e preview do tracker conservam comportamento aprovado.
- **Verificações:** XMLs atuais de 17 suítes somam **220 testes (187 anteriores + 33 novos)**, zero falhas/erros/ignorados. Domínio 131, data 23, app 66. Verificação conjunta de testes/build/migração/Spotless/lint data/widgets passou em **214,910 s** após revisão; ajuste posterior de deduplicação do visualizador foi validado novamente por testes/build/Spotless em **90,629 s**. Data lint sem issues; widgets: zero erros e quatro avisos herdados `UnusedAttribute` no XML não alterado. Cinco APKs debug locais; assinatura v2 do universal verificada. Contagens, hashes, tarefas e arquivos em [SESSION_7.json](evidence/SESSION_7.json); tentativas/limites em [SESSION_7_COMMANDS.md](evidence/SESSION_7_COMMANDS.md).
- **Provas e limites:** testes de política e SQLite real, cadeia Coil com requests reais/resultados sintéticos, DiskCache físico reaberto offline e HTTP fake. Não houve tráfego AniList/CDN ou imagens reais. ADB sem dispositivo conectado; decoder/UI/Glance Android e comparação de scroll/memória/rede não executados. Logo, **não há aceite integral da sessão 7 nem alegação de ausência perceptível de regressão**. Lint integral do app segue inconclusivo desde a sessão 4, sem supressões. Quarentena de imagem/cooldown é limitada e em processo; revalidação durável/saúde fica pendente.
- **Documentação e arquivos:** [contrato/mapa de consumidores](metadata/cover-resolution.md), [ADR 0006](DECISIONS/0006-cover-resolution.md), [rascunho local de PR](SESSION_7_PR.md), arquitetura/especificação/README/changelog atualizados. Fontes principais em `domain/.../metadata/service/CoverResolver.kt`, `data/.../metadata/CoverResolverImpl.kt`, `app/.../data/coil/{MetadataCoverInterceptor,ExternalCoverFetcher}.kt`, `MetadataCover.kt`, visualizador, widgets, repositório/query, strings e testes; lista completa no snapshot. Trabalho novo assistido pelo Codex, preservando autoria herdada.
- **Ponto de parada:** opt-in e automação textual permanecem off; nenhuma busca/seleção remota/configuração global/coordenador ativados. Validação Android da sessão 7 e demais gates herdados continuam pendentes. Sessão 8 não iniciada, aguardando autorização explícita. Somente commit local; sem push, PR remoto, tag ou release.

## Sessão 6 — Persistência, cache e migração

**20/10/2026 — implementação anterior consolidada antes da sessão 7, com autorização explícita do proprietário.** Base `4531d813e680488497b384a83d82f01ce478b065`, branch `feat/session-6-metadata-persistence`. Ao iniciar o pedido de sessão 7, foram encontradas alterações staged e adicionais não commitadas da sessão 6, sem registro final. O proprietário autorizou validar/finalizar esta base em commit separado e prosseguir com a sessão 7. Alterações anteriores preservadas.

- **Implementação:** migração aditiva 148/schema 149 e snapshot sintético; decisões, leases/CAS/generation/epoch, cooldown, snapshots/TTLs/negativos/LRU e índice/barreira de imagens separados. Repositório SQLDelight com Flow em lote, pins/rejeições/modo original e limpeza seletiva. Backup protobuf opcional v1/tag 600, remapeamento conservador e conflitos duráveis. Correção localizada do restore de seis campos `custom*`, preservando originais e personalizações do destino. Opt-in e automação textual permanecem off; sem consumidor visual/coordenador nesta sessão.
- **Verificação da continuidade:** `testDebugUnitTest` passou em 169,897 s; XMLs de 13 suítes somam **187 testes (164 anteriores + 19 SQL + 4 backup)**, zero falhas/erros/ignorados. `:app:assembleDebug` passou em 88,060 s e `verifySqlDelightMigration` em 24,017 s. `:data:lintDebug` passou em 94 s, sem issues; formatação final aprovada após normalizar somente CRLF→LF em três arquivos e quebrar uma linha longa sem alterar comportamento. O script SQLite confirmou igualdade das linhas/definições das 11 tabelas herdadas, FKs, default off e rollback em cópia sintética. Tempos e resultados finais em [SESSION_6.json](evidence/SESSION_6.json).
- **Ambiente:** tentativa restrita de testes falhou por acesso ao JAR de `presentation-widget`; repetição com acesso aos caches passou. Alias `python` do WindowsApps inacessível; usado Python do runtime local. Nenhum contorno no código ou supressão de detector. Comandos/tentativas e limites em [SESSION_6_COMMANDS.md](evidence/SESSION_6_COMMANDS.md).
- **Documentação:** [persistência](metadata/persistence.md), [ADR 0005](DECISIONS/0005-metadata-persistence.md) e [rascunho local de PR](SESSION_6_PR.md); README, changelog, arquitetura e especificação atualizados. Código/documentação novos assistidos pelo Codex.
- **Limites e próximo passo:** testes JVM/SQLite e fixtures de leitor anterior não substituem morte de processo/restore em Android. Sem instalação, emulador funcional, benchmark, headers/decode/cache Coil real ou backup de bytes de capas manuais. Lint integral do app permanece inconclusivo desde a sessão 4. Integração durável da quota e fila fica na sessão 9. Sessão 7 autorizada pelo proprietário após esta consolidação; sem push/PR remoto/tag/release.

## Sessão 5 — Normalização e correspondência de animes

**20/10/2026 — matcher puro implementado; testes/build/formatação aprovados; automação textual desativada.** Pedido do proprietário: ler o commit anterior, executar sessão 5 e fazer commit depois. Base `74a670cc0eb27563a80f54fe0378ab95dc6e5ce9`; branch `feat/session-5-anime-matcher` derivada do HEAD da sessão 4 para conservar dependências.

- **Mudanças:** modelos/identidade/fingerprint `identity-v1`, normalização `title-nfc-v1`, ranking/evidências/vetos `matcher-v1`, completude agregada/rejeições e regras de preservação de vínculos. Seleção manual propõe pin somente com identidade validada pelo provider e reconhecimento de diferenças. Domínio puro, sem consumidor UI/worker ou mudança de SQL, backup, fontes, tracking, player e dados originais. Código novo assistido pelo Codex, com marcadores de origem.
- **Calibração:** 29 fixtures de calibração e 36 de validação separada, sintéticas e rotuladas, com falsos candidatos deliberados. Grade de 186 configurações, 124 sem falso positivo na calibração; parâmetros propostos 50/15/10, limiares 70/30 e margem 10. Na cópia habilitada de teste: calibração 10 acertos automáticos/29 (34,48%), 19 abstenções; validação 10/36 (27,78%), 26 abstenções; zero falsos positivos automáticos conhecidos, 10/10 positivos cobertos em cada conjunto. Rótulos não são avaliação humana independente; resultados não demonstram precisão no catálogo real. `ProposalV1.automationApproved=false`; sem configuração aprovada, nenhum texto associa automaticamente.
- **Verificações:** `testDebugUnitTest` passou em 84,732 s; XMLs de 11 suítes somam **164 testes (87 anteriores + 77 novos)**, zero falhas/erros/ignorados. `:app:assembleDebug` passou em 74,224 s, cinco APKs locais; `spotlessCheck` passou em 19,135 s. Tentativas restritas de teste/build falharam por acesso a `presentation-widget/.../classes.jar`; repetição com acesso aos caches locais passou, sem alteração de código para contornar o erro. Domínio teve tarefas reutilizadas quando já atualizado. Não foi medida cobertura de linhas/branches; lint integral não repetido, permanece inconclusivo desde a sessão 4. Sem teste em Android/instalação ou migração/restore.
- **Documentação:** [contrato e limites](metadata/anime-matcher.md), [ADR 0004](DECISIONS/0004-conservative-anime-matcher.md), [comandos](evidence/SESSION_5_COMMANDS.md), [relatório por fixture](evidence/SESSION_5_MATCHING.txt), [snapshot](evidence/SESSION_5.json) e [rascunho local de PR](SESSION_5_PR.md). README, changelog e arquitetura atualizados; P-01 registra proposta/gate pendente e P-04 registra a parcela de domínio resolvida.
- **Próxima sessão:** 6, persistência/cache/migração/backup e gravação condicional de vínculos/pins/rejeições. Revisão do gate de matching continua necessária antes de habilitar automação; a entrega atual não grava decisões nem comprova concorrência persistente. Demais limitações do baseline/sessão 4 permanecem. Trabalho/commit somente locais, sem push/PR remoto/tag/release.

## Sessão 4 — Cliente público AniList

**20/10/2026 — cliente isolado implementado e validado por testes/build; lint inconclusivo.** Pedido do proprietário: executar a sessão 4 e fazer commit depois. Base `48b23e78dd1b07526603b15b31bed8fd43caaf62`; branch `feat/session-4-anilist-client`, derivada do HEAD da especificação para manter as dependências da Sessão 3.

- **Mudanças:** contratos/modelos em `domain/.../metadata`; DTOs, mapper/provider, transporte neutro, gate opt-in, quota/circuito/retries e singleton Metro em `app/.../data/metadata/anilist`; exposição no `AppGraph`. Nenhum consumidor UI/worker ativado, nenhuma escrita nos dados originais ou mudança de tracking/fontes/player. Código novo assistido pelo Codex, com marcadores de origem.
- **Decisões:** [ADR 0003](DECISIONS/0003-anilist-public-client.md) e [contrato implementado](metadata/anilist-client.md) registram limites de input, URLs observadas, parciais/completude, cancelamento, três emissões/45 s e orçamento de processo. README, changelog e arquitetura atualizados; [rascunho local de PR](SESSION_4_PR.md) preparado.
- **Evidência externa:** documentação oficial AniList/OkHttp relida; um POST público confirmou esquema/capas/banner e headers 30/min (restante 29), sem login/mutation/download de imagens. Fontes e resultados reais estão no [registro de comandos](evidence/SESSION_4_COMMANDS.md).
- **Verificações:** `testDebugUnitTest` passou em 109,434 s; oito suítes, **87 testes (52 herdados + 35 novos)**, zero falhas/erros/ignorados nos XML. Build debug passou em 71,303 s; cinco APKs locais, assinatura v2 do universal verificada. `spotlessCheck` final passou em 8,761 s; diff sem erro de whitespace, links locais conferidos, nenhum controle bidi nos nove arquivos Kotlin novos. [Snapshot de resultados](evidence/SESSION_4.json). Não foi medida cobertura de linhas/branches. O certificado debug atual difere do baseline, sem teste de atualização direta e sem mudança da configuração de assinatura versionada.
- **Riscos e pendências:** lint interrompido após 20 min retido na travessia PSI de comentários de `BidirectionalText`, sem relatório novo concluído; nenhum detector foi suprimido e o relatório de 19/10 não valida este diff. Budget/cooldown/circuito em memória, sem integração com tracker/AniChart/AniSkip; persistência e teste de restart ficam nas Sessões 6/9 antes de ativar o recurso. Sem teste de instalação/Android real, Coil, backup/migração ou performance. Enquadramento nos termos/direitos de imagens e pendências herdadas permanecem para distribuição.
- **Próxima sessão:** 5, normalização, matching conservador e calibração; não iniciada. Trabalho e commit somente locais, sem push/PR remoto/tag/release.

## Sessão 3 — Especificação funcional após aprovação da Etapa B

**19/10/2026 — especificação produzida para revisão; arquitetura aprovada, implementação futura.** O proprietário solicitou usar a arquitetura aprovada na Etapa B, produzir a especificação funcional e fazer commit. Base: `cb86e7ff7471b4d2c0bdbc952b51b6ba6d722bd0`, branch `docs/session-3-analysis`. Essa instrução atualiza o status do ADR 0002; os registros anteriores de proposta pendente permanecem históricos.

- **Entrega:** [functional-specification.md](metadata/functional-specification.md) reúne 20 requisitos funcionais e oito não funcionais, nove contratos lógicos de interface, modelos de domínio/persistência/backup, estados/comandos, oito fluxos de usuário, 22 critérios de aceitação, casos extremos, 19 conjuntos de testes planejados e tarefas para as Sessões 4–10. Onze pendências identificam calibração, hosts/links, detalhes de contrato/DDL, backup, Coil, fila, desempenho, UI, termos e limitações herdadas, sem inventar valores ou concluir testes.
- **Coerência documental:** ADR 0002 registra a aprovação da direção pelo pedido atual; arquitetura canônica e índice histórico apontam para a especificação e distinguem aprovação arquitetural de aceite funcional/execução.
- **Verificação:** leitura da Etapa A/B, ADR, progresso/baseline e pontos de código de modelo/SQL, transporte AniList e backup. Nos cinco documentos da entrega, 90 links locais e 27 âncoras conferidos, todos válidos; referências RF/RNF/CA/T/P sem IDs indefinidos ou duplicados e cercas balanceadas. Os 28 requisitos apontam para aceite, os 22 critérios apontam para testes e os 19 conjuntos de testes estão cobertos pelos critérios; nove interfaces e 16 tarefas conferidas. `git diff --check` e `git diff --cached --check` passaram, incluindo a especificação nova no diff staged. Sem novo acesso API/CDN, build, teste Android, migração, restore ou benchmark nesta entrega documental.
- **Próximas sessões:** implementar e validar cliente → matcher/calibração → persistência/backup → capas → UI → sync → regressão conforme tarefas/dependências da especificação. Aprovação da arquitetura não aprova os resultados da implementação. Somente commit local solicitado, sem publicação.

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
