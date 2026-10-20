# Origem e reprodução do Yume

## Protocolo vigente de branches — 20/10/2026

Consulte [AGENTS.md](../AGENTS.md). A integração usa `yume-develop`, versões estáveis usam `yume-main` e sessões usam `feature/session-XX-descricao`, sempre baseadas no `origin/yume-develop` atualizado. PRs têm base `yume-develop`; publicação e merge exigem autorização. As tabelas e descrições da fundação abaixo são históricas.

Na atualização local, `main` passou a `yume-main` preservando seu SHA auditado; isso não representa uma promoção de versões nem uma release aprovada. As branches locais das sessões 1–9 foram renomeadas ao novo padrão. `git fetch origin` confirmou `origin/yume-develop` e `origin/yume-release`; a adequação remota de `yume-release` para `yume-main`, branch padrão e proteções permanece pendente. As referências de upstream conservam seus nomes originais.

Data de referência: 19/10/2026. O fork público foi criado com autorização explícita no mesmo dia; a fundação documental continua local, sem push ou PR.

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
| Fork público | https://github.com/sevenstarr/Yume; página confirma origem `animiruorg/Animiru` |
| Remote `origin` Yume | `https://github.com/sevenstarr/Yume.git`, configurado após criação e verificação |
| Branch padrão remota atual | `animiru-new-main`, herdada da criação do fork; configurações GitHub não alteradas |
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

## Criação do fork remoto

**Executado em 19/10/2026 após resposta explícita “autorizo” à criação pública de `sevenstarr/Yume`.** Formulário GitHub utilizado com owner sevenstarr, nome Yume e cópia apenas da branch padrão. API confirmou ID `1426233623`, URL/clone URL e visibilidade pública; a página do repositório confirmou `forked from animiruorg/Animiru`. Registro em [FORK_CREATION.json](evidence/FORK_CREATION.json).

Configurado localmente `git remote add origin https://github.com/sevenstarr/Yume.git`. Nenhum push, PR, release, mudança de branch padrão ou configuração GitHub foi executado. As instruções abaixo descrevem o procedimento de criação/verificação utilizado e a separação das etapas futuras.

A [orientação oficial do GitHub](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/fork-a-repo) descreve a criação de um fork com vínculo ao repositório pai. No [formulário de fork do Animiru](https://github.com/animiruorg/Animiru/fork), confirmar conta/organização, nome `Yume`, visibilidade resultante e depois criar. Não usar importação para um repositório vazio como substituto desse vínculo.

Após criação autorizada, verificar a relação de fork na API ou na indicação de origem da página, confirmar a URL real e só então adicionar `origin`. A publicação das branches locais, configuração da branch padrão `yume-main`, proteções e abertura de PR precisam de autorização. O [rascunho do PR](SESSION_1_PR.md) já pode ser revisado localmente.

Os gatilhos foram alinhados ao protocolo: build em push para `yume-develop`/`yume-main` e PRs; release em `yume-main` ou tags `v*`; o workflow de release contém guardas `github.repository == 'animiruorg/Animiru'`. Não se declarou CI Yume verde nem se habilitou publicação. Não importar segredos de assinatura upstream. Antes de qualquer push autorizado, revisar gatilhos, permissões e efeitos da execução dos workflows; a configuração completa de CI/release é a sessão 11.

## Sincronização futura

1. Começar com checkout limpo e registrar a base Yume e o SHA upstream pretendido.
2. Buscar o upstream sem mover automaticamente `yume-develop` ou `yume-main`: `git fetch upstream --tags`.
3. Executar `git fetch origin`, conferir `origin/yume-develop` e criar uma branch `feature/session-XX-upstream` diretamente dessa referência; comparar os SHAs antes de incorporar upstream.
4. Fazer merge do SHA upstream revisado, preservando commits/autoria. Resolver conflitos com foco nas pequenas mudanças do fork; não usar reset destrutivo nem force-push.
5. Rodar build, testes e regressões de banco/backup/identidade pertinentes; revisar atribuições e atualizar a base documentada somente após integração.
6. Preparar revisão. Envio, PR e integração no remoto seguem a autorização vigente.

## Diferenças atuais

Somente documentação, avisos, templates e `.gitignore`. Código Kotlin, SQL, recursos, manifesto, dependências, Gradle Wrapper e workflows não foram alterados. A identidade Android futura é tratada em [IDENTITY.md](IDENTITY.md).
