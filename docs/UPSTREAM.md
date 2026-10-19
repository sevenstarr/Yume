# Origem e reprodução do Yume

Data de referência: 19/10/2026. Este documento descreve a fundação local, sem afirmar que já existe um fork hospedado no GitHub.

## Proveniência

| Campo | Valor |
| --- | --- |
| Upstream canônico | https://github.com/animiruorg/Animiru |
| Branch auditada | `animiru-new-main` |
| Base fixada | `0b9bb0a347aa7bcee226d8782d98d9abafc34b32` |
| Data do commit upstream | 16/10/2026 |
| Release de comparação | `v0.20.0.1`; não é equivalente ao HEAD auditado |
| Repositório local | `Animiru/` dentro do workspace Yume; nome da pasta preservado |
| Remote local | `upstream` → `https://github.com/animiruorg/Animiru.git` |
| Remote `origin` Yume | Ausente; depende da conta e criação autorizada do fork |
| Branch de integração local | `main`, no SHA da base |
| Branch desta sessão | `chore/session-1-foundation`, descendente de `main` |

Não foi executado `git init`, importação por cópia sem histórico, rebase de commits upstream ou troca de autores. A branch original `animiru-new-main` permanece disponível. O clone inicial era raso; o resultado da obtenção do histórico completo consta nas [evidências da sessão 1](evidence/SESSION_1.json).

Os snapshots em `docs/upstream/` preservam README, CONTRIBUTING e CODE_OF_CONDUCT da base. Links relativos nesses snapshots pertencem ao contexto original; para navegação atual use os documentos da raiz. O changelog original continua abaixo da seção Yume em `CHANGELOG.md`.

## Reproduzir somente a base

Instruções, não comandos executados nesta ordem na sessão:

```powershell
git clone https://github.com/animiruorg/Animiru.git Animiru-reference
git -C Animiru-reference switch --detach 0b9bb0a347aa7bcee226d8782d98d9abafc34b32
```

A sessão 2 deverá preparar JDK/SDK, compilar essa referência e registrar testes. O commit fixado é um ponto de partida auditado, ainda sem baseline de execução. Não confundir com uma garantia de build verde.

## Criar o fork remoto quando autorizado

A [orientação oficial do GitHub](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/fork-a-repo) descreve a criação de um fork com vínculo ao repositório pai. No [formulário de fork do Animiru](https://github.com/animiruorg/Animiru/fork), confirmar conta/organização, nome `Yume`, visibilidade resultante e depois criar. Não usar importação para um repositório vazio como substituto desse vínculo.

Após criação autorizada, verificar na API/página `fork=true` e `parent.full_name=animiruorg/Animiru`, confirmar a URL real e só então adicionar `origin`. Nenhuma URL de destino foi configurada por suposição. A publicação das branches locais, configuração da branch padrão `main`, proteções e abertura de PR precisam de autorização. O [rascunho do PR](SESSION_1_PR.md) já pode ser revisado localmente.

Os workflows herdados permanecem intactos. O build escuta push em `main` e PRs; o workflow de release contém guardas `github.repository == 'animiruorg/Animiru'`. Não se declarou CI Yume verde nem se habilitou publicação. Não importar segredos de assinatura upstream. Antes de qualquer push autorizado, revisar gatilhos, permissões e efeitos da execução dos workflows; a configuração completa de CI/release é a sessão 11.

## Sincronização futura

1. Começar com checkout limpo e registrar a base Yume e o SHA upstream pretendido.
2. Buscar o upstream sem mover automaticamente `main`: `git fetch upstream --tags`.
3. Comparar os SHAs e criar uma branch `chore/upstream-<data>` a partir de `main`.
4. Fazer merge do SHA upstream revisado, preservando commits/autoria. Resolver conflitos com foco nas pequenas mudanças do fork; não usar reset destrutivo nem force-push.
5. Rodar build, testes e regressões de banco/backup/identidade pertinentes; revisar atribuições e atualizar a base documentada somente após integração.
6. Preparar revisão. Envio, PR e integração no remoto seguem a autorização vigente.

## Diferenças atuais

Somente documentação, avisos, templates e `.gitignore`. Código Kotlin, SQL, recursos, manifesto, dependências, Gradle Wrapper e workflows não foram alterados. A identidade Android futura é tratada em [IDENTITY.md](IDENTITY.md).
