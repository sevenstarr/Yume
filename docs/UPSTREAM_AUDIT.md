# Sessão 0 — Diagnóstico técnico do Animiru original

Data da inspeção: **19/10/2026**, referência de calendário `America/Sao_Paulo`.

**Status: reconhecimento concluído. Viabilidade técnica confirmada pela inspeção estática e por uma consulta pública ao AniList; build, instalação e conformidade de uso ainda não validados. Arquitetura preliminar aguardando aprovação. Sessão 1 não iniciada.**

## 1. Escopo e preservação da origem

O arquivo `C:\Users\Administrator\Downloads\prompt.md` foi lido integralmente. Foram executadas apenas as tarefas da Sessão 0. O workspace `C:\Users\Administrator\Documents\Yume` estava vazio e não era um repositório Git. Não havia `AGENTS.md` nos diretórios ancestrais verificados nem nos arquivos da cópia obtida.

Foi criada uma **cópia local de inspeção**, em `../Animiru`, pelo comando `git clone --depth 1 --no-single-branch https://github.com/animiruorg/Animiru.git Animiru`. Isso não cria um fork no GitHub. Os relatórios estão fora da árvore Git do upstream, em `Yume/docs`, para preservar o código original intacto. Nenhum arquivo rastreado foi alterado; `git diff --exit-code`, `git diff --cached --exit-code` e `git status --porcelain=v1` confirmaram a árvore limpa.

Não foram criados fork remoto, branch de implementação, commit, tag, PR, issue, release, APK, credencial ou mudança de marca/configuração. O protocolo genérico de commits será aplicado quando houver repositório de desenvolvimento autorizado; não se atribui ao novo projeto a autoria do código examinado. Esta documentação foi produzida com assistência do Codex/IA.

Documentos complementares:

- [Mapa exato de integração](INTEGRATION_MAP.md).
- [Matriz de riscos](RISK_MATRIX.md).
- [Proposta preliminar de arquitetura](ARCHITECTURE.md).
- [Comandos, verificações e limitações](evidence/COMMANDS.md).
- [Snapshot estruturado das evidências](evidence/SESSION_0.json).
- [Registro de progresso](PROGRESS.md).

## 2. Origem, estado e escolha da base

| Item | Evidência confirmada |
|---|---|
| Repositório canônico do Animiru | [animiruorg/Animiru](https://github.com/animiruorg/Animiru), público, ID GitHub `468685161`, não arquivado |
| Confirmação externa da origem | A [página do Animiru no site Aniyomi](https://aniyomi.org/forks/Animiru/) aponta para `Quickdesh/Animiru`; esse endereço redirecionou para `animiruorg/Animiru`. O README declara derivação do Aniyomi e o GitHub registra essa relação. Isso confirma a origem pública indicada pelo projeto; não é uma verificação de identidade jurídica dos mantenedores. |
| Branch padrão real | `animiru-new-main`, não `main` nem `master` |
| Commit auditado | [`0b9bb0a347aa7bcee226d8782d98d9abafc34b32`](https://github.com/animiruorg/Animiru/commit/0b9bb0a347aa7bcee226d8782d98d9abafc34b32), 16/10/2026, `More downloader fixes (#242)` |
| Release estável mais recente consultada | [`v0.20.0.1`](https://github.com/animiruorg/Animiru/releases/tag/v0.20.0.1), publicada em 14/09/2026; APK universal, quatro ABIs e universal FOSS |
| Tags recentes | `v0.20.0.1`, `v0.20.0.0`, `v0.19.8.1`, `v0.19.8.0`, `v0.19.7.9`; lista de tags inspecionada com Git |
| Diferença release → HEAD | `git diff --shortstat v0.20.0.1 HEAD`: **720 arquivos**, 11.312 inserções, 9.004 exclusões. A arquitetura descrita aqui é a do HEAD auditado; não se presume equivalência com a release. |
| Profundidade local | Clone raso: `git rev-parse --is-shallow-repository` = `true`; `git rev-list --count HEAD` = `1`. Tags/refs estão disponíveis, mas o histórico completo não foi obtido. |

Branches remotas observadas: `animiru-new-main`, `animiru-tv-main`, `animiru-video-player`, `copy_file_loc`, `master`, `merge`, `retired-code`. A última apareceu protegida no retorno da API. Não foi feita auditoria funcional das branches alternativas nem de toda a história. SHAs constam do snapshot.

**Recomendação para aprovação:** fixar o commit acima como base de referência da próxima etapa, antes de qualquer mudança. Se a preferência for a tag estável, os pontos de integração precisam de revalidação: o HEAD já contém mudanças recentes em metadados personalizados e esquema.

## 3. Licença, contribuições e identidade

O `LICENSE` completo é Apache-2.0. Seu texto exige entregar a licença, indicar arquivos modificados, preservar avisos pertinentes e reproduzir atribuições de `NOTICE` quando aplicável. A cláusula 6 não concede uma autorização geral de marcas. O README atribui créditos a Javier Tomás, Mihon e Aniyomi; esses avisos e o histórico devem permanecer. Não foi encontrado arquivo `NOTICE` rastreado na árvore consultada.

O `CONTRIBUTING.md`, linhas 64–77, autoriza forks condicionados à licença e pede mudança de **nome, ícone, applicationId e alteração/desativação do atualizador**. Há links antigos para a branch `main`; os arquivos efetivos foram localizados na branch padrão correta. As linhas 29–34 também pedem marcação `AM (...)` em código adicionado/editado; a convenção de atribuição do futuro fork deve preservar a distinção entre herança, alterações novas e assistência por IA. O README pede discussão prévia para mudanças grandes enviadas ao upstream. Nenhuma discussão foi aberta nesta sessão.

Não foi localizado um documento separado de política de marcas. Isso não transforma nome/ícone em recursos livres de restrição. A declaração Apache do repositório também não comprova a licença de todas as bibliotecas/binários nativos: mpv, FFmpeg e demais dependências precisam de inventário e avisos antes de distribuir APK. `app/build.gradle.kts` exclui vários arquivos `META-INF/LICENSE/NOTICE` do empacotamento e usa AboutLibraries; será necessário verificar o resultado efetivo, sem inferir que os avisos já estejam completos.

O `.gitignore` atual exclui `keystore.properties` e `local.properties`, mas não contém regras amplas para `*.jks`, `*.keystore`, `.env` ou APKs. Isso é uma pendência da fundação do fork; não foi executado scanner completo de segredos/dependências nesta sessão.

## 4. Arquitetura efetivamente encontrada

Aplicativo Android majoritariamente Kotlin, com Gradle Kotlin DSL. Não é um app Flutter, React Native ou um projeto baseado em Room. A UI usa **Jetpack Compose/Material 3**, além de infraestrutura Android/Views existente e navegação Voyager. Estado usa coroutines/Flow/ViewModel. DI está em transição: **Metro**, com ponte e usos legados de **Injekt**. Não se deve introduzir Hilt ou um segundo cliente de imagens apenas para este recurso.

| Módulo incluído em `settings.gradle.kts` | Função observada |
|---|---|
| `:app` | Composição da aplicação, UI, trackers, imagens, atualização, player, downloads, backup, workers e DI (`mihon/app/di/AppGraph.kt`) |
| `:domain` | Modelos `Anime`, `AnimeCover`, episódios, histórico, tracking; contratos de repositórios e interactors |
| `:data` | SQLDelight, consultas/esquema/migrações, mappers e implementações dos repositórios |
| `:core:common` | HTTP/OkHttp, preferências e utilitários compartilhados |
| `:core:archive`, `:core:metro` | Arquivos/arquivos compactados e infraestrutura de DI |
| `:core-metadata` | DTOs `AnimeDetails`/`EpisodeDetails` para metadados locais; **não é um agregador de APIs externas** |
| `:source-api`, `:source-local` | Contratos de extensões/fontes e biblioteca local; manter compatibilidade |
| `:presentation-core`, `:presentation-widget` | Componentes UI comuns e widgets Glance |
| `:i18n`, `:i18n-aniyomi`, `:i18n-animiru` | Strings/recursos Moko; usar recursos traduzíveis existentes |
| `:icons:material-symbols`, `:icons:simple-icons` | Ícones |
| `:cast` | Casting; inclusão funcional controlada pela distribuição/propriedade |
| `:baseline-profile` | Testes/perfis de inicialização e desempenho; aparece duas vezes em `settings.gradle.kts`, sem diagnóstico de falha executado |

O build composto está em `gradle/build-logic`. O player usa `io.github.secozzi:mpv-android-lib` 0.1.14, além de FFmpeg-kit 1.18 e TorrServer 0.1.1 no catálogo Aniyomi. Downloads/player são áreas herdadas sensíveis e não constituem o local da integração de capas.

### Banco e identidade

- **SQLDelight 2.4.0**, SQLite bundled (`androidx-sqlite` 2.7.1) e driver AndroidX (`sqldelight-androidx-driver` 0.2.2). `DatabaseBindings.kt`, linhas 22–29, cria `tachiyomi.animedb` com FKs habilitadas.
- Esquema e consultas: `data/src/main/sqldelight/tachiyomi/data/`; views: diretório `view/`; migrações presentes de `113.sqm` até `147.sqm`. A última adiciona metadados personalizados e atualiza views. Não se deve editar uma migração já publicada.
- `animes._id` é PK local; `source` e `url` guardam identidade de origem. `getAnimeByUrlAndSource` usa o par com `LIMIT 1`, **sem restrição UNIQUE desse par**. Não adicionar uma restrição global sem análise de duplicatas.
- Existem `parent_id`, `fetch_type`, `season_number` e relações de temporadas. Um registro pai pode agregar temporadas; não assumir correspondência 1:1 com uma entrada AniList.
- Histórico, episódios, progresso e vínculos de tracking têm armazenamento próprio. A nova associação de metadados não deve reutilizar `anime_sync` como depósito de capas.

## 5. Fluxo real de título, capa e refresh

1. As fontes fornecem `SAnime.title` e `SAnime.thumbnail_url` em `source-api/.../animesource/model/SAnime.kt`. `NetworkToLocalAnime` delega ao `AnimeRepository.insertNetworkAnime` para inserção/resolução local.
2. `animes.sq` persiste `title` e `thumbnail_url`. `AnimeMapper.kt` constrói o modelo de domínio: `ogTitle` representa o título original e `thumbnailUrl` a capa original. `customTitle` e os outros `custom*` são campos separados.
3. `Anime.title` retorna `customTitle ?: ogTitle`. Já existe edição manual em `EditAnimeDialog` → `AnimeViewModel.updateAnimeInfo` (linhas 528–596) → `updateCustomInfo`. **Não há campo equivalente de URL externa com provedor, confiança ou estado de associação.**
4. `Anime.asAnimeCover()` gera `AnimeCover(animeId, sourceId, isAnimeFavorite, url, lastModified)`. Biblioteca, histórico e atualizações usam esse modelo ou seus equivalentes construídos nas telas.
5. `App.newImageLoader`, linhas 214–248, registra `AnimeImageFetcher.AnimeFactory`, `AnimeCoverFactory`, `AnimeKeyer` e `AnimeCoverKeyer` no Coil 3.6.3.
6. `AnimeImageFetcher.fetch`, linhas 63–80, usa primeiro arquivo de capa personalizada, depois URL/arquivo/URI original. Para itens da biblioteca utiliza `CoverCache`; para os demais, DiskCache do Coil. `CoverCache` armazena capa da fonte pelo hash da URL e capa manual pelo hash do ID em `covers/custom`.
7. `AnimeImageViewModel.editImage/deleteCustomImage` e `util/AnimeExtensions.kt` permitem importar/remover capa manual; `UpdateAnime.awaitUpdateCoverLastModified` invalida a identidade visual. `USE_CUSTOM_COVER_KEY` já existe, mas keyers atuais não distinguem esse bypass; a futura visualização da original precisará de chaves separadas.
8. `UpdateAnimeFromRemote.awaitUpdateFromSource`, linhas 186–266, preserva o título dos favoritos no refresh, atualiza `thumbnailUrl` da fonte e invalida seu cache quando necessário. Alteração de título também pode renomear diretório de download. O enriquecimento não deve chamar esse caminho para escrever título/capa externos.
9. `MetadataUpdateJob` é um worker de **refresh de detalhes das fontes**, não um matcher externo. Ele chama `UpdateAnimeFromRemote.awaitEpisodesUpdate(fetchDetails = true)`. `LibraryUpdateJob` trata atualização da biblioteca/episódios. Ambos devem manter a função herdada.

**Implicação:** substituir `thumbnailUrl` ou gravar capa AniList em `covers/custom` seria um atalho inadequado: confundiria origem, prioridade manual, cache e restauração. Há um ponto central aproveitável no Coil, mas também há consumidores fora do componente Compose. O [mapa de integração](INTEGRATION_MAP.md) registra todos os caminhos identificados e as alterações propostas.

## 6. Integrações externas já existentes

| Integração | Evidência e conclusão |
|---|---|
| AniList tracker | `app/.../data/track/anilist/Anilist.kt`, `AnilistApi.kt`, `AnilistInterceptor.kt`, `dto/ALSearchItem.kt`. Busca, detalhes por ID, autenticação, listas e progresso já existem. |
| Dados de capa AniList | Busca/detalhes pedem `coverImage.large` e `title.userPreferred`. `ALAnime.toTrack` copia a URL para `TrackSearch.cover_url`, usada no preview de seleção do tracker. Isso **não substitui a capa da biblioteca**. |
| Dependência de login atual | Busca e `getAnimeDetails` passam por `authClient`; o interceptor rejeita ausência de token. Não reutilizar esse fluxo como pré-requisito para capas públicas. |
| Limite existente | `AnilistApi.kt`, linhas 40–43, aplica **25/minuto apenas à instância authClient**. Não é orçamento global do app. |
| Outros acessos AniList | `util/AniChartApi.kt` consulta airing/conversão MAL→AniList; `ui/player/utils/AniSkipApi.kt` consulta AniList→MAL. Ambos criam `OkHttpClient()` próprios. Um novo cliente com limiter isolado não protege o tráfego combinado. |
| Demais trackers | `TrackerManager` registra MAL, AniList, Kitsu, Shikimori, Bangumi, Simkl, Jellyfin e Hikka. Vínculo AniList já escolhido pelo usuário pode servir de candidato confiável, com validação do ID e sem gravar progresso. |
| TMDB/Jikan | Busca `rg -n -i 'tmdb|themoviedb|jikan' -g '*.kt' -g '*.kts' -g '*.toml'` não encontrou implementação no código versionado consultado. Isso não abrange código distribuído por extensões externas. |
| Framework de enriquecimento | Não foi encontrado contrato `MetadataProvider` ou matcher/persistência de capa externa na árvore consultada. A infraestrutura HTTP, serialização e DI deve ser reaproveitada; os comportamentos específicos são novos. |

## 7. AniList: contrato atual, limites e restrições

O site `docs.anilist.co` retornou 403 na leitura direta pelo navegador de pesquisa. Para verificar o conteúdo, foram lidos os arquivos do **repositório oficial [AniList/docs](https://github.com/AniList/docs/tree/03281c0a4bbf0c7f2097e0c935cddaed1096aa65)**, fixado no commit `03281c0a4bbf0c7f2097e0c935cddaed1096aa65`. Não se trata de documentação de terceiros. Foram consultadas também referências oficiais de `Media`/`Query`.

- Endpoint GraphQL: `POST https://graphql.anilist.co`, JSON com `query`/`variables`, `Content-Type` e `Accept: application/json`. Dados públicos de anime não exigem login; operações privadas/de usuário exigem OAuth. Isso foi confirmado por [documentação de autenticação](https://docs.anilist.co/guide/auth/) e pela consulta pública executada.
- A [documentação de rate limiting](https://docs.anilist.co/guide/rate-limiting) anuncia 90/min nominais, mas mantém aviso de operação degradada a **30/min**. A resposta real desta sessão informou `X-RateLimit-Limit: 30` e `Remaining: 29`. Respeitar limites retornados, `429`, `Retry-After`, `X-RateLimit-Reset` e proteção contra rajadas. Não se realizou teste de saturação/429.
- [MediaCoverImage](https://docs.anilist.co/reference/object/mediacoverimage) expõe `extraLarge`, `large`, `medium`, `color`; `extraLarge` pode devolver a imagem large quando a maior não existir. São URLs, sem garantia de dimensão, extensão de arquivo ou resolução superior à fonte. O schema permite ausência de valores; tratar nulos. Não construir URLs trocando segmentos do caminho.
- A consulta de `Media(id:1,type:ANIME)` retornou **HTTP 200** com títulos romaji/english/native, sinônimos, formato, temporada, ano, capas e banner. O HEAD da URL retornada como `extraLarge` retornou `image/png` e 489.507 bytes. Não foi baixado binário nem medida a resolução; PNG é o formato desta amostra, não uma garantia universal. URLs retornadas no snapshot mostram que nomes de diretórios não equivalem aos nomes dos campos GraphQL.
- Busca por nome deve usar resultados paginados, `type: ANIME` e validação de candidatos. A [documentação de busca](https://docs.anilist.co/guide/graphql/queries/media) esclarece que títulos não identificam univocamente a obra. Não se confirmou busca ao vivo nem o comportamento de todos os campos/erros; estes seguem para testes futuros.

Os [termos de uso oficiais](https://docs.anilist.co/guide/terms-of-use) proíbem coleta massiva, uso como armazenamento/backup e serviços concorrentes não complementares de listas/tracking; a restrição também inclui dados de mídia. Há regras de nome e condições comerciais (abaixo de US$150/mês sem licença comercial; acima disso exige contato). O texto não resolve expressamente o valor exato de US$150. **Inferência para este projeto:** um gerenciador local com tracker pode ser complementar, mas a existência da integração herdada não comprova autorização para o novo enriquecimento. Confirmar enquadramento/obter esclarecimento do provedor antes do uso público em escala. Nenhum contato externo foi realizado.

Não foi encontrada nos arquivos consultados uma licença geral que autorize redistribuir as ilustrações como conteúdo do APK/repositório. A proposta utiliza apenas URLs e cache de uso do app, sem empacotar capas; isso não substitui a verificação dos direitos e políticas aplicáveis. Aprovação da arquitetura pelo proprietário não equivale a autorização do AniList.

## 8. Build, ambiente e instalação

As versões abaixo são as **declaradas no código auditado**, não dependências resolvidas em um build nesta máquina.

| Requisito | Valor | Fonte |
|---|---|---|
| Gradle Wrapper | 9.7.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 9.4.1 | `gradle/libs.versions.toml:4` |
| Kotlin | 2.4.20 | `gradle/libs.versions.toml:49` |
| JDK de execução no CI | **21** | `.github/.java-version` e ambos os workflows |
| Compatibilidade Java do código | **17** | `gradle/mihon.versions.toml`, aplicada por `configurations/Kotlin.kt`; distinta do JDK do CI |
| compile SDK | **37.2** | catálogo Mihon + `PluginAndroidBase.kt` + `extensions/CompileSdkSpec.kt` (major 37, minor 2) |
| target / min SDK | **36 / 26** | catálogo Mihon; Android 8.0+ confirmado também no README |
| NDK declarado | 29.0.14206865 | catálogo Mihon; necessidade efetiva de download/compilação não executada |
| HTTP / serialização | OkHttp 5.5.0, Kotlin serialization 1.11.0 | catálogo principal |
| Jobs | WorkManager 2.11.2 | catálogo e workers existentes |
| Repositórios de dependências | Google, Maven Central, JitPack, Gradle Plugin Portal para plugins | `settings.gradle.kts` |
| Memória Gradle | `-Xmx4g`; paralelismo e isolated projects ativos | `gradle.properties` |

Inventário local: Git 2.53.0.windows.3; Java Oracle **25.0.4** no PATH; `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT` ausentes; sem `adb`, `sdkmanager`, `emulator` ou `gh` no PATH; pasta padrão `%LOCALAPPDATA%\Android\Sdk` e `local.properties` ausentes. Não se conclui que inexista SDK em qualquer outro local. Nenhum pacote foi instalado, licença de SDK aceita ou configuração alterada. Recomenda-se alinhar o futuro ambiente ao JDK 21 do CI, sem presumir incompatibilidade universal do Java 25.

O build usa informações Git (`BuildInfo.kt`: `rev-list --count`, SHA e timestamp). Um clone raso como este altera `COMMIT_COUNT`; ele é apropriado para esta inspeção, mas o build de referência deve definir checkout/histórico reproduzível. Não houve verificação de disponibilidade de todos os artefatos Maven/JitPack ou compatibilidade real das versões declaradas.

### Conflitos de instalação e dados

- `applicationId = xyz.Quickdev.Animiru.mi`, namespace `eu.kanade.tachiyomi`, versão 0.20.0.1/código 153. Sufixos: `.dev`, `.foss`, `.preview`, `.benchmark`.
- ABIs declaradas: armeabi-v7a, arm64-v8a, x86, x86_64 e APK universal. Futuro dispositivo/emulador deve corresponder à ABI e Android >=26; validar bibliotecas nativas e playback real. O projeto já descreve GMD Pixel 6/API34/imagem Google no módulo baseline-profile; isso não constitui teste completo de regressão nem foi provisionado aqui.
- Mesmo applicationId com assinatura diferente impede atualização normal sobre o original. Mudar ID permite coexistência, mas não transfere banco/dados privados automaticamente. Não desinstalar nem limpar dados para testar.
- `AndroidManifest.xml` usa `allowBackup=false`: a migração deve empregar o backup interno do app, preservando biblioteca, episódios, histórico e progresso. Downloads/arquivos e capas locais exigem verificação própria; não foi provada a transferência desses binários no backup.
- Deep links OAuth usam `animiru://` com hosts como `anilist-auth`; instalação paralela pode disputar handlers. Links `aniyomi://add-repo`/`extension-store` também existem. `FileProvider` e Shizuku usam authorities derivadas de `${applicationId}`. Decidir identidade/deep links na Sessão 1.
- `AppUpdateChecker.kt` aponta para `animiruorg/Animiru` ou `animiruorg/animiru-preview`. `BuildConfig.kt` desliga updater em distribuição LOCAL, mas o habilita por padrão nas demais; o fork deve ter uma política própria antes de distribuição.
- Release herda `debug.signingConfig`; o workflow oficial troca suas credenciais via ambiente. Isso não dá ao fork acesso à assinatura upstream. Não gerar nem versionar keystore nesta sessão.

## 9. CI e testes existentes

`.github/workflows/build.yml` executa `spotlessCheck`, `testDebugUnitTest`, `verifySqlDelightMigration`, `assembleRelease -Pdist=ci` e publica artifacts. Permissão global é `contents: read`; não usa `pull_request_target`. **O push está limitado a `main`, embora a branch padrão seja `animiru-new-main`**: não assumir cobertura de todo push do HEAD. O release workflow dispara por branch `release`/tags `v*`, usa secrets e possui guarda `github.repository == 'animiruorg/Animiru'`; cria release draft. Deve ser revisado futuramente antes de habilitar publicação do fork.

A API GitHub retornou sucesso nos cinco runs recentes consultados, incluindo [PR downloader-fix de 16/10/2026](https://github.com/animiruorg/Animiru/actions/runs/38907846971). **Eles não são o SHA auditado**. Consulta filtrada pelo HEAD retornou `total_count = 0`; não há evidência consultada de CI verde para o commit exato. Não foram lidos todos os logs desses runs.

Foram localizadas seis classes de testes unitários: `MigratorTest` no app; `FetchIntervalTest`, `EpisodeRecognitionTest`, `MissingEpisodesTest`, `LibraryFlagsTest`, `GetApplicationReleaseTest` no domain. Baseline-profile possui geradores e benchmark de inicialização. Não foram encontrados testes dedicados a metadados/capas/AniList ou arquivos `*.db` de snapshot SQLDelight na árvore inspecionada; a existência da tarefa de migração no CI não prova cobertura do novo esquema.

Há ainda um risco concreto no backup herdado: `AnimeBackupCreator.toBackupAnime` grava campos `custom*`, mas `BackupAnime.getAnimeImpl()` não os repõe, e `AnimeRestorer` usa esse método. A perda de personalizações é um **defeito candidato identificado estaticamente**, sem reprodução em execução nesta sessão. Antes de qualquer migração do usuário, incluir uma prova de ida e volta desses campos no baseline.

## 10. Parecer de viabilidade e aprovação

**Viável tecnicamente**, pois já existem identidade local por fonte/URL, persistência versionada, cliente HTTP, serialização, coroutines, DI, Coil com fetchers/keyers próprios e WorkManager. A consulta pública confirmou os campos necessários do AniList. Não é necessário alterar contratos de extensões, episódios ou player para enriquecer a apresentação de capas.

**Viabilidade condicionada** à validação de build/backup nas sessões próprias e ao esclarecimento dos termos do AniList. A arquitetura proposta mantém a URL original, prioriza escolhas manuais e separa metadados externos de tracking. O mapa inclui exceções de widgets, notificações e URLs entregues a outros apps; não se promete cobertura de todas as superfícies apenas mudando um composable.

Decisões solicitadas ao proprietário:

1. Aprovar **a direção preliminar** de camada separada + SQLDelight + resolução central no Coil + opt-in desligado, descrita em `ARCHITECTURE.md`. Contratos definitivos/ADRs e limiares de matching ficam para a Sessão 3 autorizada.
2. Escolher a referência de partida: **HEAD auditado `0b9bb0a` (recomendado para aproveitar o código atual)** ou release `v0.20.0.1` com revalidação.
3. Aceitar como condição de distribuição o esclarecimento do uso perante AniList, sem considerar o tracker herdado prova de permissão.

Nome, ícone, conta GitHub, applicationId, assinatura e publicação ficam pendentes da Sessão 1 ou posteriores; nenhuma escolha foi imposta. **Próximo passo possível: Sessão 1, somente após autorização explícita.**
