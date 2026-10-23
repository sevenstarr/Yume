# Configurações e escolha manual — Sessão 8

**Leitura atualizada na sessão 12:** este é o registro da sessão 8. As menções
a lote/governor como futuros foram superadas pela [sessão 9](background-sync.md)
e o texto de dados móveis foi corrigido em `25d50a506`. Para comportamento
consolidado consulte a [arquitetura atual](../ARCHITECTURE.md) e o
[README](../../README.md). A validação Android integral continua pendente.

20/10/2026. Implementação nova assistida pelo Codex, na branch `feat/session-8-metadata-controls`, sobre `d8a60d7c9`. Nenhuma alteração anterior não commitada encontrada. [ADR 0007](../DECISIONS/0007-manual-metadata-review.md).

## Uso

1. Abra **Configurações → Metadados externos**. O interruptor começa desligado, usa o controle SQL existente e explica o envio de consultas públicas ao AniList e o carregamento de capas externas. A ativação não percorre a biblioteca.
2. Nos detalhes de um anime persistido, abra **⋮ → Metadados externos**. A ação está nos layouts de telefone e tablet, independentemente de login no tracker.
3. Pesquise um título editável, um ID positivo AniList ou um link oficial aceito. A busca ocorre somente por ação explícita. Páginas adicionais exigem clique e terminam no limite local de 20 páginas; não há carregamento infinito ou busca ao rolar.
4. Cada candidato mostra títulos retornados, ano/formato quando conhecidos, ID e miniatura disponível. **Revisar seleção** consulta o ID pelo provider, apresenta o título da fonte e os sinais ausentes/conflitantes. A caixa de reconhecimento começa desmarcada; somente a confirmação grava o pin. A associação anterior continua durante a proposta e é preservada se o diálogo for cancelado.
5. **Atualizar metadados deste anime** consulta somente o ID aceito. Sem associação, abre uma pesquisa pelo título original; não autoriza associação textual automática. Remakes/temporadas e dados ausentes continuam exigindo revisão humana.
6. **Usar original** e **modo normal** conservam vínculo e arquivos manuais. O visualizador herdado da sessão 7 permite preview da original. **Remover associação** pede confirmação, remove o vínculo, usa original-only e bloqueia automação; outra escolha manual explícita continua possível. Apagar uma capa local permanece ação herdada separada no visualizador.

O painel informa catálogo/ID, estado da decisão, data da última resposta completa e falha sanitizada. Carregamento e resultado possuem textos e região viva; botões/checkboxes usam Material/semântica Compose. Strings em inglês e português brasileiro, com fallback dos demais idiomas. Não se apresenta score como probabilidade.

## Contratos e concorrência

`MetadataReviewActions` é um serviço suspenso de ações de primeiro plano, com provider/repository/relógio injetáveis. O ViewModel segue Metro/lifecycle existentes; IDs locais são parâmetros de navegação e o contexto vem de `GetAnime.subscribe`. Identidade usa `ogTitle`, fonte/URL/pai e temporada estruturada integral quando disponível; nunca inventa ano/formato a partir do título personalizado.

`beginReview` exige exatamente a revisão observada, inclusive ausência de registro, e permite pesquisa explícita mantendo original-only. O ticket captura revisão, generation, epoch, fingerprint, stamp da fonte e estado anterior. A lease é encerrada em `finally` não cancelável; cancelamento não vira falha de rede nem deixa o item bloqueado até expirar. Operação antiga não libera lease nova. Após morte de processo, somente nova ação manual pode iniciar outra operação quando a lease de 60 s expirar; cooldown permanece obrigatório.

Validação da proposta exige ID/tipo íntegros do provider. Confirmação exige reconhecimento, mesma revisão/epoch/stamp da origem e validação com menos de cinco minutos; mudança somente na temporada/fetch type também invalida a proposta. Cache de busca não autoriza pin sozinho. Off e clear invalidam propostas/candidatos temporários e cancelam a coroutine até o transporte; voltar das configurações não refaz miniaturas de resultados antigos. Gravações locais usam CAS; nenhuma resposta antiga restaura um vínculo removido. Pesquisa/revisão não suspendem biblioteca, player ou episódios.

O provider emitia `ANILIST`, incompatível com o namespace `anilist` exigido pelo matcher/SQL/Coil. Corrigido para o identificador canônico, com teste que leva o resultado real do mapper até a validação de seleção manual. Não existe dado válido anterior com o namespace incompatível a migrar, pois ele era recusado antes da gravação e o cliente ainda não tinha consumidor.

## Links, rede e cache

Aceitos: inteiro decimal ASCII positivo canônico até `Int.MAX_VALUE`, ou `https://anilist.co/anime/<id>` com barra opcional e slug ASCII `[A-Za-z0-9_-]+` opcional. Rejeitados outros hosts/esquemas/portas, userinfo, query, fragmento, caminhos extras/traversal, IDs codificados ou com zeros iniciais. O parser só extrai o ID; nenhuma URL digitada é aberta/buscada. A leitura remota usa a query fixa de anime do provider.

Consultas reutilizam o cache durável por fingerprint/página, sem gravar respostas parciais como negativas. Seleção e refresh validam o ID pelo provider; seu cache breve, retries/deadline/quota em processo permanecem os da sessão 4. Cada chamada reserva conservadoramente até três emissões no ticket antes de entrar no provider. Isso é limite superior por operação, **não contagem de emissões físicas nem orçamento agregado com tracker/AniChart/AniSkip**. Essa integração, fila/retomada e fairness continuam na sessão 9. Cooldown observado é gravado no repositório e impede novas ações durante o prazo, inclusive após reinício.

Miniaturas usam modelo próprio `ExternalCover` e transporte CDN neutro já validado; jamais cliente/headers da fonte ou OAuth. Somente `medium` retornado e URL permitida. São previews efêmeros, sem escrita em disk/memory cache; cancelados ao sair da composição/desligar. Não criam arquivos órfãos fora da limpeza seletiva; revisitar a busca pode baixar a miniatura novamente. As capas aceitas continuam com o cache e barreira da sessão 7.

## Configurações e recuperação

**Limpar cache** explica e remove somente detalhes/pesquisas/assets externos; conserva pins e decisões. **Limpar todos os dados enriquecidos** desliga e apaga vínculos/rejeições/pendências/cache; conserva biblioteca, fontes, episódios, progresso e arquivos manuais. Ambas as ações confirmam seu alcance, usam as barreiras existentes e não iniciam nova varredura.

Preferência de dados móveis para lote persistida no PreferenceStore, default off. O texto informa que lote ainda não está disponível e que ações manuais usam a conexão atual. Não há worker novo, agendamento ou gatilho automático; o `MetadataUpdateJob` herdado continua independente.

As pendências de backup são apresentadas nas configurações com origem/ID/fonte/item e instrução de revisão manual na biblioteca. Nenhuma pendência é aplicada a um anime arbitrário nem sobrepõe decisão do destino; a lista retida pode ser removida pela limpeza total. Remapeamento assistido individual/dispensa de uma linha permanece melhoria futura; não há promessa de resolução automática.

## Validação e gates

Testes de parser cobrem formas oficiais e entradas hostis; testes JDBC executam o serviço real com provider fake, correção de falso positivo a partir de original-only, pin preservado na pesquisa, confirmação/cancelamento/revisão/epoch/TTL, refresh exato, rejeição e cooldown. [Comandos e resultados](../evidence/SESSION_8_COMMANDS.md).

ADB/dispositivo, UI Compose/TalkBack/foco, screenshots reais, rede/CDN/decoder Android e benchmark continuam exigindo evidência própria. Testes JVM e build não demonstram que o usuário concluiu o fluxo num aparelho. **Aceite integral visual/acessível da sessão 8 permanece pendente até esse teste**. Automação textual off, gates de distribuição/termos/identidade preservados, sem publicação. Sessão 9 depende de autorização explícita.
