<!-- Modified for Yume on 2026-10-19: local workflow, attribution and review policy. -->
# Contribuindo com o Yume

Yume deriva do Animiru. Consulte o [guia original e seus créditos](docs/upstream/CONTRIBUTING.md), a [origem](docs/UPSTREAM.md), o [código de conduta](CODE_OF_CONDUCT.md) e o [progresso](docs/PROGRESS.md). O [fork público](https://github.com/sevenstarr/Yume) foi criado com autorização; canais privados e envio desta fundação permanecem pendentes. Problemas específicos do Yume devem ser tratados no repositório Yume, sem encaminhar automaticamente seus usuários ao suporte upstream.

## Branches e commits

- `yume-main`: versões estáveis e releases aprovadas.
- `yume-develop`: integração de funcionalidades concluídas, testadas e aprovadas.
- Novas sessões: `feature/session-XX-descricao`; correções isoladas: `fix/descricao`. Criar sempre a partir de `origin/yume-develop` após `git fetch origin`. Não commitar diretamente em `yume-develop` ou `yume-main`. PRs usam base `yume-develop`; sincronizar por merge e revalidar antes de abrir/atualizar. Consulte o [protocolo completo](AGENTS.md).
- Commits pequenos com `tipo: resumo`, resumo preferencialmente até 50 caracteres e corpo explicando o motivo e a validação. Não adicionar tags de release sem autorização.
- Não rebasear nem alterar autoria do histórico importado. Para sincronização, seguir [UPSTREAM.md](docs/UPSTREAM.md), com revisão e testes antes da integração.

Os commits automatizados locais desta fundação usam `Codex (AI assistant) <codex@invalid>`, identidade explícita sem endereço pessoal. Não representa autoria humana nem uma conta GitHub. Contribuições humanas posteriores usam a identidade escolhida pelo próprio autor, preferencialmente um endereço GitHub noreply se desejado.

## Mudanças e evidências

Antes de editar, inspecione os arquivos reais. Preserve copyright, licença, avisos e os marcadores `AM`/créditos herdados. O guia upstream pede marcação das alterações de código com `AM (<título>) --> ... <-- AM (<título>)`; esta sessão não altera código Android. Ao introduzir código futuro, documente sua origem, mantenha os créditos existentes e inclua aviso de modificação adequado ao formato do arquivo. Arquivos herdados editados nesta sessão trazem aviso datado de modificação Yume.

O PR deve explicar o problema, comportamento resultante, arquivos relevantes, riscos e validações efetivamente realizadas. Diferencie código herdado, adaptação de terceiros e assistência por IA; não declare testes, screenshots, autoria ou funcionalidades que não existem. Não inclua tokens, credenciais OAuth privadas, arquivos de assinatura, backups, bancos pessoais ou logs sem revisão.

## Verificação

Ferramentas exigidas pelo upstream: Kotlin/Android, JDK 21 e Android SDK/NDK nas versões dos catálogos. Use o wrapper do repositório. Comandos de referência, ainda não executados nesta fundação:

```powershell
./gradlew.bat spotlessCheck
./gradlew.bat testDebugUnitTest
./gradlew.bat verifySqlDelightMigration
./gradlew.bat assembleDebug -Penable-updater=false
```

Execute verificações proporcionais à mudança e registre limitações. Alterações de biblioteca, banco, backup, player ou identidade exigem regressão adequada. Para mudanças visuais, forneça screenshots reais e verifique temas, acessibilidade e telas grandes. `spotlessApply` modifica arquivos; revise o diff antes de incluí-los.

## Revisão e publicação

Prepare o PR localmente com o template e evidências. Nesta execução, envio ao remoto, abertura de PR, mudança de configuração GitHub e publicação dependem de autorização explícita do proprietário. O [rascunho da fundação](docs/SESSION_1_PR.md) está pronto para revisão local. Relatos de vulnerabilidade seguem [SECURITY.md](SECURITY.md).
