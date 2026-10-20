# Evidências — Sessão 5

20/10/2026. Base `74a670cc0eb27563a80f54fe0378ab95dc6e5ce9`, branch `feat/session-5-anime-matcher`, derivada do commit da sessão 4 para conservar dependências. Árvore inicial limpa. Trabalho autorizado pelo proprietário: ler commit anterior, executar sessão 5 e fazer commit local. Sem push/PR remoto/tag/release.

## Inspeção e escopo

Lidos commit da sessão 4, prompt mestre local (`Downloads/prompt.md`), contribuição, arquitetura/especificação, ADR, progresso e evidências de build. Inspecionados modelos/provider da sessão anterior, domínio Gradle/testes e script do baseline. Sem AGENTS.md aplicável encontrado nos diretórios de trabalho/ancestrais inspecionados. Não houve pesquisa externa, consulta ao catálogo, download de imagens ou alteração de dependências.

Implementados modelos de matching, normalização/fingerprint, matcher e proposta manual em `domain/.../metadata`. Quatro arquivos auxiliares/de testes e dois TSV versionados exercitam decisões determinísticas, rejeições, estabilidade de pins, validação manual, casos difíceis e calibração. Código/documentação novos assistidos pelo Codex, com marcadores de origem. Nenhum consumidor UI/worker ativado, nem escrita em dados originais, tracking, SQL ou backup.

## Reprodução

JDK 21.0.11, SDK/cache do baseline; wrapper Gradle 9.7.1 e dois workers. A formatação automática foi limitada ao domínio; diff revisado, sem mudança de código herdado.

```powershell
.\scripts\run-baseline.ps1 `
  -JavaHome 'C:\Program Files\Java\jdk-21.0.11' `
  -SdkRoot 'C:\Users\Administrator\Documents\Yume\.tooling\android-sdk' `
  -GradleUserHome 'C:\Users\Administrator\Documents\Yume\.tooling\gradle' `
  -Tasks ':domain:spotlessKotlinApply','testDebugUnitTest',':app:assembleDebug','spotlessCheck' `
  -OutputDirectory 'build/session5/reproduction'
```

Os logs completos ficam em `build/session5` (ignorados). O script registra o HEAD base antes do commit, mas verifica o working tree da entrega. BuildConfig.BUILD_TIME herdado pode provocar recompilação de app entre invocações. Gradle pode reutilizar tarefas já atualizadas; os XMLs de resultados são inspecionados separadamente.

## Tentativas e recuperação

1. `first`: formatação apontou linha longa em código novo, corrigida; `:domain:testDebugUnitTest` passou (55,068 s). Havia aviso de escrita de marcador do daemon Kotlin fora do workspace; a compilação/teste do domínio terminou. A sessão não foi considerada integralmente validada nessa tentativa.
2. `format`: formatação passou em 12,716 s; corrigidos/formatados somente arquivos Kotlin novos.
3. `final`: após revisão e casos adicionais, `:domain:spotlessKotlinApply` passou (11,061 s) e `spotlessCheck` passou (19,135 s). A suíte agregada falhou após 89,896 s e assemble após 83,941 s, em `:app:compileDebugJavaWithJavac` com `AccessDeniedException` ao fechar `presentation-widget/.../classes.jar`. Esses comandos **não** foram classificados como aprovados. O domínio havia gerado resultados atuais sem falhas, mas app não concluiu a tentativa.
4. `verified`: repetidos `testDebugUnitTest` e `:app:assembleDebug` com acesso aos caches locais fora das restrições anteriores. Resultados finais, tempos, XMLs e artefatos no [snapshot JSON](SESSION_5.json). Sem alterar código ou suprimir regras para contornar a falha de acesso.

| Verificação final | Resultado | Tempo externo |
| --- | --- | --- |
| `testDebugUnitTest` | Exit 0; 11 suítes, 164 testes, zero falhas/erros/ignorados nos XMLs | 84,732 s |
| `:app:assembleDebug` | Exit 0; cinco APKs locais | 74,224 s |
| `spotlessCheck` | Exit 0 | 19,135 s |
| Controles Unicode bidi | Nenhum nos oito arquivos Kotlin novos | Não cronometrado |
| Links Markdown locais e diff/staged whitespace | Destinos conferidos, sem erro de whitespace | Não cronometrado |

Novos testes: `AnimeMatcherTest` 59, `MetadataTitleNormalizerTest` 17 e `MetadataCalibrationTest` 1; os outros 87 resultados são das suítes anteriores. Tarefas do domínio já atualizadas foram reutilizadas na execução agregada final; seus XMLs atuais foram gerados nesta sessão, após os últimos ajustes funcionais. Sem testes ignorados. A compilação de app emitiu avisos herdados de APIs obsoletas/experimentais. Os APKs permanecem locais/ignorados, sem instalação ou distribuição; não foi repetida verificação externa de assinatura nesta sessão.

## Calibração e validação

`MetadataCalibrationTest` avalia 186 configurações e conserva 124 sem falso positivo automático no conjunto de calibração. Escolha usa apenas calibração, conforme critério determinístico documentado no [contrato](../metadata/anime-matcher.md). Configuração proposta: título 50, ano 15, formato 10, limiar alto 70, revisão 30, margem 10, suficiência v1. Fatores auxiliares e regras de suficiência são fixos no algoritmo v1; a grade não os aprende. `automationApproved=false` no código versionado, cópia true somente nas medições/testes.

| Conjunto | Casos | Positivos rotulados | Acertos automáticos | Falsos positivos automáticos | Cobertura automática | Abstenção | Cobertura dos positivos |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Calibração | 29 | 10 | 10 | 0 | 34,48% (10/29) | 65,52% (19/29) | 100% (10/10) |
| Validação separada | 36 | 10 | 10 | 0 | 27,78% (10/36) | 72,22% (26/36) | 100% (10/10) |

O [relatório bruto por caso](SESSION_5_MATCHING.txt) é copiado da saída real do teste `domain/build/reports/metadata/calibration-v1.txt`. SHA-256 dos TSVs no JSON usa texto UTF-8 com finais LF, independente da conversão CRLF de checkout. Os conjuntos não compartilham títulos locais ou IDs. São fixtures sintéticas desenvolvidas nesta implementação; não são uma amostra aleatória de AniList nem avaliação humana independente. Zero erro conhecido aqui não demonstra zero erro em produção. Gates/revisão/integração continuam pendentes e a automação permanece off.

## Limitações

Não foi medida cobertura de linhas/branches nem desempenho em dispositivo. Nenhuma instalação/regressão manual Android, migração/restore, Coil, persistência de pin ou concorrência SQL foi testada: esses componentes não foram modificados nesta sessão. Pins são propostas/regras puras; a Sessão 6 precisa gravá-los e proteger revisões atomicamente. Não houve repetição do lint integral, inconclusivo na sessão 4; nenhuma supressão ou alteração de baseline. Os erros/limitações herdados continuam registrados no baseline, sem declaração de lint limpo ou release pronta.
