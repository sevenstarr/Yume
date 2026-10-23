# Demonstração técnica e portfólio

22/10/2026, sessão 12. O objetivo é permitir avaliar a contribuição pelo código e por resultados observáveis. É um fork experimental, sem release nem garantia de aceite Android integral. O mantenedor dirige o projeto e decide aprovações; Codex assistiu análise, código, testes e documentação. Player, fontes e biblioteca são trabalho herdado, não criações novas atribuídas ao mantenedor ou à IA.

## Problema e escolhas técnicas

Fontes podem fornecer capas limitadas e títulos ambíguos. O incremento consulta um catálogo público separado e conserva a identidade/original da fonte. A troca visual exige uma decisão aceita; candidatos por título não se tornam vínculos automaticamente.

| Escolha | Benefício e custo | Evidência para avaliar |
| --- | --- | --- |
| Opt-in desligado e cliente público isolado | Usuário decide o tráfego; não reutiliza login ou headers de fontes | [Provider](../domain/src/main/java/tachiyomi/domain/metadata/service/MetadataProvider.kt), [privacidade](PRIVACY.md) |
| Revisão/pin e automação textual off | Evita temporada/remake incorreto; exige ação humana | [Matcher](../domain/src/main/java/tachiyomi/domain/metadata/service/AnimeMatcher.kt), [ADR 0004](DECISIONS/0004-conservative-anime-matcher.md) |
| Decisão separada de cache, revisão/epoch/CAS | Limpeza e resposta atrasada não apagam escolha válida; maior complexidade transacional | [Repository](../domain/src/main/java/tachiyomi/domain/metadata/service/MetadataRepository.kt), [ADR 0005](DECISIONS/0005-metadata-persistence.md) |
| Resolver local, Coil e transporte neutro | Sem pesquisa por tile; fallback e isolamento; precisa testar decoder/UI reais | [Capas](metadata/cover-resolution.md), [ADR 0006](DECISIONS/0006-cover-resolution.md) |
| Fila SQL e governor compartilhado | Retomada/quota entre consumidores; sincronização deliberadamente lenta | [Background](metadata/background-sync.md), [ADR 0008](DECISIONS/0008-durable-metadata-sync.md) |

## Demonstração sem consultar o serviço real

Prepare o [ambiente de desenvolvimento](DEVELOPMENT.md). Execute da raiz, PowerShell; em Unix use `./gradlew`:

```powershell
./gradlew.bat :domain:testDebugUnitTest --tests 'tachiyomi.domain.metadata.*' -Pdist=ci --console=plain --max-workers=2
./gradlew.bat :data:testDebugUnitTest --tests 'tachiyomi.data.metadata.*' -Pdist=ci --console=plain --max-workers=2
./gradlew.bat :app:testDebugUnitTest --tests 'eu.kanade.tachiyomi.data.coil.*' --tests 'eu.kanade.tachiyomi.data.metadata.anilist.*' --tests 'eu.kanade.tachiyomi.data.backup.MetadataBackupTest' -Pdist=ci --console=plain --max-workers=2
python scripts/verify-metadata-migration.py
python scripts/verify-metadata-sync-migration.py
```

| Caso | Resultado esperado e onde conferir |
| --- | --- |
| Títulos/temporadas/remakes e entradas hostis | Fixtures e parser recusam associações/IDs inválidos; [testes domain](../domain/src/test/java/tachiyomi/domain/metadata) |
| Pin, pausa, cancelamento e 2.000 itens | Fatias limitadas, ID manual exato, originais preservados e geração antiga recusada; [MetadataSyncTest](../data/src/test/java/tachiyomi/data/metadata/MetadataSyncTest.kt) |
| Fila após fechar/reabrir SQLite | Tentativas e estado sobrevivem; teste de reabertura no mesmo arquivo acima |
| 404/decode/offline/manual/original | Tentativas terminam em fallback elegível sem apagar arquivo manual; [interceptor](../app/src/test/java/eu/kanade/tachiyomi/data/coil/MetadataCoverInterceptorTest.kt) |
| Transporte e limite AniList | Respostas mockadas, cancelamento e governor com relógio injetado; [testes AniList](../app/src/test/java/eu/kanade/tachiyomi/data/metadata/anilist) |
| Migração e backup opcional | FKs/defaults/rollback e wire antigo/novo preservados; [backup](../app/src/test/java/eu/kanade/tachiyomi/data/backup/MetadataBackupTest.kt) |

Os testes usam SQLite JDBC, fakes/mocks e respostas sintéticas. Não demonstram bateria Android, precisão de catálogo real ou decoder físico. Dependências do build podem exigir rede, sem consultar AniList para os cenários de teste. Confira JUnit XMLs e exit codes, não apenas a presença de um APK. [Resultados históricos e cobertura por cenário](TEST_REPORT.md).

## Roteiro manual em instalação sintética

Este é um roteiro reproduzível para validação futura, não um resultado já aprovado nesta sessão. Use dados sem informação pessoal e material visual cujo uso seja permitido.

1. Compile/instale debug, anote SHA/certificado/API e conclua onboarding em pasta de teste. Verifique opt-ins desligados na instalação limpa.
2. Com um anime sintético persistido, abra metadados externos. Ao ativar e pesquisar, título/ID serão enviados ao AniList. Verifique que pesquisa sozinha não altera o vínculo.
3. Revise um candidato e confirme explicitamente. Confira ID/origem e capa; cancele outra proposta e confirme que o pin anterior permanece.
4. Alterne **Usar original** e modo normal com detalhes/biblioteca abertos. Teste opt-out e fallback offline com/sem cache; compare biblioteca, episódios e progresso.
5. Inicie lote, pause/retome/cancele e confira contadores e preservação do ID manual. Um lote concluído só com itens ignorados comprova o caminho de skip, não novas consultas.
6. Restore/limpeza completa, reprodução/download offline, TalkBack e bateria exigem o [roteiro de qualidade](TEST_REPORT.md), backups sintéticos e comparação antes/depois. Não execute limpeza em uma biblioteca pessoal para demonstração.

Registre resultado esperado/observado, SHA, variante, pré-condições e evidência sanitizada por caso. Não prometa resultado idêntico para títulos reais ambíguos nem provoque carga no serviço para testar quota.

## Material visual e limites de apresentação

As [capturas reais da sessão 9](screenshots/README.md) mostram configurações/estados de lote, não uma versão atual de todas as telas. Não incorporam capas de obras, biblioteca, contas ou notificações pessoais. Capturas atuais de seleção, fallback e identidade Yume ainda precisam ser produzidas em ambiente sintético; não foram inventadas ou substituídas por mockups.

Apresente separadamente: funcionalidades herdadas, incremento Yume, testes automatizados executados, observações Android parciais e gates ainda abertos. [Arquitetura](ARCHITECTURE.md), [origem](UPSTREAM.md), [progresso](PROGRESS.md) e [changelog](../CHANGELOG.md) permitem conferir essa separação. A sessão documental não autoriza beta, release, promoção para principal ou afirmação de revisão humana integral.
