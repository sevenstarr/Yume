# Evidências — Sessão 9

20/10/2026. Pedido explícito: branch temporária de `yume-develop`, sessão nove, testes, PR e merge. Repositório canônico em `Animiru/`; lidos o prompt mestre em `C:/Users/Administrator/Downloads/prompt.md`, progresso/arquitetura e contratos anteriores. A pasta externa Yume não é Git e não contém o prompt. Conferidos status limpo, últimos cinco commits e stat do HEAD; fetch autorizado confirmou base `090442f489db222fc064721b44f048502ab80a0b`, merge da PR #1. Criada `feat/session-9-metadata-sync` sobre `origin/yume-develop`, sem descartar alterações.

## Ambiente e execução

JDK 21.0.11, Gradle 9.7.1, SDK/caches do baseline em `.tooling`, dois workers. Variáveis somente por processo: `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME`, `ANDROID_USER_HOME`. Logs locais em `build/session9/`, ignorados pelo Git. Não houve tráfego AniList/CDN real, OAuth, mutação de tracking ou publicação de release.

1. O primeiro fetch restrito falhou por rede; repetição com acesso autorizado ao GitHub passou. As referências atualizadas e a PR #1 confirmaram a base integrada. Nenhum force-push/rebase ou alteração em `yume-release`/upstream.
2. `compile.log`: tentativa restrita registrou acesso negado ao marcador do daemon Kotlin e fallback; SQLDelight recusou referências OLD/NEW de um trigger proposto. A implementação final usa um hook mínimo na transação de favoritos, com queries geradas/notificações observáveis, sem trigger. `compile2.log`: corrigidos tipos de agregação SQL (SUM inferido como Double) com CAST INTEGER e retorno Unit do adaptador. Nenhuma supressão de detector.
3. `checks1.log`/`checks2.log`: Spotless exigiu quebra de linha no construtor de identidade. Corrigida a formatação. `checks3.log` passou em 5min1s; `checks4.log` passou em 6min58s, incluindo todos os testes JVM, build, migrações e lint data/widgets. Esses resultados precedem refinamentos finais de recuperação, quota e agendamento; não constituem isoladamente prova do diff final.
4. `final-format.log` passou; `final-checks.log` passou em **191,927 s**, com todos os testes JVM, build, migrações, Spotless e lint data/widgets. A revisão posterior distinguiu continuação de fatias bem-sucedidas do retry exponencial por erro operacional, evitando atrasos crescentes em bibliotecas grandes; completou cancelamento, readição de favoritos, prioridade global, circuito e relógio monotônico. `approved-checks.log` terminou com testes/build, mas falhou no detector AndroidX do lint integral (`Unexpected owner function: null` em `AniListHostGovernor.kt`). Sem supressões ou mudança de dependências.
5. As tentativas intermediárias `quota-checks.log`/`review-checks.log` não são evidência final: durante a revisão houve mudança de fontes depois da formatação/compilação inicial, causando Spotless divergente e um artefato de teste sem o novo parâmetro de relógio. A tentativa `complete-format.log` exigiu uma quebra de linha adicional. As fontes estabilizadas são verificadas por `complete-format2.log`, `complete-checks.log`, `complete-result.json` e, separadamente, `complete-app-lint.log`/`complete-app-lint-result.json`, resumidos no [snapshot](SESSION_9.json).
6. Executados `scripts/verify-metadata-migration.py` e `scripts/verify-metadata-sync-migration.py`: preservação de linhas/schema de 11 tabelas herdadas na migração 148 e 18 tabelas existentes na migração 149, defaults, FKs e rollback em cópias sintéticas. Snapshot 149 derivado do 148 versionado + migração 148, sem dados pessoais.
7. O primeiro `adb devices -l` restrito não conseguiu criar o diretório Android padrão; repetição com `ANDROID_USER_HOME` do baseline e acesso ao host passou, lista vazia. Nenhuma instalação, UI/TalkBack, Doze, reboot Android, queda real de conectividade, benchmark ou medição de bateria foi realizada.
8. **Verificação final estabilizada:** `complete-format2.log` passou; `complete-checks.log`/`complete-result.json` passaram em **146,021 s**, com **298 testes/20 suítes**, zero falhas/erros/ignorados, build/migrações/Spotless/lint data/widgets. Lint integral repetido separadamente concluiu em **356,490 s**, exit 0, com relatório atual: 3 erros herdados `NonObservableLocale` (`Calendar.kt:74/95`, `CalendarHeader.kt:95`), 205 warnings; nenhum erro nas fontes alteradas. Três novos `UseKtx` no governor são avisos de estilo: mantido `Editor.commit()` explícito para conferir o Boolean e impedir emissão se persistir falhar. Nenhuma supressão. Esse resultado final supera a inconclusão da tentativa anterior, sem declarar lint limpo.
9. Cinco APKs debug locais; `apksigner verify --verbose` do universal passou com assinatura v2. Na revisão staged, removida somente uma linha vazia final/CRLF da migração 149, sem mudança de DDL; comparação de conteúdo SQL normalizado e script de upgrade/rollback passaram novamente. Os hashes finais estão no snapshot. Nenhuma alteração funcional após os testes estabilizados.
10. `verifySqlDelightMigration spotlessCheck` após normalização passou novamente (`post-format.log`, 51 s no Gradle). Na revisão final, identificado commit documental paralelo `a4bb6431c` e renomeação para `feature/session-09-metadata-sync`, ambos preservados. Novo `AGENTS.md` lido; sincronização por fetch/merge antes da PR, sem commits diretos em integração/principal. O commit próprio contém somente as 42 alterações da sessão; o commit documental anterior continua separado na ancestralidade.

## Reprodução

Com o ambiente descrito no [baseline](../BUILD_BASELINE.md):

```powershell
./gradlew.bat testDebugUnitTest :app:assembleDebug verifySqlDelightMigration spotlessCheck :data:lintDebug :presentation-widget:lintDebug :app:lintDebug --console=plain --stacktrace --max-workers=2
```

Os dois scripts Python usam somente a biblioteca padrão e SQLite. Lint deve ser avaliado pelos relatórios, pois `abortOnError=false` é herdado. O snapshot registra o resultado do lint integral separadamente dos testes; exit zero não significa relatório sem achados. Nenhum detector foi desativado ou adicionado ao baseline.

## Provas e limites

- Provider fake e SQLite real: lote de 2.000 animes até conclusão em fatias, progresso, TTL/cache removido, original-only, pin/ID exato, originais/personalizados, retry finito/due/cooldown, pausa/cancelamento, reinício JDBC, leases abandonadas e limite durável de três tentativas, rejeição de respostas tardias dentro da transação e exigência de carga persistida.
- Relógio/store injetáveis: janela de 12/min, espaçamento, limite menor durável, cooldown/Retry-After em data/segundos, remaining zero, rollback do relógio, cancelamento e falha fechada de persistência. Observação de rate limit GraphQL do provider propaga ao governor compartilhado.
- WorkManager e restrições de Android são integrados/compilados, mas não exercitados em dispositivo. Não há comprovação de economia de bateria, comportamento Doze/reboot ou regressão perceptível. Contagem de cobertura de linhas/branches não medida. Automação textual permanece desativada.

A publicação da PR e o merge em `yume-develop` foram autorizados no pedido desta sessão. O resultado remoto é informado na entrega final; a sessão 10 não está autorizada por esse pedido.
