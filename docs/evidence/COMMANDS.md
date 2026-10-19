# Evidências e comandos — Sessão 0

Data: 19/10/2026. Shell: PowerShell. Workspace: `C:\Users\Administrator\Documents\Yume`. Diretório da cópia: `C:\Users\Administrator\Documents\Yume\Animiru`.

Este registro lista comandos relevantes realmente executados e o resultado observado. Não é um log integral byte a byte da sessão. A captura [SESSION_0.json](SESSION_0.json) contém metadados selecionados dos retornos. Comandos Gradle abaixo dos workflows foram apenas **lidos**, não executados.

## Leitura do pedido e descoberta local

```powershell
Get-Content -LiteralPath 'C:\Users\Administrator\Downloads\prompt.md' -Raw
Get-Location
Get-ChildItem -Force
rg --files -g AGENTS.md -g '!node_modules' -g '!.git' -g '!dist' -g '!build' -g '!vendor'
git --version
git status --short --branch
```

Resultado: prompt lido integralmente; workspace inicialmente vazio; `git version 2.53.0.windows.3`; status inicial informou que a pasta não era repositório Git. A busca por AGENTS não encontrou arquivos (exit 1 do rg por ausência de match). Também foram verificados `AGENTS.md` em `C:\`, `C:\Users`, perfil Administrator, Documents e Yume, sem encontrar instruções adicionais.

## Obtenção do upstream

```powershell
git clone --depth 1 --no-single-branch https://github.com/animiruorg/Animiru.git Animiru
```

Primeira tentativa sob rede restrita: falha de conexão à porta 443. O mesmo comando foi autorizado pelo mecanismo de aprovação de rede e repetido; terminou com exit 0 e checkout de 1.839 arquivos. Não houve rejeição de aprovação, envio de alterações ou criação de fork remoto.

Dentro de `Animiru`:

```powershell
git rev-parse HEAD
git log -1 --format=fuller
git branch -a
git tag --sort=-version:refname
git diff --shortstat v0.20.0.1 HEAD
git rev-parse --is-shallow-repository
git rev-list --count HEAD
git status --porcelain=v1
git diff --exit-code
git diff --cached --exit-code
```

Resultados: SHA `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`; commit de 16/10/2026; branch padrão `animiru-new-main`; sete branches remotas mais `origin/HEAD`; tags inspecionadas. Diff estável→HEAD: `720 files changed, 11312 insertions(+), 9004 deletions(-)`. Clone raso = true, contagem local HEAD = 1. Status vazio e ambos os diffs com exit 0.

Na verificação final executada a partir da pasta externa, `git -C` recusou a cópia por diferença de proprietário entre o clone autorizado e o usuário do sandbox (`dubious ownership`). Não foi alterada a configuração global do Git. Repetiu-se a verificação com confiança limitada ao caminho conhecido e à chamada:

```powershell
git -c safe.directory=C:/Users/Administrator/Documents/Yume/Animiru status --porcelain=v1
git -c safe.directory=C:/Users/Administrator/Documents/Yume/Animiru diff --exit-code
git -c safe.directory=C:/Users/Administrator/Documents/Yume/Animiru diff --cached --exit-code
git -c safe.directory=C:/Users/Administrator/Documents/Yume/Animiru rev-parse HEAD
```

As três verificações terminaram com exit 0, status vazio e o mesmo SHA. Esse erro de contexto foi resolvido e não alterou arquivos nem constituiu uma rejeição de aprovação.

## Inspeção estática do código

Foram usados `rg --files`, `rg -n` e leituras completas/trechos numerados com `Get-Content`. Grupos de arquivos efetivamente lidos:

- README, CONTRIBUTING, LICENSE completo, `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, wrapper e os três catálogos de versões.
- `app/build.gradle.kts`, `domain/build.gradle.kts`, `data/build.gradle.kts`, `core-metadata/build.gradle.kts`, `presentation-widget/build.gradle.kts`, `baseline-profile/build.gradle.kts`.
- Build logic `PluginAndroidBase.kt`, `configurations/Kotlin.kt`, `BuildConfig.kt`, `BuildInfo.kt`, `extensions/CompileSdkSpec.kt`, `extensions/Project.kt`; workflows build/release e `.github/.java-version`.
- Modelos `Anime`, `AnimeCover`, `SAnime`, `NetworkToLocalAnime`; tabela `animes.sq`, migração `147.sqm`, `DatabaseBindings`; buscas de mapper/repositório de anime e views/modelos de histórico/updates.
- Coil `AnimeImageFetcher`, `AnimeCoverKeyer`, `Utils`, `CoverCache`, registro em `App.newImageLoader`, `AnimeDetails` no core-metadata.
- `AnimeViewModel.updateAnimeInfo`, `AnimeImageViewModel`, `EditAnimeDialog`; componentes biblioteca/detalhes/histórico/updates, widget e notificações; buscas de URLs diretas no player/cast.
- `UpdateAnimeFromRemote`, `MetadataUpdateJob`, `Anilist`, `AnilistApi`, `AnilistInterceptor`, `ALAnime`, `ALSearchItem`, `TrackerManager`, `AniChartApi`, `AniSkipApi`, `NetworkHelper`, `RateLimitInterceptor`.
- `BackupAnime`, `AnimeBackupCreator`, `AnimeRestorer`, updater e trechos do manifesto.

Exemplos exatos de buscas executadas:

```powershell
rg -n -i 'tmdb|themoviedb|jikan' -g '*.kt' -g '*.kts' -g '*.toml'
rg --files -g '*Test.kt' -g '!**/build/**'
rg --files data/src/main/sqldelight | rg '\.db$'
Get-ChildItem -LiteralPath data/src/main/sqldelight -Recurse -File -Filter '*.db' | Select-Object FullName
rg -n 'scheme|host|authorities|allowBackup|uses-feature' app/src/main/AndroidManifest.xml
```

TMDB/Jikan e snapshots `.db`: nenhum resultado na árvore consultada. Listagem `*Test.kt` inclui também `PluginAndroidTest.kt`, que é build logic e não suíte de teste: a contagem do relatório exclui esse arquivo. Algumas buscas exploratórias atingiram nomes presumidos inexistentes (`AnimeScreenModel.kt`, `app/.../di`, caminhos antigos de NetworkHelper/build logic) e um glob Windows não expandido; os paths corretos foram localizados com `rg --files` e lidos em seguida. Esses erros de descoberta não foram apresentados como falhas de build.

## Leituras remotas realmente realizadas

Pelo conector GitHub, somente operações de leitura:

```text
get_repo: animiruorg/Animiru
GET https://api.github.com/repos/animiruorg/Animiru/branches?per_page=100
GET https://api.github.com/repos/animiruorg/Animiru/releases?per_page=5
GET https://api.github.com/repos/animiruorg/Animiru/actions/runs?per_page=5
GET https://api.github.com/repos/animiruorg/Animiru/actions/runs?head_sha=0b9bb0a347aa7bcee226d8782d98d9abafc34b32&per_page=10
GET https://api.github.com/repos/AniList/docs/git/trees/master?recursive=1
```

Consulta de tags pelo conector foi rejeitada por endpoint não permitido (`INVALID_ARGUMENT`); as tags foram obtidas pelo clone Git. Cinco runs recentes estavam completed/success, mas em outros SHAs. A consulta filtrada pelo SHA auditado retornou zero runs.

Leituras web: página do repositório, página Animiru no site Aniyomi e seu link antigo Quickdesh (redirecionado para animiruorg); documentação oficial AniList (busca e referência). Aberturas diretas de várias páginas `docs.anilist.co` deram 403; fallback para arquivos oficiais em `AniList/docs` no SHA `03281c0a4bbf0c7f2097e0c935cddaed1096aa65`:

```text
docs/guide/terms-of-use.md
docs/guide/rate-limiting.md
docs/reference/object/mediacoverimage.md
docs/guide/auth/index.md
docs/guide/graphql/queries/media.md
```

Para confirmar o mecanismo de fallback proposto, foram lidas `Interceptor.kt` e `RealInterceptorChain.kt` na tag oficial **3.6.3** de `coil-kt/coil`. A página de referência web mostrava 3.7.0-SNAPSHOT; ela não foi tomada como prova da versão usada pelo Animiru. Nenhuma nova dependência foi instalada.

## Consulta pública ao AniList

Executado via `Invoke-WebRequest`, POST JSON, sem token e sem dados pessoais do usuário:

```graphql
query {
  Media(id: 1, type: ANIME) {
    id
    title { romaji english native }
    synonyms
    format
    season
    seasonYear
    startDate { year }
    coverImage { extraLarge large medium }
    bannerImage
  }
}
```

Headers enviados: `Content-Type: application/json`, `Accept: application/json`. Timeout 30s. Resultado real:

```text
HTTP 200
Content-Type: application/json
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 29
Retry-After: ausente
X-RateLimit-Reset: ausente
Media.id=1; title=Cowboy Bebop; format=TV; season=SPRING; seasonYear=1998
```

Títulos alternativos, sinônimos, URLs de capa e banner também foram retornados. O JSON de evidências transcreve campos selecionados; não representa uma captura integral da resposta. Não foram exercitados busca, OAuth, mutations, paginação, 429, timeouts simulados ou fallback do app.

Um HEAD (timeout 20s) da URL `extraLarge` devolvida, `https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-GCsPm7waJ4kS.png`, retornou HTTP 200, `Content-Type: image/png`, `Content-Length: 489507`. Não foi salvo/decodificado binário e não se mediram dimensões. As duas chamadas tiveram aprovação automática de acesso à rede.

## Ambiente local e verificações não executadas

```powershell
java -version
```

Retorno: Java Oracle 25.0.4 LTS. `Get-Command` localizou java/javac; não localizou adb/sdkmanager/emulator/gh. Flags de presença das variáveis `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`: false. Pasta SDK padrão e `local.properties`: false. Nenhuma enumeração de segredos do ambiente foi feita.

**Não executados:** Gradle Wrapper, configuração/resolução Gradle, build debug/release, spotless/lint, testes unitários, migrations em banco real, instrumentação, emulador/dispositivo, instalação/reprodução/downloads e restauração de backup. O inventário estático e o probe HTTP não substituem esses testes. O build de referência permanece na Sessão 2, após a autorização e preparação previstas.

## Verificação da entrega documental

Um script PowerShell leu os relatórios, extraiu caminhos exatos entre crases e conferiu sua presença com `Test-Path`: **54 caminhos**, sendo **36 existentes e 18 arquivos novos expressamente propostos e ainda ausentes**, sem caminho inesperadamente ausente. Não é uma contagem de todos os arquivos lidos ou de todas as referências abreviadas. `SESSION_0.json` foi validado por `ConvertFrom-Json`; o SHA correspondeu à cópia auditada. Sete arquivos documentais foram produzidos, todos fora de `Animiru/`.
