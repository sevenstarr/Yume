# Origem e sincronização do Yume

Atualizado em 22/10/2026, sessão 12, com assistência do Codex. Este guia distingue a base importada, o incremento Yume e a integração conhecida. A [auditoria original](UPSTREAM_AUDIT.md) e os snapshots em [upstream/README](upstream/README.md) e [upstream/CONTRIBUTING](upstream/CONTRIBUTING.md) preservam o contexto histórico.

## Proveniência

| Campo | Referência |
| --- | --- |
| Projeto pai | [animiruorg/Animiru](https://github.com/animiruorg/Animiru), derivado de Aniyomi, com trabalho herdado de Mihon/Tachiyomi e seus contribuidores |
| Branch upstream auditada | `animiru-new-main` |
| Base importada fixada | `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`, commit de 16/10/2026 |
| Release usada na comparação inicial | `v0.20.0.1`; não equivale ao HEAD importado |
| Fork | [sevenstarr/Yume](https://github.com/sevenstarr/Yume), criado sob autorização em 19/10/2026; [evidência](evidence/FORK_CREATION.json) |
| Remotos do workspace | `origin`: `https://github.com/sevenstarr/Yume.git`; `upstream`: `https://github.com/animiruorg/Animiru.git` |
| Integração conhecida no fetch inicial da sessão 12 | `origin/yume-develop=25d50a506b36b29c4ae47b76bc354bec4ac6f6ed` |
| Código adicional documentado | Até `91a378cee729b45457a89c691747b49a7d3b7a16`, correções/sessões 10–11 na PR #5; incorporado localmente à sessão 12 com autorização, sem integração remota presumida |

O histórico e autores importados foram preservados, sem `git init` sobre uma cópia de arquivos nem rebase para atribuir trabalho herdado. O clone original era raso; a obtenção do histórico completo consta nas [evidências da fundação](evidence/SESSION_1.json). Não houve atualização do upstream funcional como parte desta sessão documental.

## Branches atuais e histórico de organização

`yume-develop` recebe funcionalidades aprovadas por PR; `yume-main` é reservada para versões estáveis/releases. Sessões usam `feature/session-XX-descricao` e correções `fix/descricao`, sempre a partir de `origin/yume-develop` atualizado. Consulte [AGENTS](../AGENTS.md).

A fundação usou nomes antigos como `main` e `chore/session-1-foundation`; são referências históricas nos registros. Em 20/10 as branches locais foram adequadas ao protocolo. Em 21/10 a remota `yume-release` foi renomeada para `yume-main`, preservando `787b275e8230a748661be1f7c50ebbdd49ea9533` e função de branch padrão. Isso não promoveu funcionalidades experimentais à principal.

Proteções foram registradas em 21/10 no ruleset `25605405` para integração/principal: PR, `Build & Test App`, base atualizada, conversas resolvidas e bloqueio de force-push/exclusão, sem atores de bypass. É um registro daquela conferência, não uma nova auditoria de configuração nesta sessão. Autorização humana de merge continua exigida.

## Reproduzir a base upstream

```sh
git clone https://github.com/animiruorg/Animiru.git Animiru-reference
git -C Animiru-reference switch --detach 0b9bb0a347aa7bcee226d8782d98d9abafc34b32
```

Use a referência separada para comparar comportamento, sem alterar seu checkout Yume. A sessão 2 registrou [baseline de build/testes](BUILD_BASELINE.md); seus APKs tinham identidade herdada e não são releases Yume. Para compilar o incremento atual, use [DEVELOPMENT](DEVELOPMENT.md).

## Sincronizar futuramente com o upstream

Procedimento a executar somente para um escopo de atualização autorizado:

1. Garanta checkout limpo ou preserve trabalho em andamento sem movê-lo silenciosamente. Registre SHAs Yume/upstream e escopo da atualização.
2. Execute `git fetch origin` e `git fetch upstream --tags`. Num clone novo, adicione `upstream` com a URL acima se ainda não existir. Fetch não promove branches.
3. Crie branch temática diretamente do `origin/yume-develop` recém-atualizado. Compare histórico/diff com o SHA upstream escolhido e revise identidade, updater, Gradle, migrações, licenças e dependências antes de incorporar.
4. Faça merge do SHA upstream revisado, preservando histórico/autores. Resolva conflitos mantendo comportamento herdado e incremento; decisões arquiteturais/requisitos incompatíveis exigem escolha do proprietário. Não descarte mudanças ou use force-push como atalho.
5. Execute build, testes, migrações, lint e regressões pertinentes de banco/backup, extensões, reprodução/download, identidade e capas. Registre conflitos, decisões e limitações em progresso/ADRs quando necessário.
6. Atualize novamente a base de integração antes de PR e revalide. Envio, PR e merge exigem autorização; checks obrigatórios devem passar na cabeça final. Atualize a base documentada somente após integração confirmada.

Ausência de conflito Git não prova ausência de regressão. Não promova upstream diretamente a `yume-main`. Tags, releases e exclusão de branches exigem autorização própria.

## Diferenças e atribuição

O incremento Yume inclui cliente público/matcher/revisão, SQL/cache/backup, resolver Coil, fila/quota e configurações de metadados; também identidade própria, updater desligado, correções pontuais e CI. Biblioteca, fontes, player, downloads e tracking continuam herdados. Compare a [tabela do README](../README.md), [arquitetura](ARCHITECTURE.md), [changelog](../CHANGELOG.md) e commits para avaliar o alcance real.

Preserve [LICENSE](../LICENSE), [NOTICE](../NOTICE), avisos nos fontes e créditos. O [levantamento de licenças](LICENSE_REVIEW.md) não certifica todas as dependências nativas/imagens. Código assistido pelo Codex é identificado; o mantenedor dirige/revisa/autoriza o trabalho sem reivindicar autoria dos recursos importados. Nem build aprovado nem fork público equivalem a aprovação de distribuição de binários.
