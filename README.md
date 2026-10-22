<!-- Modified for Yume on 2026-10-20: fork identity, session status and attribution; assisted by Codex. -->
# Yume

Derivação independente do [Animiru](https://github.com/animiruorg/Animiru), por sua vez derivado do [Aniyomi](https://github.com/aniyomiorg/aniyomi). O nome **Yume** foi confirmado pelo proprietário em 19/10/2026. O [fork público sevenstarr/Yume](https://github.com/sevenstarr/Yume) contém as sessões 1–9 integradas em `yume-develop`; `yume-main` preserva a base estável anterior.

O objetivo é adicionar enriquecimento **opt-in**, inicialmente com metadados e capas do AniList, preservando biblioteca, extensões, episódios, downloads e progresso. As Sessões 4–9 implementam o [cliente público isolado](docs/metadata/anilist-client.md), o [matcher conservador](docs/metadata/anime-matcher.md), a [persistência/cache/backup](docs/metadata/persistence.md), a [resolução de capas](docs/metadata/cover-resolution.md), as [configurações/escolha manual](docs/metadata/manual-review.md) e a [sincronização em lote](docs/metadata/background-sync.md). Opt-in global, novos favoritos e agendamento diário começam desligados. O lote atualiza vínculos aceitos e pesquisa candidatos para revisão; associação automática textual permanece desativada. O tracking AniList já existente é herdado do Animiru.

## Estado atual

| Área | Situação |
| --- | --- |
| Base | `animiruorg/Animiru`, `animiru-new-main`, commit `0b9bb0a347aa7bcee226d8782d98d9abafc34b32` |
| Sessão 0 | Reconhecimento concluído; [auditoria](docs/UPSTREAM_AUDIT.md) |
| Sessão 1 | Fork público e identidade Yume aprovados; pacote próprio e marca ¥ preto/amarelo aplicados na rodada de [pendências sem aparelho](docs/NON_DEVICE_PENDING.md) |
| Sessão 2 | Build debug e 52 testes passaram; lint com 3 erros herdados; [baseline e limitações](docs/BUILD_BASELINE.md) |
| Sessão 3 | Arquitetura aprovada e [especificação funcional](docs/metadata/functional-specification.md) documentada |
| Sessão 4 | Cliente AniList com busca/ID, tratamento de falhas e testes sem rede; [escopo e limites](docs/metadata/anilist-client.md) |
| Sessão 5 | Normalização, matching puro, proteção de pins e parâmetros propostos com fixtures separadas; [contrato e gate](docs/metadata/anime-matcher.md) |
| Sessão 6 | Migração SQLDelight, decisões/cache separados, gravação condicional, backup opcional e preservação de personalizados; [contrato e limites](docs/metadata/persistence.md) |
| Sessão 7 | Resolver/Coil, fallback/cache e reversão à original integrados; testes JVM; aceite visual e performance em Android pendentes; [contrato e limites](docs/metadata/cover-resolution.md) |
| Sessão 8 | Configurações, busca por título/ID/link, seleção confirmada, refresh e remoção/reversão; [uso, testes e limites](docs/metadata/manual-review.md); validação UI/Android pendente |
| Sessão 9 | Fila SQL/WorkManager, gatilhos opcionais, pausa/retomada/cancelamento e quota AniList compartilhada; [contrato e limites](docs/metadata/background-sync.md); bateria/Doze/UI em Android pendentes |
| APK local | Cinco APKs debug gerados na retomada de 22/10; ARM64 com assinatura v2 verificada, instalado separadamente no Galaxy A05s e onboarding visível; validação Android completa pendente; nenhuma release |
| Funcionalidade nova | Configurações → Metadados externos; detalhes do anime → ⋮ → Metadados externos. Capas, correção manual e lote opt-in integrados; originais preservados e chamadas AniList próprias limitadas em conjunto |

Não há release Yume. Os APKs do upstream pertencem ao Animiru. O código atual usa nome/ícone Yume e pacote `io.github.sevenstarr.yume`, com `.dev` no debug; os APKs históricos de baseline usavam o pacote herdado. O updater está bloqueado até existir canal próprio aprovado. A identidade nova cria um aplicativo separado, sem migração automática de dados ou sessões. Consulte [identidade e coexistência](docs/IDENTITY.md), [privacidade](docs/PRIVACY.md) e [gates externos](docs/PROVIDER_REVIEW.md) antes de instalar ou distribuir. O aceite Android e as dependências de distribuição continuam explícitos.

## Desenvolvimento

O upstream exige JDK **21**, Gradle **9.7.1** pelo wrapper e as versões Android declaradas em `gradle/mihon.versions.toml`. A [sessão 2](docs/BUILD_BASELINE.md) registra ferramentas, comandos realmente executados, tempos, resultados e bloqueios. O [script PowerShell](scripts/run-baseline.ps1) repete as verificações com logs e dois workers por padrão; as tarefas principais são:

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat spotlessCheck
./gradlew.bat testDebugUnitTest
./gradlew.bat verifySqlDelightMigration
./gradlew.bat :app:lintDebug
```

O checkout da referência upstream permite estabelecer a linha de base sem as mudanças documentais. Veja [origem e reprodução](docs/UPSTREAM.md), [contribuição](CONTRIBUTING.md), [segurança](SECURITY.md) e [progresso](docs/PROGRESS.md).

## Autoria e licença

Biblioteca, extensões, reprodução, downloads, categorias, temas, backups e tracking são recursos herdados. O [changelog](CHANGELOG.md) distingue as mudanças Yume do histórico original. A documentação e o script de baseline foram produzidos com assistência do Codex; o histórico identifica os commits de automação, sem atribuí-los a uma pessoa como implementação própria. A revisão e as decisões de publicação cabem ao proprietário.

O texto [Apache-2.0](LICENSE) foi preservado. [NOTICE](NOTICE) registra a origem e as atribuições; o [levantamento de licença](docs/LICENSE_REVIEW.md) documenta obrigações e pendências de componentes e imagens. Yume não representa as equipes Animiru, Aniyomi, Mihon ou os provedores de metadados.

Avisos preservados do README upstream:

```text
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project
Copyright © 2024 Aniyomi Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

Os [README](docs/upstream/README.md) e [créditos de contribuição](docs/upstream/CONTRIBUTING.md) originais foram preservados como snapshots históricos. Seus canais e downloads referem-se ao upstream. O aplicativo não hospeda conteúdo e não tem afiliação com os provedores de conteúdo disponíveis.
