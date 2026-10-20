# Evidências — Sessão 8

20/10/2026. Pedido: executar sessão 8. Lidos prompt mestre em `C:/Users/Administrator/Downloads/prompt.md`, progresso/arquitetura canônicos, especificação funcional, contratos cliente/matcher/persistência/capas e pontos reais do código. A pasta externa Yume não é Git e não contém `prompt.md`; o repositório canônico é `Animiru/`, como já documentado. Conferidos `git status`, `git log -5 --oneline` e `git show --stat HEAD`. Base limpa `d8a60d7c9b25daf6166c13da902454a0786d6ef9`; criada branch `feat/session-8-metadata-controls`. Nenhuma alteração anterior descartada.

## Comandos e tentativas

JDK 21.0.11, SDK/caches locais do baseline, Gradle 9.7.1 e dois workers. Logs completos locais em `build/session8/` ignorado. Não houve chamada AniList/CDN, download de imagens reais, OAuth ou mutation; testes usam fixtures/transporte/provider fake.

1. `compile.log`: `:app:compileDebugKotlin` inicial falhou por dois recursos de strings ausentes no novo painel. Durante a execução restrita, Kotlin também registrou `AccessDeniedException` ao criar marcadores do daemon em AppData e usou fallback. Adicionadas as duas strings, sem contorno de implementação/permissão.
2. `format.log` até `format5.log`: Spotless exigiu quebra de linhas de 120 caracteres em condições/chamadas da tela nova; ajustes sem supressão. As posições reportadas eram relativas ao resultado formatado. `format6.log` passou, seguido da primeira verificação conjunta em `checks.log`.
3. Primeira verificação conjunta passou em **367,524 s**, com 260 testes nos XMLs e build/migração/Spotless/lint data/widgets. Esse resultado precede os refinamentos finais: semântica acionável do interruptor, rótulos localizados de formato, separação de modo/vínculo, padding do teclado, cancelamento ao sair da composição e três testes adicionais de recuperação/identidade. Não usar como prova isolada do diff final.
4. Após revisão, normalizado o estado de uma lease abandonada antes de revisão explícita, preservando a política de não renovar orçamento automaticamente. `final-format.log` passou; `final-checks.log` passou em **278,626 s**, com 263 testes. A revisão seguinte acrescentou à proposta o stamp da origem, recusando confirmação se somente temporada/fetch type mudar enquanto o diálogo está aberto; teste SQL específico incluído. `approved-format.log` passou e `approved-checks.log` passou em **202,762 s**, com **264 testes**, build, migração, Spotless e lint data/widgets.
5. O ViewModel também passou a invalidar candidatos/propostas por epoch/off, impedindo reapresentação de resultados ou download de miniaturas antigas ao voltar das configurações depois de limpeza. Publicação local verifica epoch e opt-in. Esse último ajuste do app recebeu `ui-format.log` e verificação adicional `:app:testDebugUnitTest :app:assembleDebug spotlessCheck`, aprovada em **146,794 s** nas fontes finais, em `ui-checks.log`/`ui-result.json`. Os checks anteriores de domínio/data/migração/lint não foram ampliados novamente porque suas fontes não mudaram. Resultado estruturado, XMLs, hashes e arquivos no [snapshot](SESSION_8.json). Cinco APKs debug locais; `apksigner verify --verbose` confirmou assinatura v2 do universal final, exit 0.
6. Repetições com acesso aos caches locais do Kotlin/Gradle foram aprovadas automaticamente. Não houve rejeição de auto-review nem mudança global de ambiente. Variáveis do baseline configuradas somente no processo; nenhum APK, keystore ou cache entrou no Git.
7. `adb devices -l` com acesso ao host terminou exit 0, lista vazia. Sem instalação, emulador, UI, TalkBack, foco/teclado em Android, decoder real, screenshots ou benchmark. Não repetidas as tentativas de emulação de software que já falharam no baseline.

## Reprodução

Com `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME` e `ANDROID_USER_HOME` apontando para as ferramentas/caches do baseline, executar no repositório:

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug verifySqlDelightMigration spotlessCheck :data:lintDebug :presentation-widget:lintDebug --console=plain --stacktrace --max-workers=2
```

O Gradle pode reutilizar tarefas atualizadas. A contagem final vem dos XMLs das suítes, não só da saída zero. Lint data sem issues; widgets com quatro avisos `UnusedAttribute` no XML herdado. Lint integral do app não foi repetido nesta sessão: continua inconclusivo desde a sessão 4, sem supressões novas. Cobertura de linhas/branches não medida.

## Alcance dos testes

- Parser: ID positivo canônico e links oficiais com/sem slug/barra; limites de inteiro, Unicode numérico não canônico, hosts enganosos, esquemas/portas, credenciais, parâmetros, fragmentos, caminhos/codificação/traversal recusados.
- Cliente/matcher: resposta real do mapper AniList no namespace `anilist` aceita pelo validador de pin, além da suíte herdada de transporte/contrato.
- SQL/JDBC e serviço real: recurso off sem emissão, ativação sem varredura, pesquisa sem trocar pin, correção após remoção/original-only, consentimento/revisão/epoch/TTL de proposta, refresh do ID exato, indisponibilidade preservada ao descartar proposta, rejeição e override manual explícito, parciais sem identidade, cooldown e cancelamento de transporte ao desligar. Snapshot da linha original/customizada comparado antes/depois da correção.
- Recuperação: lease esgotada ativa impede nova revisão; após expirar, apenas ação explícita abre novo orçamento, sem reviver ticket antigo nem restaurar estado buscando sem lease. Mudança da fonte impede confirmação.
- Testes anteriores de migração/backup/resolver/Coil mantidos; não foi criada DDL ou migração nova. Não são testes de interação Compose em aparelho.

Revisados diff completo/staged e whitespace. Conferidos 121 links locais nos documentos da entrega, 62 chaves en/pt-BR e referências de strings sem duplicatas/ausências, 22 hashes de fontes e ausência de controles bidi nesses arquivos. Manifesto staged contém somente os 32 arquivos da sessão, sem APK/cache/keystore. Verificações finais detalhadas no snapshot.

## Limites e ponto de parada

Implementação local permite revisar/corrigir vínculos e controlar os dados enriquecidos; aceite visual/acessível em Android continua pendente. Quota agregada com consumidores herdados, fila/worker/retomada, medições e gatilhos pertencem à sessão 9. Preview de miniaturas não persiste bytes e pode repetir download ao revisitar resultados. Pendências de backup têm apresentação e instruções conservadoras; remapeamento assistido individual continua melhoria futura. Sem push, PR remoto, tag ou release. Não iniciar sessão 9 sem autorização explícita.
