<!-- Modified for Yume on 2026-10-20: fork identity, session status and attribution; assisted by Codex. -->
# Yume

Derivação independente do [Animiru](https://github.com/animiruorg/Animiru), por sua vez derivado do [Aniyomi](https://github.com/aniyomiorg/aniyomi). O nome **Yume** foi confirmado pelo proprietário em 19/10/2026. O [fork público sevenstarr/Yume](https://github.com/sevenstarr/Yume) foi criado com autorização explícita; os commits da fundação e do baseline ainda estão locais, sem push ou PR.

O objetivo é adicionar enriquecimento **opt-in**, inicialmente com metadados e capas do AniList, preservando biblioteca, extensões, episódios, downloads e progresso. As Sessões 4/5 implementam o [cliente público isolado](docs/metadata/anilist-client.md) e o [matcher conservador](docs/metadata/anime-matcher.md), com calibração sintética e automação textual desativada. Persistência e exibição continuam futuras. O recurso permanece inativo e o tracking AniList já existente é herdado do Animiru.

## Estado atual

| Área | Situação |
| --- | --- |
| Base | `animiruorg/Animiru`, `animiru-new-main`, commit `0b9bb0a347aa7bcee226d8782d98d9abafc34b32` |
| Sessão 0 | Reconhecimento concluído; [auditoria](docs/UPSTREAM_AUDIT.md) |
| Sessão 1 | Fork público criado; fundação local; identidade Android e envio dos commits pendentes |
| Sessão 2 | Build debug e 52 testes passaram; lint com 3 erros herdados; [baseline e limitações](docs/BUILD_BASELINE.md) |
| Sessão 3 | Arquitetura aprovada e [especificação funcional](docs/metadata/functional-specification.md) documentada |
| Sessão 4 | Cliente AniList com busca/ID, tratamento de falhas e testes sem rede; [escopo e limites](docs/metadata/anilist-client.md) |
| Sessão 5 | Normalização, matching puro, proteção de pins e parâmetros propostos com fixtures separadas; [contrato e gate](docs/metadata/anime-matcher.md) |
| APK local | Cinco APKs debug gerados; assinatura universal/x86_64 verificada; instalação ainda não validada; nenhuma release |
| Funcionalidade nova | Cliente e matcher isolados, sem UI/worker ativo; fontes, banco, player e trackers preservados |

Não há distribuição pública de APK Yume. Os APKs do upstream pertencem ao Animiru. O código ainda usa o pacote, ícone, nome Android, links e referências de atualização herdados; consulte o [plano de identidade](docs/IDENTITY.md) antes de instalar ou distribuir. Os APKs locais de baseline usam assinatura debug e o pacote herdado com sufixo `.dev`.

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
