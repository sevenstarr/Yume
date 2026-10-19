<!-- Modified for Yume on 2026-10-19: fork identity, status and attribution. -->
# Yume

Derivação independente do [Animiru](https://github.com/animiruorg/Animiru), por sua vez derivado do [Aniyomi](https://github.com/aniyomiorg/aniyomi). O nome **Yume** foi confirmado pelo proprietário em 19/10/2026. A fundação está sendo preparada localmente; o fork público ainda não foi criado.

O objetivo é adicionar enriquecimento **opt-in**, inicialmente com metadados e capas do AniList, preservando biblioteca, extensões, episódios, downloads e progresso. Esse recurso está planejado e **ainda não foi implementado**. O tracking AniList já existente é herdado do Animiru.

## Estado atual

| Área | Situação |
| --- | --- |
| Base | `animiruorg/Animiru`, `animiru-new-main`, commit `0b9bb0a347aa7bcee226d8782d98d9abafc34b32` |
| Sessão 0 | Reconhecimento concluído; [auditoria](docs/UPSTREAM_AUDIT.md) |
| Sessão 1 | Fundação local; conta GitHub e identidade Android pendentes |
| APK Yume | Nenhum build, assinatura, teste de instalação ou release |
| Funcionalidade nova | Nenhuma; fontes, banco, player e trackers preservados |

Não há download do Yume. Os APKs do upstream pertencem ao Animiru. O código ainda usa o pacote, ícone, nome Android, links e referências de atualização herdados; consulte o [plano de identidade](docs/IDENTITY.md) antes de compilar para instalação ou distribuição.

## Desenvolvimento

O upstream exige JDK **21**, Gradle **9.7.1** pelo wrapper e as versões Android declaradas em `gradle/mihon.versions.toml`. A preparação do ambiente e o build de referência são o escopo da sessão 2; os comandos abaixo são instruções, **não evidência de execução**:

```powershell
./gradlew.bat assembleDebug -Penable-updater=false
./gradlew.bat spotlessCheck
./gradlew.bat testDebugUnitTest
./gradlew.bat verifySqlDelightMigration
```

O checkout da referência upstream permite estabelecer a linha de base sem as mudanças documentais. Veja [origem e reprodução](docs/UPSTREAM.md), [contribuição](CONTRIBUTING.md), [segurança](SECURITY.md) e [progresso](docs/PROGRESS.md).

## Autoria e licença

Biblioteca, extensões, reprodução, downloads, categorias, temas, backups e tracking são recursos herdados. O [changelog](CHANGELOG.md) distingue as mudanças Yume do histórico original. O trabalho documental desta fundação foi produzido com assistência do Codex; o histórico identifica os commits de automação, sem atribuí-los a uma pessoa como implementação própria. A revisão e as decisões de publicação cabem ao proprietário.

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
