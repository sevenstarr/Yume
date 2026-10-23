<!-- Yume modification 2026-10-22: current public contribution workflow; assisted by Codex. -->
# Contribuindo com o Yume

Yume deriva do Animiru/Aniyomi. Leia [origem](docs/UPSTREAM.md), [arquitetura](docs/ARCHITECTURE.md), [código de conduta](CODE_OF_CONDUCT.md) e [créditos originais](docs/upstream/CONTRIBUTING.md). Problemas específicos do fork pertencem ao [repositório Yume](https://github.com/sevenstarr/Yume), sem encaminhamento automático ao suporte upstream. Vulnerabilidades seguem [SECURITY](SECURITY.md).

## Preparar e delimitar a mudança

Use o [guia público de desenvolvimento](docs/DEVELOPMENT.md) para JDK/SDK, clone, comandos e saídas. Não são necessários prompt privado, conta de tracker ou secrets para testes mockados/debug. Confira o [progresso](docs/PROGRESS.md): trabalho em branch/PR pode ainda não estar integrado.

Antes de implementar, leia [AGENTS.md](AGENTS.md), documentação relevante, estado Git e últimos commits. Preserve alterações não commitadas. O protocolo do mantenedor organiza o trabalho em sessões autorizadas; ele não é necessário para um leitor compilar ou avaliar o código. Contexto local externo, quando disponível, auxilia navegação e nunca substitui as fontes versionadas.

## Branches e commits

- `yume-main`: versões estáveis/releases aprovadas; `yume-develop`: integração.
- Sessões: `feature/session-XX-descricao`; bugs isolados: `fix/descricao`.
- Execute `git fetch origin` e crie a branch diretamente de `origin/yume-develop`. Se o fetch falhar, não presuma referência atual. Não comece de outra sessão nem transporte trabalho anterior silenciosamente.
- Incorpore a integração atualizada por merge antes de abrir/atualizar PR; revalide conflitos e comportamento. PR usa base `yume-develop`.
- Não commite diretamente em integração/principal; não reescreva autores ou commits importados, não use force-push como rotina e não exclua branch com trabalho não integrado.

Use commits pequenos com `tipo: resumo` (`docs`, `feat`, `fix`, `test`, `ci`, `chore`), resumo preferencialmente até 50 caracteres e corpo com motivo/validação quando útil. Commits assistidos pelo agente usam a identidade explícita `Codex (AI assistant) <codex@invalid>`, seguindo o histórico; contribuições humanas usam a identidade real escolhida pelo autor, inclusive noreply se desejado.

## Escopo e atribuição

Preserve biblioteca, episódios, fontes, histórico, progresso, dados originais e capas manuais. O enriquecimento é opt-in, separado de tracking/fontes; ambiguidades exigem revisão. Mudanças nesses contratos precisam de justificativa e testes. Decisões estruturais relevantes exigem ADR e revisão do mantenedor.

Conserve copyright, licenças e marcadores/créditos herdados. O [guia upstream](docs/upstream/CONTRIBUTING.md) pede delimitação `AM (<título>) --> ... <-- AM (<título>)` para alterações de código; adapte ao formato do arquivo e registre data/origem. Novas contribuições Yume e assistência por IA devem ficar identificáveis sem atribuir trabalho herdado como criação própria. Não afirme revisão humana, teste ou autoria que não possa demonstrar.

Não inclua tokens, keystores, credenciais OAuth privadas, backups/bancos pessoais ou logs/capturas sem revisão. Screenshots precisam ser reais, com commit/versão, contexto, dados sintéticos e material permitido; rotule capturas históricas. Sem artes de obras incorporadas como decoração do portfólio.

## Validação e revisão

Execute checks apropriados conforme [DEVELOPMENT](docs/DEVELOPMENT.md): Spotless, testes JVM, SQLDelight, scripts de migração, lint e debug para alterações funcionais. Banco/backup/identidade/player também exigem regressão correspondente. Mudanças apenas documentais exigem revisão de links, comandos, atribuição e diff; não alegue um build novo usando resultado histórico.

Para UI, inclua evidência sanitizada e confira temas, fonte ampliada, TalkBack/foco e telas relevantes. Testes JVM não substituem aparelho. Registre falhas, tarefas reutilizadas por cache e verificações não executadas. `spotlessApply` modifica arquivos: revise antes de incluir no commit.

O PR deve explicar problema e comportamento final, escopo, evidências, riscos e critérios de aceite. Use o [template](.github/PULL_REQUEST_TEMPLATE.md). Ao encerrar uma sessão, atualize progresso, revise diff integral e crie commit exclusivo, conforme AGENTS.

Envio por automação, abertura de PR, merge, configuração GitHub, tags e publicação exigem autorização explícita do proprietário. Implementação/commit local não autoriza integração. Não inicie a sessão seguinte sem autorização. Assinatura e distribuição seguem [BUILD_DISTRIBUTION](docs/BUILD_DISTRIBUTION.md); nunca reutilize chave/canal upstream.
