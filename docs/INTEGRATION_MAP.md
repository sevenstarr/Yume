# Mapa dos pontos de integração — Sessão 0

Base auditada: `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`. Caminhos abaixo são relativos a `../Animiru/`. **Nenhum arquivo de código foi alterado.** “Modificar” significa proposta para sessões futuras; não autorização de execução.

## 1. Arquivos existentes a modificar na proposta

| Caminho exato / classe ou função | Evidência e alteração proposta |
|---|---|
| `app/src/main/java/eu/kanade/tachiyomi/App.kt` — `newImageLoader`, linhas 214–248 | Registro central das duas factories e keyers. Registrar interceptor/fetcher/keyer novos e resolver via DI. |
| `app/src/main/java/eu/kanade/tachiyomi/data/coil/AnimeImageFetcher.kt` — `fetch`, `executeNetworkRequest`, `AnimeFactory`, `AnimeCoverFactory` | Preservar manual/original; expor/adaptar o caminho de fonte para composição com fallback. Separar cliente/headers do ramo externo. Arquivo também trata background; não enriquecer esse ramo. |
| `app/src/main/java/eu/kanade/tachiyomi/data/coil/AnimeCoverKeyer.kt` — `AnimeKeyer`, `AnimeCoverKeyer` | Keys atuais usam URL/ID e `coverLastModified`, sem revisão externa nem distinção completa de bypass. Adicionar chaves de origem/revisão/modo para evitar capa antiga ou fallback armazenado sob identidade errada. |
| `app/src/main/java/eu/kanade/presentation/anime/components/AnimeCover.kt` — `AnimeCover.invoke` | Wrapper comum de `AsyncImage`. Observar revisão/preferência e incorporar snapshot resolvido ao request. Aceita `Any?`, inclusive `ImageRequest`; não assumir que sempre recebe uma URL. |
| `app/src/main/java/eu/kanade/presentation/anime/components/AnimeInfoHeader.kt` — requests em 405–412 e 444–452 | Capa de detalhes já vem dentro de `ImageRequest`; adaptar a observação/revisão. Backdrop em 137–144 usa `useBackground(true)` e permanece fora do enriquecimento de capa. |
| `app/src/main/java/eu/kanade/presentation/anime/components/AnimeImagesDialog.kt` e `app/src/main/java/eu/kanade/tachiyomi/ui/anime/AnimeImageViewModel.kt` | Diálogo/ações de imagem. Distinguir original/externa/local manual e permitir remover apenas o enriquecimento; requests de salvar/compartilhar precisam da mesma política explícita. |
| `app/src/main/java/eu/kanade/tachiyomi/ui/anime/AnimeScreen.kt` e `AnimeViewModel.kt` | Ligar ação de pesquisa/associação/revisão; adição à biblioteca já ocorre neste ViewModel. Não gravar metadados externos por `updateAnimeInfo`, nem misturar com tracking. |
| `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsMainScreen.kt` | Entrada para configuração opt-in e limpeza; usar nova tela própria e recursos Moko. |
| `app/src/main/java/mihon/app/di/AppGraph.kt` | Grafo Metro com bindings/injeções. Disponibilizar coordenador/resolver necessários e injeção do novo worker. |
| `app/src/main/java/eu/kanade/tachiyomi/data/track/anilist/AnilistApi.kt` | Compartilhar orçamento/transportes públicos quando extraído, mantendo o caminho OAuth. Busca em 139–194 e detalhes em 307+ hoje usam `authClient`; não chamar para impor login em capas públicas. |
| `app/src/main/java/eu/kanade/tachiyomi/util/AniChartApi.kt` e `app/src/main/java/eu/kanade/tachiyomi/ui/player/utils/AniSkipApi.kt` | Chamadas AniList com clientes próprios. Conectar apenas o transporte/orçamento por host, com regressão de airing e conversão de IDs. Nenhuma alteração de algoritmos de player/episódios proposta. |
| `presentation-widget/src/main/java/tachiyomi/presentation/widget/BaseUpdatesGridGlanceWidget.kt` — `prepareData`, 125–157 | Constrói `AnimeCover` e executa ImageLoader sem composable. Usar resolver/revisão comum e atualizar widget quando a seleção mudar. Contrato compartilhado deve ficar em `domain`; widget não depende de `app`. |
| `app/src/main/java/eu/kanade/tachiyomi/data/library/LibraryUpdateNotifier.kt` — `getAnimeIcon`, 294–301 | Notificação carrega `Anime` diretamente no Coil. Passar pelo caminho comum, sem disparar pesquisa de metadados ao notificar. |
| `app/src/main/java/eu/kanade/tachiyomi/data/backup/models/BackupAnime.kt`, `app/src/main/java/eu/kanade/tachiyomi/data/backup/create/creators/AnimeBackupCreator.kt`, `app/src/main/java/eu/kanade/tachiyomi/data/backup/restore/restorers/AnimeRestorer.kt` | Pontos confirmados de serialização/restauração. Acrescentar associação opcional separada e remapeamento por fonte/URL; decidir representação final na sessão de persistência. Verificar a omissão herdada de campos `custom*` em `getAnimeImpl()`. |
| `data/src/main/java/tachiyomi/data/backup/RestoreRepositoryImpl.kt` | Persistência transacional da restauração; coordenar vínculo novo somente após localizar/criar o anime. |
| `i18n-animiru/src/commonMain/moko-resources/base/strings.xml` | Textos traduzíveis de opt-in, revisão, origem, limpeza e falhas. |

Mudanças em dependências de build só devem ocorrer se necessárias à implementação: o stack existente já oferece os recursos principais. Os caminhos novos abaixo usam organização preliminar própria, sem renomear módulos.

## 2. Arquivos novos propostos, ainda inexistentes

| Caminho proposto | Conteúdo |
|---|---|
| `domain/src/main/java/tachiyomi/domain/metadata/service/MetadataProvider.kt` | Contrato de busca/detalhes públicos |
| `domain/src/main/java/tachiyomi/domain/metadata/model/MetadataCandidate.kt` | Dados remotos normalizados, nulos e variantes de título |
| `domain/src/main/java/tachiyomi/domain/metadata/model/MetadataAssociation.kt` | Associação, evidência, estado e preferência manual |
| `domain/src/main/java/tachiyomi/domain/metadata/model/ResolvedCover.kt` | Snapshot imutável com original, escolha e revisão para keys |
| `domain/src/main/java/tachiyomi/domain/metadata/service/CoverResolver.kt` | Contrato compartilhável com widget e UI |
| `domain/src/main/java/tachiyomi/domain/metadata/repository/MetadataRepository.kt` | Leitura observável, atualização transacional e limpeza |
| `domain/src/main/java/tachiyomi/domain/metadata/interactor/AnimeMatcher.kt` | Correspondência conservadora, sem escrever a fonte |
| `data/src/main/sqldelight/tachiyomi/data/anime_metadata.sq` | Tabela e queries separadas |
| `data/src/main/sqldelight/tachiyomi/migrations/148.sqm` | Próxima migração **somente se 147 continuar sendo a última quando implementada** |
| `data/src/main/java/tachiyomi/data/metadata/MetadataRepositoryImpl.kt` | Implementação SQLDelight/Metro |
| `app/src/main/java/eu/kanade/tachiyomi/data/metadata/anilist/AniListProvider.kt` | Adapter público AniList |
| `app/src/main/java/eu/kanade/tachiyomi/data/metadata/anilist/AniListMetadataDtos.kt` | DTOs GraphQL públicos, sem reutilizar estado mutável de tracking |
| `app/src/main/java/eu/kanade/tachiyomi/data/metadata/DefaultCoverResolver.kt` | Precedência, snapshot e observação de revisão |
| `app/src/main/java/eu/kanade/tachiyomi/data/coil/MetadataCoverInterceptor.kt` | Tentativa externa e fallback após `ImageResult` de erro |
| `app/src/main/java/eu/kanade/tachiyomi/data/coil/ResolvedCoverKeyer.kt` | Chave que distingue externa/original/manual/bypass |
| `app/src/main/java/eu/kanade/tachiyomi/data/metadata/MetadataSyncCoordinator.kt` e `MetadataSyncJob.kt` | Coordenação e worker independente na sessão de sync |
| `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsMetadataScreen.kt` | Opt-in e controle de uso de dados/limpeza |
| `app/src/main/java/eu/kanade/tachiyomi/ui/anime/metadata/MetadataSelectionDialog.kt` | Busca, candidatos, estados e escolha/correção manual |

São nomes de arquivos propostos, não classes alegadamente herdadas. DTOs, bindings, testes e helpers podem ser divididos em arquivos menores na Sessão 3, preservando os mesmos limites arquiteturais.

## 3. Superfícies de capa localizadas

| Superfície | Arquivos observados | Tratamento |
|---|---|---|
| Biblioteca (grids/lista) | `app/src/main/java/eu/kanade/presentation/library/components/LibraryCompactGrid.kt`, `LibraryComfortableGrid.kt`, `LibraryList.kt`, `CommonAnimeItem.kt` | Constroem/recebem `AnimeCover`. Cobrir pelo modelo/pipeline comuns e recomposição por revisão; não alterar ordenação/título da biblioteca. |
| Detalhes, temporadas e relacionados | `app/src/main/java/eu/kanade/presentation/anime/components/AnimeInfoHeader.kt`, `AnimeSeasonListItem.kt`, `RelatedAnimeRows.kt`, `BaseAnimeListItem.kt`, `DuplicateAnimeDialog.kt` | Pai/temporada requer associação própria. Controles de capa não podem atingir backdrop ou preview de episódio. |
| Histórico | `app/src/main/java/eu/kanade/presentation/history/components/HistoryItem.kt`; `domain/src/main/java/tachiyomi/domain/history/model/HistoryWithRelations.kt` | Usa `coverData: AnimeCover`. Não precisa substituir `thumbnail_url` na view. |
| Atualizações | `app/src/main/java/eu/kanade/presentation/updates/UpdatesUiItem.kt`; `domain/src/main/java/tachiyomi/domain/updates/model/UpdatesWithRelations.kt` | Mesmo caminho de `AnimeCover`; preservar dados de episódios. |
| Widgets | `presentation-widget/src/main/java/tachiyomi/presentation/widget/BaseUpdatesGridGlanceWidget.kt` | Carregamento imperativo e atualização explícita do widget. |
| Notificação de biblioteca | `app/src/main/java/eu/kanade/tachiyomi/data/library/LibraryUpdateNotifier.kt` | `ImageRequest.data(anime)` atende factory de `Anime`. |
| Busca de tracker | `app/src/main/java/eu/kanade/presentation/track/TrackerSearch.kt` — usa `TrackSearch.cover_url` | Já mostra miniatura remota; manter preview de candidato, sem criar associação visual silenciosa. |
| Catálogo/busca de fonte | Resultados em `Anime`/`AnimeCover` passam pela pipeline; URLs puras não trazem identidade local | Sem ID local/associação, manter capa da fonte. Não executar matching durante scroll. |
| Player/cast externos | `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerActivity.kt:766`, `ExternalIntents.kt:143` passam `anime.thumbnailUrl` diretamente | Esse caminho não usa Coil. Preservar URL original no MVP; enriquecer metadados entregues a outro app/cast exige aprovação específica e tratamento próprio. |

Esta é a cobertura identificada por leitura e buscas estáticas, não uma alegação de navegação/teste de todas as telas. Na sessão visual será necessário repetir a busca global por carregamentos diretos e provar a cobertura em execução.

## 4. Fronteiras herdadas a preservar

- `source-api/src/main/kotlin/eu/kanade/tachiyomi/animesource/model/SAnime.kt`: contrato consumido por extensões; não adicionar capa do provedor ao objeto enviado à fonte.
- `app/src/main/java/eu/kanade/tachiyomi/extension/ExtensionManager.kt` e `source/AndroidSourceManager.kt`: carregamento e seleção das fontes; sem mudanças funcionais previstas.
- `app/src/main/java/mihon/domain/source/interactor/UpdateAnimeFromRemote.kt`: refresh original e sincronização de episódios/temporadas; apenas observar conclusão por camada independente se algum gatilho for aprovado depois.
- `app/src/main/java/eu/kanade/tachiyomi/data/library/MetadataUpdateJob.kt` e `LibraryUpdateJob.kt`: manter seus trabalhos originais; não renomear o novo enriquecimento como se fosse esse job existente.
- `domain/src/main/java/tachiyomi/domain/anime/model/Anime.kt`, `AnimeCover.kt`, `data/src/main/sqldelight/tachiyomi/data/animes.sq` e `data/src/main/java/tachiyomi/data/anime/AnimeMapper.kt`: representam originais e customizações atuais. A proposta preferida usa tabela/modelo resolvido separado, evitando reescrever esses campos em todas as views.
- `app/src/main/java/eu/kanade/tachiyomi/ui/anime/EditAnimeDialog.kt` e `AnimeViewModel.updateAnimeInfo`: conservar edição de texto existente, independente da seleção externa.
