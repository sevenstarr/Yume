# Build de referência — Sessão 2

Sessão autorizada em 19/10/2026. Base funcional upstream: `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`; HEAD inicial da sessão: `4d67e3a0e` (fundação documental). Branch: `chore/session-2-baseline`. As alterações anteriores ao baseline são documentação/templates e exclusões Git; código Android, recursos, dependências e workflows continuam herdados.

## Ambiente e reprodução

| Componente | Configuração observada |
|---|---|
| Sistema | Windows 10 amd64; ADB informa 10.0.19042 |
| JDK de execução | Oracle JDK 21.0.11+9-LTS-211, já instalado; `.github/.java-version` exige 21 |
| Compatibilidade Java do código | 17 em `gradle/mihon.versions.toml`; diferente do JDK que executa o Gradle |
| Gradle | 9.7.1, wrapper original; `--version` executado com sucesso |
| AGP / Kotlin | 9.4.1 / 2.4.20, conforme catálogo versionado |
| SDK | compile 37.2, target 36, mínimo 26 |
| Ferramentas instaladas | Command-line Tools 22.0, Build Tools 37.0.0 e 36.0.0 (esta última solicitada pelo AGP), Platform Tools 37.0.1, plataforma 37.2 |
| NDK declarado | 29.0.14206865; instalar se o grafo efetivo solicitar; não trocar a versão |
| Emulador | 37.2.12; imagem `system-images;android-36;default;x86_64`, revisão 2 |

Ferramentas e caches locais ficam em `C:\Users\Administrator\Documents\Yume\.tooling`, fora do repositório. Nenhuma alteração global de PATH/JAVA_HOME, instalação de driver ou configuração do Windows. O proprietário aceitou explicitamente os termos do SDK antes da instalação dos componentes.

As ferramentas vieram da [página oficial Android](https://developer.android.com/studio#command-tools). O SHA-256 do ZIP `commandlinetools-win-15859902_latest.zip` foi comparado com o publicado: `90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a`.

Exemplo de execução em PowerShell 7, após instalar as mesmas ferramentas e aceitar os termos:

```powershell
.\scripts\run-baseline.ps1 `
  -JavaHome 'C:\Program Files\Java\jdk-21.0.11' `
  -SdkRoot 'C:\Users\Administrator\Documents\Yume\.tooling\android-sdk' `
  -GradleUserHome 'C:\Users\Administrator\Documents\Yume\.tooling\gradle'
```

O script executa separadamente `:app:assembleDebug`, `testDebugUnitTest`, `spotlessCheck`, `verifySqlDelightMigration` e `:app:lintDebug`, sem alterar fontes ou corrigir formato automaticamente. Cada tarefa recebe log, SHA, horário UTC, duração e exit code em `build/baseline/<data>/results.json` (ignorado). Usa dois workers por padrão (`-MaxWorkers` permite ajustar), considerando os 8 GB de RAM deste host. As variáveis de ambiente são restauradas ao sair. Acesso de rede aos repositórios configurados é necessário para preencher o cache inicial.

Não usar `-Pdist=ci` como substituto do debug local: no código herdado isso muda Cast/updater. O debug local usa `xyz.Quickdev.Animiru.mi.dev`; ainda não é a identidade Android definitiva de Yume.

“Reproduzir o build” aqui significa repetir o procedimento e obter um APK compilado; não há garantia de identidade binária. O upstream gera `BuildConfig.BUILD_TIME` com o relógio atual nos tipos debug/preview (`app/build.gradle.kts` e `BuildInfo.kt`), o que pode invalidar compilação entre invocações. A chave debug também é local ao ambiente. Essa política herdada não foi alterada para acelerar ou tornar o baseline artificialmente determinístico.

## Testes herdados inventariados

| Arquivo | Métodos `@Test` encontrados |
|---|---:|
| `app/src/test/java/mihon/core/migration/MigratorTest.kt` | 6 |
| `domain/src/test/java/tachiyomi/domain/anime/interactor/FetchIntervalTest.kt` | 10 |
| `domain/src/test/java/tachiyomi/domain/release/interactor/GetApplicationReleaseTest.kt` | 3 |
| `domain/src/test/java/tachiyomi/domain/library/model/LibraryFlagsTest.kt` | 5 |
| `domain/src/test/java/tachiyomi/domain/episode/service/MissingEpisodesTest.kt` | 6 |
| `domain/src/test/java/tachiyomi/domain/episode/service/EpisodeRecognitionTest.kt` | 22 |

Total estático: 52 métodos em seis arquivos. Isso não é contagem de testes aprovados. O build configura JUnit Platform, JUnit Jupiter, Kotest e MockK. O módulo `baseline-profile` também contém geradores/benchmarks instrumentados com dispositivo gerenciado Pixel 6/API 34; não são uma suíte completa de regressão funcional.

## Resultados

`:app:assembleDebug --console=plain --stacktrace` passou: **25min54s no Gradle**, 1.556,010 s no cronômetro externo, 348 tarefas acionáveis (288 executadas e 60 de cache). Cinco APKs gerados: universal e splits arm64-v8a, armeabi-v7a, x86 e x86_64. A primeira tentativa de `--version` falhou por restrição de rede do sandbox; a repetição com rede autorizada passou em 20,063 s, sem trocar a distribuição fixada.

Metadados do APK: `xyz.Quickdev.Animiru.mi.dev`, versionCode 153, versionName `0.20.0.1-8244`, mínimo API 26, target API 36. `apksigner verify --verbose --print-certs` aprovou os APKs x86_64 e universal com assinatura v2 e certificado `Android Debug`; fingerprint público SHA-256 `44999debb4cc53d32b32368e029aee42396b1e77a4358eb0173227c223eb5cc5`. O primeiro build gerou a chave debug padrão em `%USERPROFILE%\.android\debug.keystore`, fora do Git; nenhuma chave de release foi usada. O manifesto x86_64 também foi inspecionado com `aapt2 dump badging`.

| APK local em `app/build/outputs/apk/debug/` | Bytes | SHA-256 |
|---|---:|---|
| `app-universal-debug.apk` | 316947711 | `75ece175af9e9c438753bb2ffb53fbcd7110f4a2f04d7cfe0de7ec3a52515c13` |
| `app-x86_64-debug.apk` | 125544725 | `236095911608dcaf62ebbde45e3190625438ce2288dbb766d2b30c7be06ea9b6` |

Avisos herdados: placeholders não posicionais nas traduções, APIs obsoletas/experimentais e `libsqliteJni.so`/`libzstd-kmp.so` empacotadas sem stripping. Não foram corrigidos na sessão de baseline.

| Verificação | Resultado | Tempo externo |
|---|---|---:|
| `testDebugUnitTest --max-workers=2` | Exit 0; 52 testes, seis suítes, zero falhas/erros/ignorados nos XML de app/domain | 219,373 s |
| `spotlessCheck --max-workers=2` | Exit 0; 120 tarefas acionáveis, 116 executadas | 91,225 s |
| `verifySqlDelightMigration --max-workers=2` | Exit 0; tarefas debug/release executadas, com alcance limitado abaixo | 12,005 s |
| `:app:lintDebug --max-workers=2` | Exit 0, mas relatório com 3 erros, 201 warnings e 2 hints; não é aprovação de lint limpo | 769,230 s |

Não foi configurado instrumento de cobertura: não há percentual de linhas/branches medido. Os outros módulos retornaram testes `NO-SOURCE`, que não foram contados como testes aprovados.

O lint concluiu em 12min48s no Gradle, com relatórios HTML/XML/SARIF/TXT em `app/build/reports/lint-results-debug.*`. Os três erros herdados são `NonObservableLocale`: `Calendar.kt:74`, `Calendar.kt:95` e `CalendarHeader.kt:95`, em `app/src/main/java/mihon/feature/upcoming/components/calendar/`. O exit 0 decorre da configuração permissiva herdada (`abortOnError=false`). Avisos incluem recursos privados/não usados, possíveis referências estáticas a contexto, alinhamento de bibliotecas nativas e dependências. Esses achados não foram corrigidos nesta sessão. A análise percorreu arquivos do projeto; o daemon Kotlin ocioso do cache desta sessão foi encerrado para liberar memória, sem interromper o lint.

**Limite da migração:** nenhuma fixture `*.db` foi encontrada em `data/src/main/sqldelight` ou `data/build`. A [documentação SQLDelight 2.4.0](https://sqldelight.github.io/sqldelight/2.4.0/android_sqlite/migrations/#verifying-migrations) descreve a aplicação das migrações aos snapshots `.db` encontrados. Portanto, o exit 0 e as duas tarefas executadas não demonstram evolução de uma base histórica com dados. Não foi criado um snapshot novo e apresentado como se fosse uma base antiga. Esse teste efetivo continua pendente para a sessão de persistência; backup/restauração também não foi validado.

### Dispositivo de teste

AVD isolado `yume_session2_api36` criado e reconhecido pelo `avdmanager list avd`: Pixel 6, API 36, x86_64, imagem default. `avdmanager` também emitiu aviso de `devices.xml` ausente na imagem; a criação foi confirmada pela listagem e pelos arquivos do AVD, sem tomar somente exit code como prova.

Host com 8.464.355.328 bytes de RAM, 8 processadores lógicos, virtualização de firmware habilitada e `HypervisorPresent=false`. `emulator -accel-check` terminou com exit 6: `Android Emulator hypervisor driver is not installed on this machine`. Nenhum driver/hipervisor foi instalado.

Tentativa de boot de software com `-accel off -gpu swiftshader -feature -Vulkan -no-window -no-metrics`: ADB mostrou `emulator-5580 offline`; `shell getprop sys.boot_completed` não pôde executar. Essa tentativa foi interrompida explicitamente após 147,523 s (exit -1) para liberar RAM durante o build. Isso não prova que software jamais funcionará: **boot, instalação e execução do app ainda não foram confirmados**. Não havia dispositivo físico conectado na listagem ADB inicial.

Após todas as verificações, `gradlew --stop` encerrou o daemon Gradle deste cache e uma nova tentativa com as mesmas opções e `-show-kernel` terminou espontaneamente em **2,885 s, exit -1073741819 (`0xC0000005`)**. Portanto, o caminho de software também não forneceu um Android utilizável neste host. O código de saída não identifica sozinho a causa raiz; não foi atribuído ao APK, que nem chegou a ser instalado. Nenhum processo de emulador desta sessão foi deixado rodando.

**Estado final:** build e testes de referência registrados; instalação e regressão em Android pendentes de dispositivo de teste utilizável ou correção do ambiente de emulação. O checklist foi criado, mas seus casos não foram aprovados em execução. Lint e migração histórica mantêm as limitações acima. A sessão 3 não foi iniciada.

O [checklist manual](REGRESSION_CHECKLIST.md) cobre instalação, biblioteca, adição, detalhes, busca, extensões, player, download, atualização, progresso, backup/restauração, cache, widgets e identificação de atualização.

## Riscos herdados

- `BackupAnime.getAnimeImpl()` continua sem copiar os campos `custom*` serializados pelo creator; é evidência estática, ainda sem reprodução de round-trip. Nenhuma correção funcional foi feita na sessão 2.
- Pacote, nome visual e callbacks ainda são herdados. Não instalar sobre dados pessoais nem usar assinatura de release.
- `lint.abortOnError=false` no upstream: mesmo uma execução verde de lint exige leitura do relatório de achados.
- A tarefa SQLDelight herdada precisa demonstrar execução efetiva e fixtures disponíveis; sucesso sem ações não prova migração de dados.
