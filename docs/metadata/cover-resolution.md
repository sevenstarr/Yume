# Resolução e exibição de capas — Sessão 7

**Leitura atualizada na sessão 12:** registro da sessão 7. Configurações/revisão
e fila/background foram implementados nas sessões 8–9. Consulte a
[arquitetura atual](../ARCHITECTURE.md) para a composição vigente. Testes
JVM/Coil não substituem os gates visuais e de performance em Android.

20/10/2026. Implementação nova assistida pelo Codex. Dependência: [persistência da sessão 6](persistence.md), consolidada no commit `bc5624692` com autorização explícita antes deste incremento. O opt-in permanece off; configurações, busca/seleção de IDs e sincronização continuam nas sessões 8/9.

## Fluxo e precedência

`CoverResolver` observa somente IDs solicitados, através de um índice singleton com contagem de assinantes. Registros e snapshots são consultados em lote no dispatcher IO; registros de tiles são agrupados por 16 ms, não por frame. O índice não acumula a biblioteca inteira e para após cinco segundos sem assinantes, descartando o replay antigo. Mudanças de banco/controle, identidade e expiração dos snapshots atualizam a seleção. Nenhuma etapa deste fluxo chama GraphQL/provider ou executa matching.

`CoverPolicy` produz um snapshot imutável com revisão, epoch, origem e até três assets. Manual local tem precedência em modo normal, seguida da associação validada, original e placeholder. Off conserva manual/original. Preview original e preferência persistida original-only ignoram manual/externa sem apagar arquivos ou vínculo. Associação restaurada, indisponível, com revisão pendente, identidade incompatível ou snapshot inelegível não fornece capa externa. Uma falha transitória de atualização não apaga um pin íntegro.

“Usar original” também funciona antes da primeira associação e com opt-in off. `setOriginalOnly` recebe revisão nula somente para criar a decisão local se ainda não existir; estado existente exige CAS pela revisão observada. O comando não pesquisa, não ativa enriquecimento e não altera a fonte. A preferência original-only continua válida se a identidade da fonte mudar; apenas o uso da associação antiga é suspenso. “Usar capa selecionada” reverte a política, conservando arquivo manual e vínculo.

O interceptor instalado em `App.newImageLoader` executa antes do keying do Coil para `Anime` e `AnimeCover`. Cada tentativa continua a cadeia com request e chave próprios; erros retornados pelo engine incluem falhas de fetch e decode. O request conserva contexto, target, tamanho, transformações e políticas de cache. A original desliga o ramo manual, e a cadeia termina depois dela. Cancelamento do chamador propaga; mudanças de política cancelam a tentativa externa e permitem fallback à original.

## Disponibilidade e cache

- Ordem de variantes: `extraLarge`, `large`, `medium`, somente URLs retornadas/validadas, deduplicadas. Não há dimensão ou URL inventada; o nome da variante não prova superioridade sobre a original. Lista usa o tamanho solicitado pelo Coil; visualização/exportação pede `Size.ORIGINAL`.
- Somente HTTPS em `s4.anilist.co`, porta padrão/443, caminho de capas AniList e sem credenciais/fragmento/traversal. Allowlist preserva o host verificado na sessão 4. Redirecionamentos são recusados, inclusive para outro caminho do mesmo host; não se infere um destino seguro.
- Transporte CDN reaproveita pool/DNS/TLS do app, removendo interceptors, cookies, autenticação, cache HTTP e headers privados. Timeouts 10/20/30 s, corpo limitado a 16 MiB, sem retries automáticos. Um interceptor neutro interrompe follow-ups especiais de 408/421/503; demais respostas não 2xx viram falha finita.
- 404/410/decode permitem tentar outra URL do mesmo ID. Falha geral de rede/429/servidor impede novas tentativas em rede nas variantes seguintes; ainda permite ler suas cópias locais. Cache-only continua cache-only até a original. Falha do banco/eviction da camada também permite a original.
- Quarentena **em processo**, LRU de no máximo 512 chaves: 24 h para asset inválido/404/410/decode; 15 min para falha transitória. Cooldown do host de imagens é separado do GraphQL. Reinício reinicia essa proteção; estado durável de saúde/revalidação fica como limitação explícita, sem agendar pesquisa por falha de imagem.
- Chave externa `yume-metadata-v1:anilist:<id>:<sha256-url>:<fetchedAt>` usa a revisão do snapshot público, compartilhando bytes entre fontes locais. Revisão da decisão e epoch continuam na seleção e na barreira de publicação. Chaves explícitas do chamador não misturam as tentativas. Original reutiliza a chave herdada da URL/mtime; manual usa chave própria e não é escrita no cache externo.
- Bytes ficam exclusivamente no disk/memory cache existente do Coil. A barreira durável registra as chaves antes de carregar/decodificar e verifica revisão/epoch/origem antes/depois. Até oito writes podem coexistir; locks com duração da requisição deduplicam o mesmo asset. Clear/prune drenam os oito permits antes da eviction final. Não há SQL/HTTP em keyers nem segundo diretório de imagens.

## Cobertura dos consumidores

| Consumidor | Caminho e comportamento |
| --- | --- |
| Biblioteca, catálogo/busca, migração, duplicatas e temporadas | `CommonAnimeItem`, `BaseAnimeListItem`, `MigrationListScreenContent`, `DuplicateAnimeDialog` usam `AnimeCover`; o helper Compose compartilha o índice por ID e reconstrói o request quando a seleção muda. Item de catálogo sem associação persistida conserva a original |
| Detalhes | `AnimeInfoHeader` usa o mesmo componente para a capa; `useBackground(true)` conserva o background herdado |
| Histórico, atualizações, próximos episódios, armazenamento, edição e controles Cast dentro do app | `HistoryItem`, `UpdatesUiItem`, `UpcomingItem`, `StorageItem`, `EditAnimeDialog`, `CastInfoBox`: mesma fronteira `AnimeCover` |
| Visualizador/salvar/compartilhar | `AnimeImagesDialog` e `AnimeImageViewModel`: preview original e uso persistido reversível, sem apagar manual/vínculo. Exportação captura página/modo ao clicar e respeita background versus capa. Requests antigos do visualizador são descartados ao atualizar/fechar |
| Notificações | `LibraryUpdateNotifier` já pede `Anime` pelo singleton Coil; ícones produzidos passam pelo resolver, sem pesquisa externa. Notificações já entregues não são reemitidas só porque uma capa mudou |
| Widgets | `BaseUpdatesGridGlanceWidget` observa um lote visível de IDs e carrega por `execute` suspenso, cancelável; `WidgetManager` atualiza ambos os widgets quando a seleção muda. Dependência continua widget→domain, sem widget→app |
| Thumbnails de episódio e imagens genéricas | Modelos próprios/bare URL seguem a cadeia herdada. Nenhuma associação é criada por carregamento |
| Preview de tracker | `TrackSearch.cover_url` continua URL do resultado do tracker, sem vínculo com biblioteca |
| Discord RPC e capa enviada ao receptor Cast | URLs diretas fora do Coil continuam originais conforme arquitetura aprovada. Exibição dos controles Cast dentro do app passa pelo componente normal |

## Validação e limites de aceite

Testes de domínio cobrem prioridade de variantes, duplicatas, URLs, TTL/relógio, modos e identidade. Testes SQL exercitam observação em lote, reversão de original-only, preservação da origem, writes paralelos e clear. Testes do interceptor usam requests reais Coil e uma cadeia/decodificador representados por resultados sintéticos; validam fallback, chaves explícitas, cache-only, revisão, cancelamento e falha de armazenamento. Teste separado escreve/lê **DiskCache real** e reabre o cache offline, com bytes sintéticos autorais. Transporte usa callbacks HTTP fake e não faz tráfego externo.

Essas evidências não equivalem a decoder Android, UI, widget físico, stress multithread ou benchmark de scroll. ADB consultado, sem dispositivo conectado; emulação permanece bloqueada desde o baseline. Aceite online/offline visual e ausência de regressão perceptível exigem Android utilizável e comparação no mesmo dispositivo; **aceite integral da sessão 7 permanece pendente desses gates**. Sem cobertura de linhas medida. Lint integral do app continua inconclusivo desde a sessão 4; checks de data/widgets registrados separadamente. Resultados efetivamente executados em [SESSION_7_COMMANDS.md](../evidence/SESSION_7_COMMANDS.md) e [SESSION_7.json](../evidence/SESSION_7.json).

Composição conferida nas interfaces/fontes oficiais do [interceptor](https://github.com/coil-kt/coil/blob/3.6.3/coil-core/src/commonMain/kotlin/coil3/intercept/Interceptor.kt), [cadeia](https://github.com/coil-kt/coil/blob/3.6.3/coil-core/src/commonMain/kotlin/coil3/intercept/RealInterceptorChain.kt) e [engine Coil 3.6.3](https://github.com/coil-kt/coil/blob/3.6.3/coil-core/src/commonMain/kotlin/coil3/intercept/EngineInterceptor.kt). Sem nova consulta AniList/CDN ou download de imagens reais nesta sessão.
