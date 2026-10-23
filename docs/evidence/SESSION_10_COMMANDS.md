# Sessão 10 — qualidade, segurança e compatibilidade

22/10/2026. Trabalho assistido pelo Codex, autorizado pelo proprietário.
Branch `feature/session-10-quality`, criada em `origin/yume-develop`
`25d50a506b36b29c4ae47b76bc354bec4ac6f6ed`, após fetch concluído.
O proprietário autorizou incorporar o commit local `33878ff77` por fast-forward.
Esse commit permanece separado das alterações desta sessão. Sem push ou PR.
O arquivo não rastreado `docs/prompt.md` foi preservado.

## Continuidade

Lidos o contexto do mapa externo e um subgrafo de testes, transporte, capas e
backup; o snapshot corresponde à integração, mas não às correções de `33878ff77`.
Conferidos os arquivos atuais, prompt mestre, AGENTS, arquitetura, progresso,
status, cinco commits e escopo do último commit. A sessão 9 está integrada;
seu aceite Android permanece parcial. As evidências históricas não são tratadas
como novas execuções. Nenhum artefato Graphify foi incluído no repositório.

## Mudanças e reprodução

- `CoverTransportTest`: matriz de 13 URLs hostis (HTTP, host falso, credenciais,
  portas, fragmentos, traversal codificado, barra invertida, loopback, esquemas
  locais e tamanho excessivo), exigindo zero chamadas HTTP.
- `AniListTransportTest`: orçamento de 2 MiB medido em bytes UTF-8; aceita o
  limite exato e recusa um byte além, com texto multibyte.
- `scripts/check-declared-dependencies.py`: consulta pública OSV das coordenadas
  Maven com versão explícita nos catálogos; JSON com omissões. Python 3.11+,
  biblioteca padrão; exit 1 se houver alertas, falha explícita se a consulta falhar.

Com JDK 21.0.11, SDK/caches locais do baseline e dois workers:

```powershell
./gradlew.bat testDebugUnitTest :app:assembleDebug verifySqlDelightMigration spotlessCheck :app:lintDebug :data:lintDebug :source-local:lintDebug :presentation-widget:lintDebug --console=plain --stacktrace --max-workers=2
python scripts/verify-metadata-migration.py
python scripts/verify-metadata-sync-migration.py
python scripts/check-declared-dependencies.py --output build/session10/dependencies.json
git diff --check
```

O alias Python do WindowsApps não funciona neste host; foi usado o Python 3.12
do runtime local. Primeiro fetch/consulta OSV restritos falharam por rede;
repetições com acesso autorizado passaram. O Gradle restrito registrou acesso
negado ao marcador do daemon Kotlin e aviso de métricas Android. Resultados
finais e eventual repetição são registrados em `SESSION_10.json`.

Logs e respostas completas locais: `build/session10/`, ignorado. O snapshot
versionado contém agregados, sem dados de usuário. Não foram extraídos bancos
privados do aparelho, nem executados fluxos destrutivos ou consultas AniList reais.

## Escopo da evidência de segurança

- OSV: 118 coordenadas/versões, zero pacotes com alertas retornados; 15 declarações
  omitidas por não fornecerem versão exata resolvível. Não cobre transitivas,
  BOMs, plugins Gradle ou binários nativos e não prova que as versões estão
  indexadas ou livres de vulnerabilidades.
- Segredos: quatro regexes (chave privada, token GitHub, access key AWS, API key
  Google), 1.960 arquivos UTF-8 rastreados, zero correspondências. Não inclui
  histórico, entropia, arquivos binários, armazenamento runtime ou credenciais
  de serviços em formatos diferentes.
- Manifesto fonte: 14 permissões declaradas e quatro componentes explicitamente
  exportados. MainActivity, DeepLinkActivity e TrackLoginActivity recebem entradas
  externas; ShizukuProvider exige `INTERACT_ACROSS_USERS_FULL`. FileProvider não
  exportado, grants por URI. Isto é inspeção estática, sem fuzzing de intents e
  sem auditoria completa do manifesto mesclado de todas as variantes.
- Transporte de metadados usa endpoint HTTPS fixo, sem cookies, authenticators,
  interceptors de fonte, redirects ou retries transparentes. CDN usa allowlist,
  orçamento de bytes e timeout; testes existentes e novos verificam essas regras.
- Busca por `logcat`, `println`, `Timber` e `Log.` no provider, domínio/persistência
  de metadados e fetcher/interceptor de capas não encontrou chamadas. Não é
  auditoria dos logs herdados, crash reporting, sistema ou extensões.
- Workflow de build inspecionado: `contents: read`, ações fixadas por SHA,
  dependency-review em PR, sem `pull_request_target`. Não houve execução remota
  ou mudança de CI nesta sessão.

## Riscos encontrados e próximos testes

| Prioridade | Observação e consequência | Próxima ação |
| --- | --- | --- |
| Alta antes de distribuição | Rede herdada permite HTTP em claro e certificados de usuário globalmente; WebView Safe Browsing desativado | Revisar compatibilidade das fontes e definir política por consumidor; testar WebView/extensões antes de endurecer |
| Alta antes de distribuição | Clientes/callbacks OAuth ainda herdados, conforme identidade; não há clientes próprios confirmados | Registrar clientes próprios e testar autenticação/coexistência antes de habilitar distribuição |
| Alta | Dependências transitivas/nativas e fontes correspondentes não auditadas integralmente | Inventário do APK e análise de advisories/licenças por versão efetivamente resolvida |
| Média | Superfícies exportadas recebem intents/URIs externas | Testar entradas malformadas e permissões em instalação sintética Android |
| Média | Quatro padrões de segredo não cobrem histórico nem todos os tipos de token | Executar scanner abrangente no histórico antes da publicação de binários |

Não foram alteradas políticas herdadas sem prova de compatibilidade. Updater
continua bloqueado pelo commit incorporado. Confirmação AniList permanece adiada
por orientação do proprietário; automação textual segue desligada.

## Encerramento das verificações

Execução restrita: falhou em 4m49 por AccessDeniedException no classes.jar de presentation-widget. Repetição com acesso autorizado aos caches: BUILD SUCCESSFUL em 11m, 66 tarefas executadas e 745 atualizadas. App executou 86 testes; domain/data/source-local reutilizaram 220 resultados UP-TO-DATE. Total 306, zero falhas/erros/ignorados. Spotless, APK e SQLDelight aprovados. Lint: zero erros; avisos app 204/data 0/source-local 3/widgets 4. ARM64 verificado com apksigner verify --verbose, assinatura v2 aprovada. Revisão integral do diff e git diff --check aprovados antes do commit.
