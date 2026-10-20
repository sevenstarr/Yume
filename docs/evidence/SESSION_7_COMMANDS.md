# Evidências — Sessão 7

20/10/2026. Pedido: executar sessão 7. Lidos prompt mestre em `Downloads/prompt.md`, progresso canônico e arquitetura/especificação/ADRs pertinentes; conferidos `git status`, `git log -5 --oneline`, `git show --stat HEAD` e as alterações anteriores. A pasta externa Yume não é Git e não contém prompt local. O proprietário autorizou explicitamente validar/finalizar a sessão 6 não commitada e depois executar a 7. Sessão 6 consolidada no commit `bc56246928dcedfb4c92a402ba7898ff3b3924b1`; árvore limpa antes de criar `feat/session-7-cover-resolution`.

## Comandos e tentativas

JDK 21.0.11, SDK/caches locais do baseline, Gradle 9.7.1, dois workers. Os logs completos permanecem em `build/session7/` ignorado. O script de baseline registra tempos por tarefa; a verificação final foi agrupada em uma chamada Gradle para evitar recompilações duplicadas por timestamp de BuildConfig.

1. `compile`: `:app:compileDebugKotlin` passou em aproximadamente 122 s, validando a integração inicial. Não é validação das alterações finais posteriores.
2. `first-tests`: compilação recusou múltiplas interfaces no binding implícito Metro do resolver. `second-tests`: tentativa de explicitar binding usou tipo incompatível com a API instalada. Corrigido conservando somente o contrato `CoverResolver`; o método interno de encerramento para testes não adiciona interface de DI.
3. `third-tests`: suíte passou em aproximadamente 67,6 s; 216 resultados (131 domain, 22 data, 63 app), sem falhas. Acrescentados depois testes de off em voo, deduplicação e falha de armazenamento.
4. `format`: Spotless aplicado somente aos módulos alterados (domain/data/app/widget), sem supressões. A sequência `validation` foi interrompida para incluir fallback após falha SQL/eviction e seu teste, antes de considerar os checks finais. Nenhum resultado parcial dessa tentativa é classificado como aprovação do diff final.
5. `final-format`: ktlint identificou linha longa no novo bloco de tratamento de falhas; quebrada sem suprimir regra. `format-approved`: formatação de app passou. Verificação conjunta final em `final/checks.log` e [SESSION_7.json](SESSION_7.json).
6. ADB restrito falhou por não conseguir criar diretório de usuário `\.android`; repetição com acesso ao host terminou exit 0 e lista de dispositivos vazia. Não houve instalação/emulação nem repetição das tentativas de boot que já falharam no baseline. Nenhuma rejeição de auto-review ocorreu.
7. Inspecionadas as fontes oficiais Coil 3.6.3 para [Interceptor](https://github.com/coil-kt/coil/blob/3.6.3/coil-core/src/commonMain/kotlin/coil3/intercept/Interceptor.kt), [RealInterceptorChain](https://github.com/coil-kt/coil/blob/3.6.3/coil-core/src/commonMain/kotlin/coil3/intercept/RealInterceptorChain.kt), [EngineInterceptor](https://github.com/coil-kt/coil/blob/3.6.3/coil-core/src/commonMain/kotlin/coil3/intercept/EngineInterceptor.kt), request/result e factory OkHttp. Nenhum probe AniList/CDN, OAuth, mutation ou download de imagens reais foi executado.
8. A chamada conjunta `final` passou em 263,574 s: 219 testes, build, migração e Spotless; data lint sem issues e widget lint com 0 erros/4 avisos `UnusedAttribute` nas linhas 10–13 do XML herdado `updates_grid_homescreen_widget_info.xml`, não alterado. Revisão posterior encontrou modo original indisponível antes da associação. Corrigida criação local com CAS/opt-in off, acrescentado teste e ampliado o caso de mudança de origem; estabilizado o token inicial Compose. Spotless exigiu parênteses explícitos na condição mista, depois uma linha em branco no contrato (`final-after-review`, 21,737 s, exit 1); corrigidos sem supressão. A repetição `final-approved` é a evidência canônica do diff final.
9. `final-approved` passou em 214,910 s, com 220 testes nos XMLs, build debug, migração, Spotless e lint de data/widgets. Revisão do visualizador acrescentou comparação de modelo/view para não reiniciar o mesmo carregamento ao recompor o menu; `viewer-validation` passou em 90,629 s com testes/build/Spotless nas fontes finais. DDL, domínio/data e widgets não mudaram nesse ajuste; seus checks anteriores permanecem válidos. Assinatura v2 do APK universal final verificada com `apksigner` do Build Tools 37.0.0.

## Reprodução da verificação conjunta

Com as variáveis do baseline (`JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME`, `ANDROID_USER_HOME`) configuradas por processo:

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug verifySqlDelightMigration spotlessCheck :data:lintDebug :presentation-widget:lintDebug --console=plain --stacktrace --max-workers=2
```

Artefatos/XMLs/lint/APKs ficam locais, sem publicar. Build usa working tree; tarefas atualizadas podem ser reutilizadas pelo Gradle. Tempos, contagens, hashes dos APKs e arquivos da sessão constam no snapshot. Não foi medida cobertura de linhas/branches.

Revisão do diff completo da sessão e staged, `git diff --check`/`git diff --cached --check`, 110 links locais em documentos alterados, unicidade de strings XML e hashes das fontes conferidos. Nenhum controle bidi nas fontes novas/alteradas verificadas. Índice Git contém somente os 34 arquivos listados no snapshot; artefatos, logs e bytes de teste permanecem ignorados.

## Alcance das provas

- Domínio: URLs/IDs/variantes, deduplicação, retenção e clock rollback, off/revisão/restauração/indisponível, modo original e chaves compartilhadas entre fontes.
- SQL real/JDBC: observação por lote, reversão de original-only/off, preservação de origem, writes concorrentes e clear que aguarda todos; publicação recusada após mudança de identidade da fonte. Sem nova migração/DDL nesta sessão; verificações 148→149 preservadas.
- Cadeia Coil: requests reais e resultado do engine representado por success/error sintéticos, cobrindo ambos os modelos atendidos pelas factories herdadas, manual corrompida preservada, 404/decode, offline com/sem cache, original falha, cache-only, contexto/target/size resolver, keys explícitas, off/cancelamento/revisão/clear e armazenamento indisponível. As factories reais são compiladas; não foi executado decoder Android.
- Cache físico: `DiskCache` real, bytes sintéticos, fechamento/reabertura e leitura offline sem novo transporte. Não é teste de decoder gráfico Android.
- HTTP: call factories/callbacks fake validam headers públicos, política neutra, falhas/redirects recusados, limite de bytes e cancelamento. O tratamento de follow-ups especiais consta no interceptor de rede; não foi disparado servidor/CDN real para medir emissões físicas.

## Limitações e ponto de parada

Sem dispositivo conectado, decoder/UI/Glance real, round-trip de APK, comparação online/offline visual, medições de scroll/memória/rede ou stress multithread. Nenhuma alegação de ausência perceptível de regressão. Aceite integral da sessão 7 continua pendente desses gates. Quarentena de imagem/cooldown em processo; sem revalidação durável/worker. Configuração/seleção remota e sincronização não iniciadas, opt-in off e matcher textual gated. Lint integral de app permanece inconclusivo desde a sessão 4; lint de data/widgets é evidência separada. Sem push, PR remoto, tag ou release.
