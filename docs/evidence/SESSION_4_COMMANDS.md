# Evidências — Sessão 4

20/10/2026. Base `48b23e78dd1b07526603b15b31bed8fd43caaf62`, branch `feat/session-4-anilist-client`. Trabalho autorizado: executar a Sessão 4 e fazer commit local. Sem push, PR remoto, tag ou release.

## Inspeção e serviço

- Lidos o prompt mestre em `Downloads/prompt.md`, a especificação/arquitetura da Sessão 3, ADR 0002, progresso, baseline e guia de contribuição. Árvore inicial limpa; branch temática criada a partir do HEAD da especificação, conservando suas dependências (não da `main` antiga).
- Inspecionados `AnilistApi`, `NetworkHelper`, extensões OkHttp, DI Metro, catálogos Gradle e testes existentes. Nenhuma dependência adicionada.
- Fontes oficiais AniList e OkHttp consultadas por navegador de pesquisa. Páginas `docs.anilist.co` retornaram 403/inacessível; o conteúdo oficial foi lido em `AniList/docs`, não inferido desse erro de acesso. Links exatos em [anilist-client.md](../metadata/anilist-client.md).
- Primeira tentativa PowerShell de POST público foi impedida por acesso de socket do sandbox. Repetição com rede autorizada: `Invoke-WebRequest -Method Post -Uri https://graphql.anilist.co -ContentType application/json`, corpo com query pública `Media(id: 1, type: ANIME)` e apenas campos de catálogo. HTTP 200; `Date: Tue, 20 Oct 2026 09:40:20 GMT`, limite 30 e restante 29. Confirmados campos solicitados, três variantes de capa no host `s4.anilist.co` e uma URL de banner. Sem token, mutation ou download de imagem. Esse probe não faz parte dos testes.

## Reprodução Gradle

JDK 21.0.11, SDK/caches da Sessão 2; dois workers. Exemplo:

```powershell
.\scripts\run-baseline.ps1 `
  -JavaHome 'C:\Program Files\Java\jdk-21.0.11' `
  -SdkRoot 'C:\Users\Administrator\Documents\Yume\.tooling\android-sdk' `
  -GradleUserHome 'C:\Users\Administrator\Documents\Yume\.tooling\gradle' `
  -Tasks 'testDebugUnitTest',':app:assembleDebug',':app:lintDebug','spotlessCheck' `
  -OutputDirectory 'build/session4/reproduction'
```

Logs completos e cronômetros ficam em `build/session4` (ignorados pelo Git). O SHA registrado pelo script é o HEAD base antes do commit; os comandos testam o conteúdo do working tree. A mudança de `BuildConfig.BUILD_TIME` herdada pode recompilar o app entre invocações.

### Tentativas e correções

1. `:app:compileDebugKotlin`: passou, exit 0, 149,778 s. O daemon Kotlin tentou escrever marcadores fora do workspace e informou `AccessDeniedException`; o compilador conseguiu terminar essa tarefa.
2. `:app:spotlessKotlinApply` / `:domain:spotlessKotlinApply`: apontaram linhas longas em fontes novas; corrigidas. A primeira tentativa de `:app:testDebugUnitTest` falhou por `AccessDeniedException` ao fechar `presentation-widget/.../classes.jar`, antes de executar a nova suíte. Não foi classificada como falha de teste nem como aprovação.
3. Nova execução com acesso aos caches locais: domain formatado; app ainda apontou duas linhas longas, corrigidas em seguida. `testDebugUnitTest` passou em 133,312 s: oito suítes, 85 testes (52 herdados + 33 novos), zero falhas/erros/ignorados. `:app:assembleDebug` também passou em 124,685 s. Durante revisão posterior foram adicionados casos de cooldown longo e caminho/nó de erro; os resultados finais abaixo devem ser usados para o conteúdo entregue.
4. `:app:lintDebug`: execução encerrada após **1.215,865 s (20 min 16 s)**, exit 1 por encerramento do daemon desta execução, sem relatório novo concluído. Duas capturas `jcmd Thread.print`, separadas por cerca de 3 min, mostraram a mesma worker em `KotlinUFile.getAllCommentsInFile` / `BidirectionalTextDetector` / travessia PSI, consumindo CPU. Isso identifica onde a execução estava retida, não prova a causa do problema. O daemon Kotlin ocioso deste workspace foi identificado por PID/classpath, sua inatividade conferida e encerrado para liberar memória; depois, apenas o daemon Gradle da execução retida foi encerrado. Capturas locais em `build/session4/lint-threads*.txt`, não versionadas. Nenhuma regra de lint foi desativada, baseline alterado ou detector suprimido. O relatório XML ainda existente é de 19/10 e **não valida este diff**; seus três erros herdados não foram apresentados como resultado desta sessão. Lint atual permanece inconclusivo e requer nova investigação/execução.

### Resultados finais

| Comando/verificação | Resultado | Tempo externo |
| --- | --- | --- |
| `:app:spotlessKotlinApply` | Exit 0; somente código novo formatado | 3,565 s |
| `spotlessCheck` após formatação | Exit 0 | 5,369 s |
| `testDebugUnitTest` | Exit 0; 87 testes em oito suítes; zero falhas/erros/ignorados | 109,434 s |
| `:app:assembleDebug` | Exit 0; cinco APKs | 71,303 s |
| `spotlessCheck` após ajuste de legibilidade | Exit 0 | Ver snapshot JSON |
| `apksigner verify --verbose --print-certs .../app-universal-debug.apk` | Exit 0; assinatura v2, certificado Android Debug | Não cronometrado |
| Verificação de controles Unicode bidi | Zero controles U+202A–U+202E/U+2066–U+2069 nos nove Kotlin novos | Não cronometrado |
| `git diff --check` / `git diff --cached --check` | Sem erro de whitespace | Não cronometrado |

XMLs finais: `AniListProviderTest` 30 testes, `AniListTransportTest` 5; herdados 52. Não há fixture de rede obrigatória nem teste ignorado. A compilação repetida emitia avisos herdados de APIs obsoletas/experimentais. O ajuste final de legibilidade preservou comportamento e passou novamente no Spotless.

Universal: 318.115.502 bytes; SHA-256 `3258dec7bc8145465ba1aec52a0f673b120dfab31c430e51ba52e202843ddba8`. Certificado público SHA-256 `c776693edea7ab013b822a7f10b52206f3a829c3c5b83065535e973348cc9267`, diferente do certificado registrado na Sessão 2; não foi testada atualização direta entre esses APKs. Nenhuma configuração de assinatura versionada foi alterada e nenhuma chave foi incluída no Git. O arquivo e os quatro splits continuam locais/ignorados, sem distribuição ou instalação. Resultados estruturados em [SESSION_4.json](SESSION_4.json).

## Limitações

Fixtures são sintéticas; a suíte não usa rede externa. O transporte é verificado com Call.Factory/callbacks simulados, sem servidor TLS/CDN ou Android real. Cobertura de linhas/branches não foi medida. Sem instalação, teste manual, restore/migração, Coil, worker, benchmark ou comprovação do orçamento agregado entre clientes herdados. Persistência de cooldown/circuito/contadores e testes de reinício permanecem nas Sessões 6/9. As pendências de emulação, identidade, backup personalizado e distribuição do baseline seguem abertas.
