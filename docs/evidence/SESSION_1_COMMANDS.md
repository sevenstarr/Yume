# Evidências e comandos — Sessão 1

Data: 19/10/2026, America/Sao_Paulo. Shell: PowerShell. Caminhos neste registro são relativos ao repositório local `Animiru/`, salvo indicação. Não é transcrição byte a byte de todas as chamadas.

## Descoberta e leitura

Lidos `Downloads/prompt.md` integralmente, relatórios da sessão 0, LICENSE completo, README/CONTRIBUTING/CODE_OF_CONDUCT, `.gitignore`, templates e workflows. Inspecionados build logic, catálogo Android, configuração de variantes, manifesto, updater e referências OAuth/branding. Buscas `rg` localizaram os caminhos reais. Não foi encontrado AGENTS.md aplicável nos diretórios verificados.

Comandos relevantes executados:

```powershell
git status --short
git log -1 --oneline
git remote -v
git branch -a
git rev-parse --is-shallow-repository
Get-Content LICENSE -Raw
Get-Content .github/workflows/build.yml -Raw
Get-Content .github/workflows/release.yml -Raw
```

Estado inicial: branch `animiru-new-main`, base auditada, working tree limpo, `origin` apontando ao upstream, clone raso. Identidade Git de usuário não configurada nas consultas; nenhuma identidade pessoal foi criada.

## Histórico e branches locais

```powershell
git fetch --unshallow --tags origin
git branch main 0b9bb0a347aa7bcee226d8782d98d9abafc34b32
git switch -c chore/session-1-foundation main
git remote rename origin upstream
git rev-parse --is-shallow-repository
git rev-list --count main
```

Primeira tentativa de fetch sob rede restrita: exit 1, conexão GitHub 443 indisponível. Repetição pelo mecanismo autorizado de acesso à rede: exit 0. Resultado: clone completo (`false`), 8.242 commits alcançáveis em `main`, sem mover a base. O fetch terminou enquanto eram preparados os documentos; a renomeação local do remote não alterou o SHA da branch de trabalho. Nenhum push ou alteração remota foi feito.

## Fontes externas

- Conector GitHub `get_profile`, somente leitura: identificou a conta conectada `sevenstarr`. Isso é candidato de destino, não confirmação do proprietário nem autorização de criação. Email/perfil completo não foram copiados para estes documentos.
- [Apache License 2.0 oficial](https://www.apache.org/licenses/LICENSE-2.0), lida em 19/10/2026 para seções 4 e 6.
- [GitHub: Fork a repository](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/fork-a-repo), lida em 19/10/2026 para orientar a criação preservando o vínculo de fork.

Nenhuma nova consulta AniList ou download de capa nesta sessão. As evidências da sessão 0 permanecem identificadas por data e escopo.

## Edição e validação

Snapshots dos três documentos upstream copiados antes de sua edição. Relatórios da sessão 0 copiados para `docs/` no Git. Fontes novas gravadas em UTF-8; arquivos herdados editados têm aviso datado. Uma tentativa inicial de patch com exclusão/adição do mesmo caminho foi recusada antes de qualquer mudança; a escrita foi refeita pelos arquivos e verificada.

A verificação foi executada por script Python temporário, sem instalar dependências, usando Git e a biblioteca padrão do runtime local. O script não faz parte do produto nem de uma suíte de testes Android. Verifica:

- base `main` e ancestralidade da feature; clone completo e contagem de commits;
- LICENSE, snapshots e seção original do changelog contra blobs da base;
- diff restrito a documentação, templates e `.gitignore`, incluindo igualdade de workflows e arquivos funcionais;
- links de arquivos locais dos documentos novos (snapshots upstream mantêm seu contexto histórico);
- estrutura básica dos três YAMLs de issue e campos obrigatórios dos formulários; YAML completo revisado estaticamente, sem parser instalado;
- regras de ignore por `git check-ignore` sem criar arquivos secretos reais, incluindo preservação do wrapper e fixture SQLDelight;
- indicadores comuns de tokens privados, chaves e atribuições de segredos em linhas adicionadas/arquivos novos, sem imprimir valores.

O [JSON](SESSION_1.json) contém contagens e resultados observados, inclusive limitações. Não é auditoria completa de segredos/licenças no histórico ou nos binários.

Tentativa de importar PyYAML: `ModuleNotFoundError`; nenhuma dependência foi instalada por causa disso. A revisão dos templates usa inspeção estática e checagens estruturais, sem alegar validação pela API GitHub ou por parser YAML.

Também executados `git diff --check`, revisão do diff e index, staging explícito dos arquivos da fundação e commit local descritivo com identidade de automação limitada à chamada. Nenhum commit foi atribuído ao proprietário por dedução do perfil GitHub.

Resultado da verificação documental: **102 checagens passaram**, incluindo 40 links locais, 29 casos de arquivos ignorados e 4 de arquivos que devem permanecer versionáveis. Nenhum indicador comum de segredo encontrado no conteúdo novo. `git fsck --connectivity-only --no-dangling` terminou com exit 0. As checagens não são testes do aplicativo.

**Não executados:** Gradle, build/lint/Spotless, testes Android/unitários do app, instrumentação, instalação, restauração de backup, scan de dependências resolvidas/SBOM, pipeline GitHub, fork remoto, push, PR, tag ou release. A sessão 2 não foi iniciada.
