# Sessão 11 — comandos, evidências e limites

22/10/2026, America/Sao_Paulo. Assistido pelo Codex e autorizado pelo proprietário.

## Base e continuidade

- Contexto externo `graphify-out/READ_FIRST.md`, `summary.json` e consulta por
  `query_graph.py 'workflow build release'`; somente subgrafo, não leitura integral.
  Snapshot `25d50a506` anterior às correções locais, conferidas nas fontes atuais.
- Lidos AGENTS do workspace/repositório, prompt mestre de Downloads/cópia local,
  arquitetura, progresso e relatório/registro da sessão 10. Git status, cinco
  commits e escopo de HEAD conferidos. Sessão 10 tem aceite documental com
  Android/performance/segurança remanescentes explícitos.
- Primeiro fetch restrito falhou por conexão; repetição com acesso autorizado
  e nova atualização na retomada passaram. `origin/yume-develop=25d50a506`.
- Criada `feature/session-11-ci-distribution` diretamente nessa integração.
  Proprietário autorizou incorporar `33878ff77` e `1b848e794`; aplicado
  `git merge --ff-only 1b848e794`, sem conflitos e com commits separados.
  `git merge-base --is-ancestor origin/yume-develop HEAD` passou.
- `docs/prompt.md` não rastreado preservado. Nenhum push, PR, merge remoto,
  promoção à principal, tag, release ou configuração remota nesta sessão.

## Ferramentas e validação estática

- Python 3.12 do runtime local (alias WindowsApps indisponível); scripts novos
  exigem Python 3.11+ e usam só biblioteca padrão.
- actionlint **1.7.12** obtido da release oficial, ZIP Windows amd64 verificado
  contra arquivo de checksums oficial: SHA-256
  `6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9`.
  Download somente em `build/session11/tools/`, ignorado.
- Primeira validação apontou somente runner `ubuntu-26.04` desconhecido pelo
  inventário antigo do actionlint. Disponibilidade confirmada no
  [inventário oficial](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2604-Readme.md).
  Declarado esse label no arquivo **local** `build/session11/actionlint.yaml`;
  nova validação dos dois workflows passou. Não foram ignoradas outras regras.
- Comando: `actionlint.exe -config-file build/session11/actionlint.yaml
  -shellcheck= -pyflakes= .github/workflows/build.yml .github/workflows/release.yml`.
  Verifica YAML/expressões/estrutura. Shellcheck e pyflakes não disponíveis nesta
  rodada; não alegar análise desses validadores. Steps de Actions e upload/cache
  não foram executados no serviço remoto.

```sh
python -m unittest discover -s scripts/tests -v
python scripts/verify-metadata-migration.py
python scripts/verify-metadata-sync-migration.py
python scripts/check-ci-reports.py
git diff --check
```

Nove testes sintéticos dos gates passaram: relatório ausente, suíte inteiramente
ignorada, testes com erros/falhas, lint com erro, warnings permitidos, integridade
de hashes/recusa de saída antiga, assinatura ausente, pacote/ABI incorretos.
As fixtures de APK são texto sintético e não provam assinatura Android. Candidato
de release com alterações rastreadas também é recusado.
Migrações SQLite sintéticas passaram, preservando 11/18 tabelas, FKs, opt-ins e
rollback. Leitura inicial dos XMLs herdados encontrou 306 testes e zero erros de
lint; resultados da execução Gradle da sessão são registrados abaixo separadamente.

## Build e verificação de assinatura

JDK 21.0.11, SDK em `.tooling/android-sdk`, GRADLE_USER_HOME em `.tooling/gradle`,
ANDROID_USER_HOME em `.tooling/android-user`, escopos de processo. Execução com
acesso autorizado aos caches devido às falhas de acesso já comprovadas na sessão 10.

```sh
./gradlew spotlessCheck testDebugUnitTest verifySqlDelightMigration :app:lintDebug :data:lintDebug :source-local:lintDebug :presentation-widget:lintDebug :app:assembleDebug -Pdist=ci --console=plain --stacktrace --max-workers=2
```

Log completo em `build/session11/checks.log`, ignorado. **BUILD SUCCESSFUL em
13m52s**, 815 tarefas acionáveis: 66 executadas e 749 atualizadas. App executou
86 testes; domain/data/source-local reutilizaram respectivamente 162/54/4
resultados `UP-TO-DATE`. Total 306, zero falhas/erros/ignorados. SQLDelight debug/
release também `UP-TO-DATE`; as duas migrações sintéticas foram reexecutadas.
Spotless aprovado. Lint XML: zero erros, avisos app 204/data 0/fonte local 3/
widgets 4; app regenerado, demais relatórios reutilizados.

Cinco APKs debug tiveram `apksigner verify --verbose --print-certs` aprovado,
assinatura v2. ARM64 inspecionado com `aapt2 dump badging`: pacote
`io.github.sevenstarr.yume.dev`, versionName `0.20.0.1-8272`, versionCode 153,
minSDK 26, targetSDK 36. Não equivale a instalação real. Empacotador executado
contra os APKs reais, não fixtures. Manifesto marca `trackedChanges=true` e
commit-base `1b848e794`, pois o build ocorreu antes do commit da sessão;
não é artifact remoto do SHA final.

```sh
apksigner verify --verbose --print-certs <apk>
python scripts/package-ci-apks.py --variant debug --output build/session11/ci-apks-verified
sha256sum --check SHA256SUMS
```

Primeiro `sha256sum --check` falhou: Python escrevia CRLF no Windows e o shell
interpretava CR como parte dos nomes. Corrigido `write_text(newline="\n")`,
acrescentada asserção de bytes sem CR, nove testes passaram novamente e novo
empacotamento teve **cinco checksums OK**. A saída antiga foi preservada, sem
misturar artifacts. O apksigner atual imprime `V2 Signer`, não somente `Signer #1`:
extração ajustada para o campo SHA-256, com normalização de CR e unicidade;
teste shell com relatório real aceitou o digest correto e recusou outro.

actionlint e os **14 blocos de shell** (`bash -n`) passaram novamente após essas
correções. Logs/helper scripts ficam somente em `build/session11/`, ignorado.
Relatórios e APKs finais locais em `build/session11/ci-apks-verified/`.

`./gradlew :app:spotlessKotlinGradleCheck :app:signingReport -Pdist=github`
passou em **31s** após a última alteração do DSL. Assinatura: debug e benchmark
usam `debug`; release/foss/preview mostram `Config: none`. As variantes auxiliares
criadas pelo plugin de baseline profile continuam com chave de desenvolvimento.

Primeira tentativa negativa com `--no-configuration-cache` falhou antes da guarda:
`Configuration Cache cannot be disabled when Isolated Projects is enabled`.
A tentativa de desligar por `-Dorg.gradle.isolated-projects=false` nesta chamada
PowerShell também não desativou o recurso. A opção CLI própria
`--no-isolated-projects` resolveu a incompatibilidade sem mudar `gradle.properties`.
O workflow de assinatura foi corrigido para usar ambas as opções. A exigência de
cache por Isolated Projects foi conferida na
[documentação Gradle](https://docs.gradle.org/current/userguide/isolated_projects.html).

Com `YUME_RELEASE_SIGNING=true`, sem caminho de chave e
`--no-configuration-cache --no-isolated-projects`, a tarefa
`:app:generateReleaseBuildConfig -Pdist=github` foi recusada em **15s** pela mensagem
exata `Release signing requires YUME_KEYSTORE_PATH`. O wrapper de teste retornou
zero somente após confirmar essa falha esperada. Não confundir as duas primeiras
falhas de opções com aprovação da guarda. Nenhuma chave foi necessária.

Sem ativar assinatura, `:app:generateReleaseBuildConfig -Pdist=github --no-daemon
--no-configuration-cache --no-isolated-projects` passou em **29s**. BuildConfig
gerado tem `UPDATER_ENABLED=false` e `CAST_INCLUDED=true`. Não foi executado
`assembleRelease`/R8 com chave do proprietário; o teste cobre configuração e
guarda, não o candidato assinado integral. As opções isoladas foram verificadas
sem alterar defaults do CI ou `gradle.properties`.

Snapshot agregado, hashes públicos dos APKs e lista dos arquivos alterados em
[SESSION_11.json](SESSION_11.json). Diferenças completas de código/documentação e
whitespace revisadas antes do commit exclusivo da sessão. O único arquivo
preexistente não rastreado (`docs/prompt.md`) permanece fora desse commit.

## Aceite e dependências externas

CI remoto verde, download de artifact remoto, instalação real desse artifact e
candidato assinado pelo proprietário **ainda não comprovados**. Não há autorização
para enviar a branch, abrir PR, criar/configurar `release-signing`, cadastrar
secrets, promover `yume-main`, disparar assinatura ou publicar. O workflow manual
permanece bloqueado sem configuração explícita do ambiente. Nenhuma chave do
proprietário foi criada, recebida ou utilizada.

Testes Android, desempenho/bateria/Doze, OAuth/provedores e revisão de dependências
nativas da sessão 10 continuam pendentes. APK debug válido não encerra esses itens.
Reprodutibilidade bit a bit não foi medida; horário/chave debug/runner/dependências
não congelados são limites conhecidos. Não houve operação de dispositivo nesta sessão.
