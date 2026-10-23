# CI, assinatura e distribuição de builds

Sessão 11, 22/10/2026. Implementação assistida pelo Codex. Este documento
descreve o processo preparado; resultados executados estão em
[SESSION_11_COMMANDS](evidence/SESSION_11_COMMANDS.md). A assinatura, a promoção
para `yume-main`, tags e publicação exigem autorização do proprietário.

## CI de PR e push

**Conferência da sessão 12, 22/10/2026:** PR #5 aberta, cabeça `91a378cee`.
A [execução 39877855871](https://github.com/sevenstarr/Yume/actions/runs/39877855871)
falhou em `Install required Android SDK`: `sdkmanager: command not found`,
exit 127 no runner Ubuntu 26.04. Checkout, Dependency Review, JDK e setup Gradle
passaram; testes, lint, build e upload de APK foram ignorados. O passo de upload
de relatórios não encontrou arquivos. Não há artifact instalável desta execução.
Corrigir a preparação/PATH das Command-line Tools e revalidar CI é pendência
da sessão 11; esta sessão documental não modifica workflows ou configurações.

`.github/workflows/build.yml` mantém o check **Build & Test App**. Executa em
PRs para `yume-develop`/`yume-main` e pushes nessas branches e `feature/**`/`fix/**`.
Não há filtros que deixem PRs documentais com check obrigatório pendente.

- Ubuntu 26.04, JDK 21 de `.github/.java-version`, wrapper Gradle 9.7.1.
- Instalação explícita de SDK 37.2 e Build Tools 36.0.0/37.0.0.
- Actions preservadas nos SHAs já adotados; checkout completo, sem persistir
  credenciais. O histórico completo mantém o contador de commits do versionName.
- Cache Gradle `basic`, gravável somente em push de integração/principal;
  PRs e branches temáticas apenas leem. Nenhum secret é fornecido ao CI.
- `spotlessCheck`, testes JVM, SQLDelight, lint debug dos quatro módulos e
  migrações SQLite sintéticas. As tarefas Gradle usam `-Pdist=ci` consistentemente.
- `check-ci-reports.py` exige suítes JUnit não vazias de app/domain/data/source-local
  e XMLs de lint de app/data/source-local/presentation-widget. Falhas, erros,
  suítes inteiramente ignoradas e erros/fatais de lint bloqueiam o build.
  Avisos são contados, sem alegar lint sem achados. Os relatórios podem ser
  reutilizados pelo Gradle; o script não comprova reexecução das tarefas.
- Dependency Review preservado em PRs. Não usa `pull_request_target`, não executa
  código de PR com secrets e limita `GITHUB_TOKEN` a `contents: read`.

Depois dos checks, gera cinco APKs **debug** com pacote
`io.github.sevenstarr.yume.dev`, verifica cada assinatura com `apksigner` e
envia `yume-debug-<SHA>` por **sete dias**. Contém universal, ARM64, ARMv7, x86 e
x86_64, `SHA256SUMS`, relatórios de certificado e `build-manifest.json` com commit,
estado de alterações rastreadas, variante, pacote, versão, ABI, tamanho e hash.
O empacotamento recusa saída antiga, pacote incorreto, ABI ausente/duplicada,
APK vazio e relatório de assinatura ausente. O exit code de `apksigner` é a
evidência criptográfica; o empacotador não substitui essa ferramenta.

Relatórios de testes/lint são enviados também em falha e expiram em sete dias.
Artifacts de PR são código não revisado: validar o SHA e usar instalação
sintética de teste. Assinatura válida não comprova segurança nem teste de UI.

## Download, checksum e instalação de teste

Na execução aprovada de Actions, baixar o artifact do SHA revisado e extrair o
ZIP. Dentro da pasta que contém os APKs:

```sh
sha256sum --check SHA256SUMS
apksigner verify --verbose --print-certs Yume-arm64-v8a-debug.apk
adb install -r Yume-arm64-v8a-debug.apk
```

Em PowerShell, `Get-FileHash -Algorithm SHA256 <arquivo>` permite comparar cada
linha de `SHA256SUMS`. Usar universal quando a ABI for desconhecida. Manter backup
de dados antes de testar atualizações. Cada runner pode criar outro certificado
debug: um APK debug de outra execução pode não atualizar a instalação anterior.
Não desinstalar um app com dados pessoais para contornar incompatibilidade.
Debug `.dev` e release sem sufixo são instalações diferentes; não transferem
automaticamente dados do Animiru nem um do outro.

## Candidato de release, sem publicação automática

`.github/workflows/release.yml` aceita **somente `workflow_dispatch`**. Push de
branch/tag não aciona assinatura ou publicação. O job exige `sevenstarr/Yume`,
ref `refs/heads/yume-main`, input booleano `approved=true` e ambiente
`release-signing` com `YUME_RELEASE_SIGNING_READY=true`. A flag é um gate de
prontidão, não substitui reviewers/proteções nem autorização de publicação.

Antes de habilitar, o proprietário deve autorizar e configurar o ambiente:

| Tipo | Nome | Uso |
| --- | --- | --- |
| Secret | `YUME_KEYSTORE_BASE64` | Keystore própria codificada em base64 (não é criptografia) |
| Secret | `YUME_STORE_PASSWORD` | Senha da keystore |
| Secret | `YUME_KEY_ALIAS` | Alias da chave |
| Secret | `YUME_KEY_PASSWORD` | Senha da chave |
| Variável | `YUME_RELEASE_CERT_SHA256` | Digest SHA-256 do certificado esperado, 64 hex minúsculos sem separadores |
| Variável | `YUME_RELEASE_SIGNING_READY` | `true` somente após testar configuração e proteções |

Restringir deployment branches a `yume-main`, exigir revisão do proprietário e
impedir bypass onde disponível. Se o plano da conta não suportar proteção
adequada, manter o gate desligado e assinar offline. Esta sessão não cria ambiente,
secrets, chave ou alterações de configuração remota.

O job valida fontes sem secrets de assinatura, desliga cache de Actions e só
disponibiliza as credenciais no passo de build `:app:assembleRelease -Pdist=github`.
Keystore temporária usa permissões restritas, trap de remoção e limpeza final.
Esse build desliga configuration cache, Isolated Projects (que exige esse cache)
e daemon persistente apenas na invocação assinada. Cada APK deve passar
`apksigner verify` e corresponder ao certificado configurado antes do upload.
O artifact `yume-release-candidate-<SHA>` expira em sete dias; contém APKs e mapping
R8. O workflow não tem `contents: write`, criação de tag, Release ou upload público
em canal de distribuição. Artifacts ainda seguem a visibilidade/acesso do repo.

`app/build.gradle.kts` usa `yumeRelease` somente nas variantes de release.
Debug conserva a chave debug do Android. Sem material próprio, release/foss/
preview ficam **sem assinatura**, sem fallback à chave debug. A variante de
desenvolvimento `benchmark` usa explicitamente a chave debug para preservar a
instalação pelo profiler; não integra os artifacts de distribuição.
Se `YUME_RELEASE_SIGNING=true`, as quatro variáveis de caminho/senha/alias são
obrigatórias; o caminho local é `YUME_KEYSTORE_PATH`. Alternativamente, um
`keystore.properties` local ignorado pode configurar `yumeRelease` com campos
`storeFile`, `storePassword`, `keyAlias`, `keyPassword`. O antigo contrato
`ANIMIRU_GITHUB_RELEASE` deixa de ser usado; não reutilizar chave upstream.

## Custódia, backup e rotação

A chave pertence ao proprietário: criar fora do Git, com identidade real e
senhas administradas por ele. Não enviar arquivos/senhas ao chat nem a issues.
Manter duas cópias criptografadas separadas, credenciais em cofre separado,
certificado público/digest e instruções de recuperação. Testar recuperação
assinando apenas um APK sintético/candidato e comparando o certificado.

Perda ou troca de certificado pode impedir atualização do mesmo pacote. Não
gerar uma chave nova silenciosamente. Em comprometimento, suspender candidatos,
revogar acesso aos secrets e avaliar rotação com lineage `apksigner`, compatibilidade
Android e eventual canal de distribuição antes de publicar. Recuperação de upload
key do Play não equivale à rotação de chave de APK distribuído diretamente.

## Versões, publicação e updater

O build ainda herda `versionName=0.20.0.1`, `versionCode=153`; a sessão não os
incrementa nem cria tag. Antes da primeira release, revisar `app/build.gradle.kts`
e `CHANGELOG.md` em commit aprovado: versionCode deve crescer para atualizações
do mesmo pacote, versionName deve identificar a versão Yume, e a tag `v<versionName>`
deve apontar ao commit promovido/testado em `yume-main`. Registrar SHA, versão,
certificado, checksums, limitações e testes do APK final nas release notes.

Publicar somente após autorização explícita, checks verdes no SHA final, revisão
de licenças/provedores/OAuth e testes Android pendentes da sessão 10. Validar
instalação e atualização com a mesma chave, dados sintéticos e backup. Não
recompilar após validar o candidato: distribuir exatamente os bytes cujo hash
foi aprovado. Tag, GitHub Release e publicação continuam manuais e separadas.

O updater permanece desligado em todas as distribuições. Futuro canal próprio
exigirá endpoint do fork, validação de pacote/certificado/hash, tratamento de
versão e testes de atualização. Não reativar endpoint do Animiru.

Reprodução aqui significa ferramentas, comandos, variante e proveniência
documentados. Não há prova de APKs byte a byte idênticos: debug inclui horário
atual e chave efêmera, runner/dependências não estão integralmente congelados,
e não foi realizada comparação de dois builds limpos assinados.

## Referências verificadas

- [Permissões mínimas de GITHUB_TOKEN](https://docs.github.com/en/actions/tutorials/authenticate-with-github_token)
  e [eventos/forks](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows).
- [Ambientes e aprovação antes de acesso a secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow).
- [Inputs da revisão Gradle adotada](https://github.com/gradle/actions/blob/9c971963bec38e04b3d30dcc455b5382be2fdbfb/setup-gradle/action.yml).
- [Runner Ubuntu 26.04](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2604-Readme.md).
- [Assinatura Android](https://developer.android.com/studio/publish/app-signing)
  e [apksigner/lineage](https://developer.android.com/tools/apksigner).
