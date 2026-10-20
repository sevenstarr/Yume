# Especificação funcional — Sessão 3

**Atualização da sessão 8 — 20/10/2026:** [configurações e revisão manual](manual-review.md) implementadas com opt-in inicialmente off, busca/ID/link/páginas, proposta confirmada, refresh, rejeição, reversão/remoção e limpeza. [ADR 0007](../DECISIONS/0007-manual-metadata-review.md). Sem worker/lote ou automação textual; UI/TalkBack em Android e orçamento agregado continuam pendentes.

**Atualização da sessão 7 — 20/10/2026:** [resolver/Coil e consumidores](cover-resolution.md) implementados conforme [ADR 0006](../DECISIONS/0006-cover-resolution.md), incluindo reversão de original no visualizador, mesmo sem associação prévia/opt-in. Testes de contrato/JVM e DiskCache real com bytes sintéticos; decoder/UI/performance em Android permanecem pendentes. Opt-in e automação textual off. Os registros de sessões anteriores abaixo são históricos.

**Atualização da sessão 6 — 20/10/2026:** persistência/cache/backup implementados no [contrato da sessão 6](persistence.md) e [ADR 0005](../DECISIONS/0005-metadata-persistence.md). Requests Coil/coordenador/UI ainda não são consumidores ativos; testes de Android real e gates de automação continuam pendentes. O corpo abaixo preserva a especificação original.

Data: **19/10/2026**, calendário `America/Sao_Paulo`. Produzido com assistência do Codex/IA.

**Entrega: especificação funcional da Sessão 3 para revisão e implementação posterior.** A arquitetura da Etapa B foi aprovada pelo proprietário na instrução que solicita este documento. Essa aprovação é registrada no [ADR 0002](../DECISIONS/0002-metadata-enrichment-architecture.md); não significa que esta especificação já tenha sido revisada, implementada ou testada em Android.

Base documental: [Etapa A — arquitetura existente](current-architecture.md), commit `4fdc01724`; [Etapa B — arquitetura de enriquecimento](proposed-architecture.md), commit `cb86e7ff7471b4d2c0bdbc952b51b6ba6d722bd0`. A Etapa B define a direção; este documento detalha seus comportamentos observáveis e contratos lógicos. O código atual continua representando o comportamento herdado. Divergências novas devem ser resolvidas explicitamente, sem ampliar o MVP por inferência.

## Navegação

- [1. Escopo e vocabulário](#1-escopo-e-vocabulário)
- [2. Requisitos e rastreabilidade](#2-requisitos-e-rastreabilidade)
- [3. Estados e comandos do usuário](#3-estados-e-comandos-do-usuário)
- [4. Contratos de interfaces](#4-contratos-de-interfaces)
- [5. Modelos de dados](#5-modelos-de-dados)
- [6. Regras de matching e identidade](#6-regras-de-matching-e-identidade)
- [7. Capas, cache e concorrência](#7-capas-cache-e-concorrência)
- [8. Fluxos de usuário](#8-fluxos-de-usuário)
- [9. Critérios de aceitação](#9-critérios-de-aceitação)
- [10. Casos extremos e falhas](#10-casos-extremos-e-falhas)
- [11. Testes planejados](#11-testes-planejados)
- [12. Tarefas das sessões seguintes](#12-tarefas-das-sessões-seguintes)
- [13. Decisões e definições pendentes](#13-decisões-e-definições-pendentes)
- [14. Verificação desta entrega](#14-verificação-desta-entrega)

## 1. Escopo e vocabulário

O usuário poderá identificar um anime local no AniList e usar a capa desse registro sem alterar dados da fonte, personalizações, biblioteca, temporadas, episódios ou progresso. Títulos alternativos, datas e formato ajudam a identificar e revisar a escolha; não substituem automaticamente os detalhes exibidos da obra.

O MVP usa AniList público, Kotlin/coroutines/Flow, SQLDelight, OkHttp, Kotlin Serialization, Metro, Compose, Coil e WorkManager nos módulos existentes. Não inclui backend, segundo catálogo, autenticação obrigatória, tradução generativa, catálogo espelhado, edição automática de sinopse/gêneros/status/título, banners ou thumbnails de episódios. Discord RPC e Cast que recebem URL diretamente continuam com a original; preview do tracker continua independente.

| Termo | Significado funcional |
| --- | --- |
| Original | Capa atual da fonte ou seu cache herdado; não promete recuperar um arquivo antigo indisponível |
| Manual local | Arquivo de capa personalizada já escolhido pelo usuário no Animiru |
| Associação | Decisão durável que liga um `Anime.id` a `(provider, remoteId)`; no máximo uma ativa por anime no MVP |
| Pin | Fixação de uma associação escolhida manualmente; renovação não muda seu ID |
| Snapshot | Detalhes remotos descartáveis; expirar não apaga a associação |
| Revisão / generation / epoch | Versões para, respectivamente, observar dados, invalidar tentativas do item e invalidar trabalhos globais |
| Completa | Pesquisa terminada, sem páginas restantes, erros relevantes ou interrupção por orçamento; não equivale a certeza de identidade |
| Default de projeto | Valor operacional herdado da Etapa B, sujeito aos gates indicados; não é limite garantido pelo provedor nem medição de desempenho |

Os rótulos de ações neste documento descrevem semântica, não texto final de interface. Nomes de novos tipos/métodos/tabelas são contratos lógicos a materializar nas sessões próprias, sem pressupor que já existam. Os IDs `RF`, `RNF`, `IF`, `FU`, `CA`, `T`, `S` e `P` permitem rastrear requisitos, interfaces, fluxos, aceite, testes, tarefas e pendências.

## 2. Requisitos e rastreabilidade

### 2.1 Requisitos funcionais

| ID | Comportamento obrigatório | Origem na Etapa B | Aceite |
| --- | --- | --- | --- |
| RF-01 | Começar desligado; ativação explica consultas públicas e imagens externas e não varre a biblioteca | §§1, 5, 10.1 | CA-01 |
| RF-02 | Preservar `SAnime`, dados originais, `custom_*`, `memo`, episódios, downloads, histórico e progresso; enriquecimento não aplica dados via `UpdateAnimeFromRemote` | §§1, 9 | CA-02 |
| RF-03 | Buscar por título e ler por ID com AniList público, sem OAuth, cookies ou headers da fonte e sem mutations de lista | §§2–3 | CA-03 |
| RF-04 | Associar por ID local; diferenciar fonte/URL/contexto e catálogo; não mesclar registros ou herdar vínculo entre pai e filhos | §4.1 | CA-04 |
| RF-05 | Normalizar conservadoramente; avaliar evidências, vetos, rejeições e completude; automatizar texto só após calibração validada | §§4.2–4.5 | CA-05, CA-06 |
| RF-06 | Permitir pesquisa editável, paginação explícita, ID/link oficial validado, escolha e rejeição de candidatos, exibindo diferenças e dados ausentes | §4.6 | CA-07 |
| RF-07 | Fixar escolha manual de forma atômica; refresh e troca de tracking não a substituem; correção mantém vínculo anterior até confirmação | §§4.2, 4.6 | CA-08 |
| RF-08 | Exibir vínculo, operação e saúde separadamente; falha/ausência de imagem não desfaz identidade | §5 | CA-09 |
| RF-09 | Resolver capa localmente com precedência manual local → externa aceita → original → placeholder; modo original ignora todas as personalizações | §§8.1–8.2 | CA-10 |
| RF-10 | Aplicar fallback finito em erro de fetch ou decode, sem procurar outra obra/provedor; preservar arquivos manuais | §8.2 | CA-11 |
| RF-11 | Desligar cancela enriquecimento e uso externo; distinguir preview original, política original, remoção de vínculo e remoção manual local | §5 | CA-12 |
| RF-12 | Separar limpar cache de limpar tudo; limpeza seletiva preserva dados herdados e impede repopulação por resposta antiga | §§5–8 | CA-13 |
| RF-13 | Persistir decisões separadas do cache; oferecer observação em lote e gravações condicionais contra concorrência/reinício | §§6–8 | CA-14 |
| RF-14 | Migrar de forma aditiva e transacional, sem backfill por título ou tracking; falha não destrói o banco | §6.3 | CA-15 |
| RF-15 | Exportar opcionalmente decisões locais; restaurar sem rede nem ativação por backup, com remapeamento/conflitos explícitos | §6.4 | CA-16 |
| RF-16 | Usar cache com retenção limitada; offline usa apenas dados/bytes elegíveis ou original; falha não renova TTL | §7 | CA-17 |
| RF-17 | Atualizar capas visíveis após commit, cobrindo `Anime`, `AnimeCover`, requests diretos e consumidores imperativos previstos | §§8.3, 9 | CA-18 |
| RF-18 | Sincronizar por ação explícita primeiro; gatilhos automáticos/lote só quando habilitados, com fila própria e retomada | §10.1 | CA-19 |
| RF-19 | Compartilhar orçamento AniList entre consumidores; respeitar cooldown, prioridades, retries finitos e cancelamento | §§10.2–10.3 | CA-20 |
| RF-20 | Informar origem, data e falha acionável sem expor informação privada em logs; oferecer UI localizada e acessível | §§4.6, 5, 10.3 | CA-21 |

### 2.2 Requisitos não funcionais

| ID | Restrição verificável | Aceite |
| --- | --- | --- |
| RNF-01 | Abrir biblioteca, episódios e player não aguarda sucesso externo; renderização não pesquisa GraphQL | CA-01, CA-18, CA-19 |
| RNF-02 | Keyer síncrono sem I/O; listas usam índice/Flow compartilhado e consultas em lote, sem SQL ou rede por frame | CA-18 |
| RNF-03 | Rede fora de transação SQL e da thread principal; cancelamento se propaga até a chamada e interrompe fallback | CA-14, CA-20 |
| RNF-04 | URLs de imagem externas HTTPS, sem credenciais, hosts permitidos e redirects validados; cliente da original mantém transporte próprio | CA-03, CA-11 |
| RNF-05 | Nenhuma telemetria remota nova; logs de produção sem token, headers privados, URL da fonte, título da biblioteca ou corpo GraphQL bruto | CA-21 |
| RNF-06 | Testes reproduzíveis com relógio/transporte falsos e fixtures; calibração separada da validação | CA-05, CA-20 |
| RNF-07 | Migração/restore e regressão com comparação dos dados herdados; saída zero de Gradle isoladamente não prova preservação | CA-02, CA-15, CA-16 |
| RNF-08 | Medir scroll, memória, consultas e rede contra baseline no mesmo dispositivo; limites numéricos de regressão continuam em P-08 | CA-22 |

## 3. Estados e comandos do usuário

### 3.1 Estado composto

O estado observado contém `association?`, estado da tentativa, disponibilidade do provedor, saúde da imagem, modo visual e próxima tentativa quando aplicável. A prioridade de apresentação não apaga dimensões persistidas.

| Estado exibido | Condição | Saída ou recuperação |
| --- | --- | --- |
| `DISABLED` — desativado | Global/provedor off; encobre vínculos conservados | Ativar permite novas ações elegíveis; não enfileira lote |
| `PENDING` — pendente | Aguardando execução, rede, quota ou lease retomável | Iniciar quando elegível, cancelar ou permanecer com `not_before` |
| `SEARCHING` — buscando | Há tentativa com generation/lease vigente | Resultado, cancelamento ou recuperação após lease expirada |
| `ASSOCIATED` — associado | ID aceito, mesmo sem imagem ou snapshot | Renovar mesmo ID, corrigir explicitamente ou remover |
| `AMBIGUOUS` — ambíguo | Empate, contexto insuficiente ou candidato plausível sem decisão segura | Revisão manual; nenhuma nova capa aplicada automaticamente |
| `NOT_FOUND` — não encontrado | Busca válida e completa sem candidato elegível | Cache negativo; editar consulta ou tentar novamente explicitamente |
| `TRANSIENT_ERROR` — erro transitório | Rede, timeout, 429 ou falha temporária | Manter vínculo, mostrar próxima tentativa e respeitar cooldown |
| `UNAVAILABLE` — indisponível | ID removido, acesso negado, contrato incompatível ou provedor suspenso | Preservar decisão; original e revisão/recuperação explícita |

Evidência insuficiente usa motivo `INSUFFICIENT_EVIDENCE` e revisão, não um negativo fabricado. Pesquisa truncada/erro parcial não produz `NOT_FOUND`. Com vínculo prévio, exibir, por exemplo, “Associado · atualização falhou”; saúde da imagem distingue `NO_IMAGE`, `HTTP_ERROR`, `DECODE_ERROR` e `UNSAFE_URL`. Exibir origem e última verificação válida, distinguindo-a da última tentativa.

Transição habitual: pendente → buscando → associado/ambíguo/não encontrado/erro. Cancelar volta ao estado anterior ou pendente, sem erro de provedor. No reinício, lease vencida torna trabalho retomável sem zerar contadores. Mudança de identidade exige revisão da associação automática; pin manual conserva ID e informa conflito.

### 3.2 Efeito dos comandos

| Comando | Decisão/cache | Rede e efeito visual |
| --- | --- | --- |
| Ativar | Persiste opt-in | Permite ações/gatilhos habilitados; sem busca em massa |
| Desligar | Persiste off e avança epoch; conserva vínculos/pins | Cancela fila/chamadas do enriquecimento; manual local → original; nenhuma nova imagem externa |
| Ver original | Preview transitório; sem gravação | Bypass externo e manual local; fechar preview retoma seleção vigente |
| Usar original neste anime | Persiste `ORIGINAL_ONLY`, conserva vínculo/pin | Cancela tentativa do item e impede reaplicação externa até ação explícita |
| Voltar ao modo normal | Remove política `ORIGINAL_ONLY` por ação explícita | Volta à precedência normal, conservando as demais restrições |
| Atualizar metadados | Renova detalhes do mesmo ID aceito; sem vínculo, executa resolução elegível | Pode ignorar TTL/negativo; nunca cooldown, rejeições, pin ou guardas |
| Corrigir associação | Busca uma proposta, sem trocar vínculo em vigor | Só confirmação válida substitui o ID |
| Rejeitar candidato | Persiste rejeição e invalida tentativa | Não volta à seleção automática; reapresentação somente por ação manual |
| Remover associação | Apaga vínculo e bloqueia reassociação automática no item | Mantém manual local e original; não apaga arquivo do usuário |
| Permitir associação automática novamente | Remove bloqueio do item | Automação ainda depende de opt-in, gatilhos e calibração |
| Remover capa personalizada | Ação herdada separada | Só a ação explícita correspondente remove a manual local |
| Limpar cache | Avança epoch e remove snapshots, buscas, negativos e assets externos | Conserva decisões/pins/bloqueios; sem refetch em massa |
| Limpar todos os dados enriquecidos | Primeiro off + epoch; apaga vínculos, rejeições, fila e cache externo | Exige novo opt-in; mantém biblioteca, originais e manuais; cooldown do host sobrevive |

As ações que descartam decisões ou cache informam seu alcance antes da execução. O desenho exato de diálogo e strings será definido na Sessão 8; este documento não acrescenta um fluxo de aprovação externa.

## 4. Contratos de interfaces

### 4.1 Convenções comuns

Estes contratos descrevem entradas, saídas e efeitos, não assinaturas Kotlin compiláveis. Usar operações suspensas para I/O e Flow para observação. Identificadores de catálogo são texto opaco namespaced; o adapter AniList valida inteiro positivo representável pelo contrato remoto. ID local é `Long` de anime persistido. Campo ausente é desconhecido, nunca concordância implícita.

Uma leitura retorna sucesso íntegro, sucesso parcial com campos afetados/completude, ausência válida ou `MetadataFailure`. Uma escrita retorna aplicado com nova revisão, conflito/resultado obsoleto, inelegível/inexistente ou falha local. Não tratar falha SQL como sucesso em memória. `CancellationException` se propaga, fora dos erros de negócio. Mensagens visuais são derivadas de códigos sanitizados e traduzíveis.

### 4.2 IF-01 — `MetadataProvider` e `AniListProvider`

| Operação | Entrada | Saída e garantias |
| --- | --- | --- |
| `capabilities` | Identidade do adapter | Tipos/formatos consultáveis, suporte a filtros/IDs e política de imagens; não ativa outros catálogos |
| `search` | Consulta não vazia, filtros confiáveis, página positiva, contexto de cancelamento/orçamento | Página de candidatos, paginação, completude e erros por campo; sem persistir vínculo |
| `getById` | `(provider, remoteId)` validado | Registro, parcial explícito, ausência íntegra ou falha; nunca outro ID como substituto |

O adapter público envia `POST` ao endpoint definido na Etapa B, `https://graphql.anilist.co`, com queries fixas e variáveis separadas. Pesquisa `Page` com `perPage: 20`, `media(..., type: ANIME)` e `pageInfo`; leitura exata `Media(id, type: ANIME)`. Um `idMal` conhecido exige argumento/conversão explícita, não troca de namespace. Link digitado é analisado como link oficial para extrair um ID e validá-lo via provider; não é uma URL arbitrária a buscar. A gramática aceita de links permanece P-03.

DTO mínimo: `id`, `idMal`, `type`, `title { romaji english native }`, `synonyms`, `format`, `startDate`, `season`, `seasonYear`, `episodes`, `siteUrl`, `coverImage { extraLarge large medium }`; relações apenas nos detalhes necessários à desambiguação. Falha de identidade/lista/títulos compromete completude; falha só de capa preserva identidade e dados íntegros. Não sobrescrever campo bom com nulo causado por erro. `Media = null` só indica indisponibilidade quando a resposta correspondente é íntegra. Enum novo é desconhecido; JSON incompatível gera falha de contrato.

### 4.3 IF-02 — `AnimeMatcher`

Entrada: `LocalAnimeIdentity`, conjunto deduplicado de candidatos, completude de todas as consultas, rejeições, versão de normalização e configuração calibrada, quando existente. Saída: `MatchDecision` com candidatos ordenados, evidências concordantes/ausentes/conflitantes, vetos, classe de confiança, motivo e versões.

Resultados lógicos: elegível para associação automática; revisão (`AMBIGUOUS`/`INSUFFICIENT_EVIDENCE`); busca completa sem elegíveis; pesquisa incompleta. O mesmo input/configuração deve produzir a mesma decisão. Não faz rede, SQL, tradução ou geração de texto para decidir identidade. Sem configuração validada, nenhum resultado textual autoriza `AUTO_MATCH`, independentemente de score.

### 4.4 IF-03 — `MetadataRepository`

| Operação lógica | Entrada | Efeito/saída |
| --- | --- | --- |
| `observe` / `observeBatch` | ID ou conjunto de IDs locais | Flow de snapshots/revisões; ausência explícita; somente dados locais |
| Ler cache | Chave de busca ou `(provider, id)` e instante | Payload, frescor, completude e revisão; leitura não renova datas |
| Iniciar tentativa | Anime, motivo, fingerprint e política vigentes | Ticket com revisão, generation, epoch, lease e orçamento durável |
| Confirmar decisão | Ticket, identidade remota validada, evidências, origem e pin | Transação condicional de vínculo/estado/revisão; conflito descarta resposta |
| Renovar detalhes | Mesmo ID aceito, snapshot e ticket vigente | Atualiza cache/última verificação; não troca ID ou pin |
| Rejeitar / mudar modo / remover / liberar automação | ID, comando e revisão esperada | Atualiza decisão local, avança versões, invalida trabalho do item |
| Off / limpar cache / limpar tudo | Comando global e estado vigente | Avança epoch antes de invalidar/remover; retorna somente após efeitos locais previstos |
| Concluir/falhar tentativa | Ticket vigente e resultado tipado | Registra tentativa/falha sem apagar associação válida |

Toda confirmação verifica atomicamente: anime ainda existe, opt-in/política permitem a ação, revisão/generation/epoch/fingerprint permanecem iguais, lease/ticket continuam válidos e, em automação, não há pin/bloqueio/rejeição impeditivos. A confirmação manual exige validação do ID/tipo e aviso de conflito quando houver; um simples clique não autoriza gravar um registro remoto ainda não validado.

Vínculo, pin, estado e revisão são publicados juntos após commit. Rede fica fora da transação. Escrita de cache derivada de tentativa antiga também é recusada; um cache compartilhado já válido não autoriza recriar associação removida. Widgets/UI só recebem notificação de mudança confirmada.

### 4.5 IF-04 — `MetadataSyncCoordinator`

Entrada: ID local, motivo (ação explícita, gatilho habilitado, renovação, lote ou revalidação de imagem), política de rede e ticket. Saída observável: pendente/buscando e resultado associado/revisão/ausência/adiado/falha. Resolução encerrada por política informa o motivo, sem chamada externa.

Ordem: ler elegibilidade → reutilizar cache válido/vínculo → obter ID confiável ou pesquisar → avaliar → confirmar condicionalmente. Deduplicar por item/provedor no processo e na fila durável. Um pedido de correção produz proposta; uma renovação não produz rematching. Revalidação após falha de imagem é agendada/deduplicada e não bloqueia o request visual.

Após escolha manual, rejeição, modo original, remoção, off ou clear, cancelar/inutilizar tickets anteriores. Retomada não zera tentativas nem ignora cooldown. Caminhos de UI, worker e ações repetidas passam pela mesma política.

### 4.6 IF-05 — `CoverResolver` e IF-06 — integração Coil

`CoverResolver` recebe snapshot local de `Anime`/`AnimeCover`, política global/individual, referência manual, associação, snapshot remoto e disponibilidade de cache. Retorna `ResolvedCover` imutável com tentativas ordenadas e revisões; não pesquisa nem baixa imagens. Observação/índice em lote alimenta o mesmo contrato nos consumidores Compose e imperativos.

O interceptor consome esse plano antes do keying, preservando tamanho, transformação, cache policy, lifecycle e target. Cada tentativa tem modelo/chave/transporte próprios. O retorno da cadeia inclui decode, permitindo fallback em bytes inválidos. Bypass e limite finito evitam recursão. Target recebe apenas resultado final; cancelamento não vira fallback. Resultado de revisão antiga não é publicado como seleção nova.

Keyer usa apenas o plano, sem rede/banco. Fetcher externo usa cliente neutro e valida URL/redirect; original mantém headers/cliente da fonte e caminhos de arquivo/cache herdados. `useBackground(true)` e pedidos de episódios ficam fora da adaptação. Requests com chaves explícitas devem ser reconstruídos para respeitar a revisão/modo.

### 4.7 IF-07 — orçamento compartilhado e IF-08 — worker

Orçamento singleton por host atende provider, tracker, AniChart e conversão AniSkip. Solicitação informa classe de prioridade e recebe permissão ou adiamento com `not_before`; conclusão registra headers/cooldown/falha. Tracking/player têm prioridade sobre lote com fairness da fila; não cancelar mutation já enviada nem alterar retry do tracker para enriquecer. Imagens CDN têm concorrência própria, separada de GraphQL.

O worker usa trabalho único por provedor e fila SQL por `(animeId, provider)`, respeitando rede/restrições, lease e cursor. Retomada conserva resultados concluídos e budgets; pausa/cancelamento invalida leases conforme motivo. Resumo de lote contém sucessos, ambiguidades, falhas, ignorados e próximos retries, sem marcar pendente como sucesso. Não substituir `LibraryUpdateJob` nem o `MetadataUpdateJob` herdado.

### 4.8 IF-09 — backup/restauração

Exportação recebe a opção de incluir decisões locais e produz bloco opcional versionado sem cache/URLs/imagens externas, fila, erros ou credenciais. Importação recebe bloco e mapeamento de animes/relações já restaurados; devolve aplicado, inativo, conflito ou pendente de remapeamento. Não faz rede e não ativa o recurso por causa do backup.

Resolver por fonte/URL exata/contexto; nunca usar `BackupAnime.id` como chave universal. Duplicatas indistinguíveis e pins divergentes ficam para revisão. Pin do destino não é substituído. Registro restaurado conserva decisão, mas só pode exibir imagem externa após revalidação sob opt-in; provedor desconhecido fica inativo. Campo protobuf, representação dos conflitos e compatibilidade entre versões dependem de P-05, sem número reservado por esta especificação.

## 5. Modelos de dados

### 5.1 Objetos de domínio e UI

| Modelo | Campos lógicos e significado |
| --- | --- |
| `LocalAnimeIdentity` | `animeId: Long`, `sourceId: Long`, URL exata, `ogTitle`, `parentId?`, `seasonNumber?`, escopo conhecido ou desconhecido, atributos opcionais com origem/confiabilidade, fingerprint e versão. Escopo desconhecido não é presumido como obra única |
| `MetadataMedia` / candidato | `provider`, `remoteId`, `idMal?`, tipo, títulos por idioma/sinônimos, data parcial com ano/mês/dia independentes, formato/estação/ano de lançamento/episódios opcionais, relações necessárias, link público e variantes; desconhecidos preservados como tais |
| `CoverVariant` | Qualidade nominal (`extraLarge`, `large`, `medium`), URL validada, dimensões observadas opcionais; sem pixels inferidos do nome |
| `MatchDecision` | Resultado, classe de confiança, ranking/score não probabilístico, sinais/vetos/motivos, completude e versões; rejeições participam da decisão |
| `MetadataAssociation` | ID local e snapshot de identidade, provedor/ID aceito, título selecionado como rótulo, origem `MANUAL`/`VERIFIED_TRACKER_ID`/`AUTO_MATCH`, pin, preferência de variante?, evidências, datas, fingerprint e revisão |
| `AttemptTicket` | ID/provedor, revisão esperada, generation, epoch, fingerprint, identificação/validade de lease e contadores de orçamento; não é prova de escolha manual |
| `MetadataFailure` | Categoria, provedor/operação, código HTTP/GraphQL opcional, retryable, instante e próxima tentativa?, mensagem sanitizada; falha local separada de provedor/imagem |
| `ResolvedCover` | ID/fonte, referência/revisão original e manual, provedor/ID?, variantes, revisões de associação/cache/política, epoch, modo `NORMAL`/`ORIGINAL_ONLY`, lista de tentativas e motivo de fallback |
| Estado de tela | Vínculo?, operação, disponibilidade, saúde de imagem, candidatos/evidências/completude, origem/data/falha, comandos elegíveis e versão observada; sem DTO GraphQL exposto |

Não enviar URL de extensão, `memo`, notas ou biblioteca completa ao provider. `customTitle` só serve de consulta se o usuário o escolher explicitamente. Ano/formato ausentes em `SAnime` não são criados a partir de suposições. Um total incompleto de episódios da extensão não se converte em contagem oficial.

### 5.2 Persistência lógica SQLDelight

Tabelas novas ficam no banco existente `tachiyomi.animedb`. Datas desta camada são instantes UTC em milissegundos; headers em segundos são convertidos na borda. `last_attempt_at` marca tentativa; `last_checked_at` avança apenas com verificação íntegra. Revisões/generation/epoch são inteiros monotônicos duráveis. O DDL final, adapters e limites de payload serão definidos na Sessão 6.

| Entidade | Chave, campos e nulabilidade lógica | Integridade/ciclo de vida |
| --- | --- | --- |
| `metadata_control` | Singleton; enabled, cleanup_epoch, versão da política, flags de automação, cooldown/circuito do host | Defaults off; fonte transacional para opt-in/epoch. Limpeza total mantém off, epoch e proteção operacional de quota |
| `anime_metadata` | PK/FK `anime_id`; source_id/URL/contexto/fingerprint; provider?/remote_id?; selected_title?, origem?, pin; variante preferida?; modo/bloqueio; evidências/versões; row_revision/generation; associated_at?/last_checked_at?/last_attempt_at?; estado de tentativa/falha? | Provider/ID presentes ou ausentes juntos; pin implica vínculo completo. Sem vínculo, a linha pode guardar política/bloqueio. FK local com cascade; sem unicidade global do remoto |
| `metadata_media_cache` | PK `(provider, remote_id)`; dados públicos íntegros do modelo, URLs/qualidade, fetched_at/expires_at, revisão de conteúdo e versão DTO | Compartilhado por várias associações. Não é FK obrigatória do vínculo; sua remoção não apaga decisão |
| `metadata_lookup_cache` | Chave de provedor/consulta/locale/filtros/página/versões; candidatos resumidos/completude/resultado/expiração. Negativo inclui fingerprint local | Descartável, limitado; sem respostas GraphQL brutas nem negativos gerados por erro |
| `metadata_rejection` | PK `(anime_id, provider, remote_id)`; razão/data | FK anime com cascade; permanece até reversão explícita ou limpeza total |
| `metadata_work` | Unique `(anime_id, provider)`; motivo/prioridade, generation/epoch/fingerprint, status/lease, tentativas/budget, not_before; controle de retomada | FK anime com cascade; uma pendência por item/provedor, não por tela/capa |
| `metadata_image_index` | Chave externa Coil; provider/remote_id, hash URL/revisão, acesso/expiração, falha/bloqueio temporário | Índice de bytes no Coil, sem arquivo duplicado. Escrita respeita epoch; remoção seletiva não alcança assets herdados |

Índices previstos: `anime_id` para leitura em lote, `(provider, remote_id)` para consumidores de snapshot, `(status, not_before)` para fila e expiração para caches. Nenhuma constraint nova `UNIQUE(source, url)` em `animes`. Evidências serializadas têm versão e limite ainda a definir (P-04). Datas remotas parciais não são convertidas em datas completas artificiais.

### 5.3 Migração e transporte de decisões

Criar novas tabelas/índices em migração aditiva transacional, após reconfirmar última numeração. Nesta base a última é `147.sqm`; não editar essa migração nem reservar 148 sem conferir novamente. Sem atualização dos registros herdados, varredura por título ou importação automática de tracking; tabelas vazias e opt-in off após upgrade.

Testar banco novo e upgrade de snapshots válidos com FKs. Falha exige rollback/erro recuperável, sem apagar/recriar banco. Off/limpeza são reversão funcional; não se garante downgrade para APK antigo. Futuro descarte de tabelas requer migração própria.

Payload lógico de backup: versão, fonte/URL/contexto restaurável, provedor/ID?, origem/pin?, modo/bloqueio e rejeições. Não transportar ID local como identidade universal, título de catálogo/URLs/snapshot, bytes ou estado operacional. Backup antigo sem bloco segue restauração herdada. Novo bloco só será considerado compatível com leitor antigo após fixture de leitura que ignore o campo desconhecido. A suspeita herdada de perda de `custom*` e a ausência de bytes de capa manual no backup têm investigação separada em S6-04.

## 6. Regras de matching e identidade

1. Ler opt-in, modo, bloqueio, rejeições e vínculo existente. `ORIGINAL_ONLY` impede resolução externa para aquele item. Vínculo aceito renova somente seu ID; TTL não o invalida.
2. Sem vínculo seguro, considerar ID do tracker do mesmo `animeId` somente com origem humana demonstrável, tipo/ID verificados e sem conflito explícito. Se a origem não puder ser provada, exibir como candidato. Tracking nunca é escrito pelo enriquecimento.
3. Sem ID confiável, pesquisar título normalizado conservadoramente. Fonte/URL/contexto ficam locais. Um par fonte/URL duplicado não permite unir animes nem selecionar o primeiro durante restore.
4. Preservar texto bruto; NFC, caixa independente de locale, espaços/pontuação normalizados sem remover tokens. Variante NFKC é auxiliar. Remover apenas sufixos de distribuição ancorados/conhecidos, por regras versionadas; não apagar parênteses indiscriminadamente.
5. Conservar números, anos, `II`, `Part 2`, `2nd Season`, `Final`, `Movie`, `OVA`, `Special` e subtítulos. Numeral só vira marcador de temporada em contexto reconhecido. Transliteração/remoção de acentos são auxiliares, não prova isolada.
6. Executar no máximo duas variantes determinísticas e duas páginas de 20 por variante: até quatro buscas, 80 candidatos antes de deduplicar por ID e uma leitura de detalhes adicional. Teto de cinco operações lógicas na resolução textual automática; caminho por ID usa uma. UI pode pedir continuação explicitamente; isso não transforma a pesquisa automática truncada em completa retroativamente sem nova avaliação.
7. Comparar títulos e contexto confiável de ano/formato/edição. Formato confirmado incompatível, ano confiável divergente ou parte/temporada explícita diferente vetam automação. Dados ausentes não concordam; não existe tolerância universal de ±1 ano.
8. Coleção/franquia não recebe obra filha automaticamente. Temporada local não recebe ID do pai. `season` AniList é estação de lançamento, não número narrativo. Remake/recap/cour/OVA, título curto/sigla ou empate exigem evidência independente/revisão.
9. Alta confiança exige pesquisa completa, sem veto, contexto suficiente, score e margem validados. Pesos, limiares e suficiência ficam em P-01. Antes do gate, automação textual desligada. Primeiro resultado/popularidade não provam identidade.
10. Seleção manual pode superar divergência de título/ano/formato após aviso explícito, mas não tipo não anime, ID inválido, URL insegura ou falha de validação remota. Persistir pin e vínculo atomicamente. Rejeitado não volta automaticamente; permitir reexibição apenas por ação manual.

Versão nova de matcher invalida caches dependentes, sem desfazer pins ou trocar vínculos aceitos. Mudança material do título/contexto original invalida fingerprint e suspende uso de associação automática para revisão; mudança só de capa original atualiza revisão visual, sem rematching. Serialização do fingerprint e lista precisa de mudanças materiais são P-04.

## 7. Capas, cache e concorrência

### 7.1 Seleção e tentativa

Em modo normal e opt-in: manual local → variante da associação externa aceita → original → placeholder. Externa manual e externa automática são origens alternativas do único vínculo ativo. Global off: manual local → original → placeholder. `ORIGINAL_ONLY`/preview original: original → placeholder, ignorando inclusive manual local sem apagá-la.

Preferência de variante manual, quando presente, prevalece entre variantes do mesmo registro. Ordem automática `extraLarge` → `large` → `medium`, removendo URLs duplicadas; não inventar tamanho/URL. Dimensão nominal não prova superioridade sobre manual/original; não baixar ambas para medir. Lista decodifica no tamanho solicitado; visualização/salvar usa melhor variante disponível da seleção explicitamente escolhida.

Falha de manual preserva arquivo e tenta externa/original. Falha individual externa (404/410/decode/URL inválida) permite próxima URL distinta do mesmo ID, até três; falha geral de conectividade/timeout/429 do host não dispara todas as variantes em rede. Original usa bypass manual/externo para evitar repetição/recursão. Falha de original termina no placeholder herdado. Cache-only continua cache-only em todos os ramos.

404 pode agendar uma revalidação deduplicada de detalhes, fora da cadeia de imagem e sem rematching. Bloqueio por falha temporária de URL/revisão: 15 minutos; por 404/410/decode: até 24 horas ou nova URL/revisão. Cancelar interrompe a cadeia sem iniciar próxima tentativa.

### 7.2 Chaves e retenção

| Camada | Default da Etapa B | Invalidação/limite |
| --- | --- | --- |
| Índice de associação em memória | IDs observados e Flow compartilhado | Revisão/política/epoch/identidade; sem cache ilimitado de biblioteca |
| Detalhes | Fresco por 7 dias; uso vencido limitado a 30 dias da última resposta íntegra | Após 30 dias remover snapshot e usar original; conservar associação |
| Pesquisa positiva/ambígua | 24 horas; LRU de até 200 entradas de páginas | Consulta/filtros/locale/página/versões e clear |
| Negativo | 24 horas e fingerprint local | Só busca completa válida; muda com identidade/versão ou retry explícito |
| Imagem externa | Coil existente; retenção máxima inicial de 30 dias e eviction normal por capacidade | Nova revisão/URL, expiração, decode e limpeza seletiva |
| Falha temporária | Backoff/cooldown, sem negativo | Sucesso ou prazo/rede/ação elegível, sem furar cooldown |

Não renovar `fetched_at` lendo cache nem prolongar retenção após falha. Prune diário durante atividade/trabalho autorizado e também no acesso; preservar apenas detalhes associados/páginas recentes. Offline não inicia pesquisa; usar bytes externos elegíveis já disponíveis, senão original. Ausência de snapshot após clear não apaga ID/pin nem provoca refetch em massa.

Chave de seleção em memória inclui ID/fonte local, origem/modo, revisões manual/original/associação/snapshot/política e epoch. Asset externo em disco: namespace da camada + provedor/ID + hash URL + revisão, sem `animeId`, permitindo compartilhar bytes entre fontes. Fallback original não é armazenado sob chave externa. Limpeza usa índice para remover só assets externos de memória/disco, incluindo chaves explícitas de requests; não limpa Coil inteiro ou `covers/custom`.

### 7.3 Ordenação e corridas

Toda tentativa captura revisão/generation/epoch/fingerprint em transação curta, faz I/O fora dela e revalida antes de gravar. Dois pedidos concorrentes deduplicam; seleção manual e comandos invalidantes avançam versões. Se cancelamento não interromper HTTP a tempo, a comparação transacional ainda rejeita o resultado antigo.

Clear/off avançam epoch antes de cancelar/remover. Escrita em cache de imagem iniciada antes da limpeza é cancelada, conferida antes da publicação e tem suas chaves removidas novamente se necessário. Não declarar clear concluído enquanto uma escrita antiga puder repor asset elegível. Remoção do anime implica cascade e impede recriação por resposta. Lease expirada permite retomada, mas geração/contadores não são reutilizados.

A implementação da barreira de escrita/limpeza Coil depende de prova na versão local (P-06). Nenhum sucesso visual é publicado antes de commit SQL. Após commit, atualizar Flow e requests visíveis; widgets recebem atualização agrupada explícita, notificações resolvem estado vigente sem pesquisar.

### 7.4 Orçamento, retry e suspensão

Todos os valores a seguir são defaults da Etapa B, não quotas atuais garantidas do serviço: teto agregado 24 chamadas/minuto, intervalo mínimo 2,5 s, suborçamento de enriquecimento 12/min e uma requisição GraphQL em voo por host inicialmente. Headers mais restritivos diminuem o teto, nunca o elevam automaticamente. Tracking/player têm prioridade sobre lote; múltiplos dispositivos/IP compartilhado ainda podem causar 429.

Queries públicas: connect 10 s, read 20 s, call 30 s e deadline interativo 45 s incluindo espera por quota. No máximo duas novas tentativas por operação, três emissões totais: resolução textual automática até 15 emissões, caminho por ID até três, sempre sujeitos a deadline/orçamento. Backoff de rede/5xx de 2 s e 4 s com jitter; esgotado, atraso durável de 1 min até 6 h. Sem retries transparentes não contabilizados em OkHttp/WorkManager.

429 usa o maior prazo válido de `Retry-After`, reset e cooldown local, com jitter; sem header válido, mínimo 60 s. Usar `Date` do servidor para diferença de relógio e relógio monotônico nas esperas do processo. Se não couber no deadline, persistir `not_before` e terminar pendente. Botão de retry não fura cooldown. Persistir contagem/budget por operação/generation; reinício não os zera.

Cinco falhas transitórias consecutivas abrem circuito por 5 min; depois uma sondagem, com nova falha ampliando até 1 h. 429 usa cooldown próprio; falha de imagem não abre circuito GraphQL. Número máximo de rodadas por execução de lote, duração/renovação de leases e fairness exata ainda são P-07; nenhuma implementação pode usar essa lacuna para um loop ilimitado.

## 8. Fluxos de usuário

### FU-01 — Ativar e consultar uma obra

Pré-condições: anime persistido, recurso inicialmente off. Usuário abre configurações, lê explicação e ativa. Não há lote automático. No detalhe, solicita busca; UI mantém capa atual e informa pendente/buscando. Coordenador lê política/cache, verifica ID seguro ou busca; resultado oferece associação elegível somente se gates satisfeitos, revisão ou ausência válida. Após commit, capa/estado se atualizam; erros mantêm navegação e capa anterior. Ativar sem solicitar ação não pesquisa biblioteca.

### FU-02 — Resolver ambiguidade ou informar ID/link

Mostrar original ao lado dos candidatos, títulos/ano/formato/edição, provedor/ID/link, miniatura, sinais ausentes e conflitos; sem candidato ambíguo pré-selecionado. Usuário pode editar consulta, continuar páginas, rejeitar, informar ID/link validado ou manter original. Selecionar valida detalhes; conflito exige aviso explícito. Confirmação grava `MANUAL` + pin e cancela tentativa anterior. Falha de validação/SQL conserva decisão anterior. Miniaturas falhando não mudam ranking nem identidade.

### FU-03 — Corrigir e renovar uma associação

Com vínculo aceito, “atualizar” lê o mesmo ID e renova snapshot. “Corrigir” abre propostas e conserva vínculo/capa em vigor enquanto pesquisa; cancelar não altera nada. Confirmar novo ID troca vínculo e pin atomicamente. Mudança posterior do tracker apenas sinaliza conflito. Capa manual local continua prioritária; UI informa isso e oferece a ação herdada de removê-la, sem removê-la ao selecionar ID externo.

### FU-04 — Ver original, usar original e remover

Preview original ignora manual/externa temporariamente; fechar restaura a seleção. “Usar original” persiste política e cancela tentativa do item; voltar ao modo normal exige ação explícita. “Remover associação” remove ID e bloqueia reassociação automática; mantém manual local. Remover arquivo personalizado é comando distinto. Se a original faltar, exibir placeholder; não prometer recuperar bytes removidos pela fonte.

### FU-05 — Desligar e limpar

Desligar conserva decisões, cancela enriquecimento e reconstrói requests como manual/original. Limpar cache remove só dados descartáveis externos e conserva pins/políticas. Limpar tudo desliga primeiro, invalida tentativas e apaga somente dados da camada; nova ativação exige opt-in. Se resposta antiga chegar durante qualquer ação invalidante, ela não repõe vínculo/cache nem reaparece na tela. Cooldown AniList compartilhado continua válido.

### FU-06 — Navegar offline ou com falha externa

Biblioteca/detalhe/episódios/player continuam disponíveis segundo os fluxos herdados. Usar cache externo elegível sem pesquisa; sem bytes/snapshot elegíveis, manual/original/placeholder conforme modo. UI conserva associação e mostra data/falha. Tentar novamente respeita rede/cooldown; não muda pin nem procura substituto por causa de capa 404.

### FU-07 — Executar lote e retomar

Disponível apenas no incremento de sincronização, com opt-in próprio; recorrência off por padrão e sem periodicidade obrigatória definida. Usuário escolhe biblioteca/escopo e política de rede, inicialmente não medida; dados móveis exigem opção explícita. Fila pula frescos, bloqueados, ambiguidades aguardando revisão e pins sem renovação necessária. Exibe resumo; pausar/cancelar preserva commits. Retomada após processo encerrado reaproveita fila/cursor/contadores, sem reenviar tudo nem ultrapassar quota.

### FU-08 — Restaurar decisões

Restaurar animes/relações primeiro e remapear bloco opcional depois, sem rede. Sem bloco, comportamento herdado. Pin do destino prevalece; dois pins divergentes ou fonte/URL/contexto não únicos geram pendência de revisão. Provedor desconhecido fica inativo. Backup não ativa enriquecimento; detalhes externos precisam de validação sob opt-in antes de exibição. Relatório de restore distingue decisão aplicada de conflito/inatividade; representação final é P-05.

## 9. Critérios de aceitação

Os critérios descrevem implementação futura. **Nenhum está declarado aprovado em execução nesta entrega documental.** Evidência exigida inclui cenário, fixture/build, resultado e limitações; testes planejados estão na seção 11.

| ID | Dado / quando / então | Testes |
| --- | --- | --- |
| CA-01 | Dado banco novo/upgrade, quando iniciar app e abrir biblioteca, então opt-in está off e há zero chamadas de enriquecimento. Ao ativar sem ação/gatilho habilitado, não inicia varredura | T-01, T-10, T-16 |
| CA-02 | Dado snapshot de dados herdados, quando associar/renovar/limpar/remover, então valores de origem, customizações, relações, episódios, downloads, histórico e progresso permanecem; somente dados da camada mudam | T-07, T-08, T-18 |
| CA-03 | Dado cliente mockado com tracker/fonte autenticados, quando pesquisar/validar ID ou baixar capa externa, então chamada pública não contém Bearer/cookie/header da fonte nem mutation; entrada inválida não provoca request indevido | T-01, T-02, T-13 |
| CA-04 | Dados dois animes de fontes distintas e pai/filho, quando um for associado, então somente aquele ID local ganha decisão; mesmo remoto pode compartilhar snapshot/bytes sem unir dados locais | T-04, T-07, T-11 |
| CA-05 | Dadas fixtures de validação separadas da calibração, quando avaliar matching, então não há falso positivo automático conhecido; relatório inclui cobertura e abstenção. Sem configuração aprovada, nenhum texto gera `AUTO_MATCH` | T-04, T-05 |
| CA-06 | Dado empate, conflito, contexto insuficiente ou pesquisa incompleta, quando avaliar, então não muda capa/vínculo; informa revisão/incompletude e não cria negativo indevido | T-02, T-04, T-06 |
| CA-07 | Dada revisão manual, quando editar/paginar/rejeitar/selecionar, então candidatos exibem evidências sem pré-seleção ambígua; rejeição impede automação; ID/tipo inválido e validação falha não criam vínculo | T-06, T-14 |
| CA-08 | Dado pin e busca concorrente, quando renovar/trocar tracker ou receber resposta atrasada, então ID fixado permanece. Corrigir só substitui após confirmação/commit válido | T-09, T-14 |
| CA-09 | Dada associação, quando faltar capa, expirar snapshot ou ocorrer erro, então vínculo permanece observável com saúde/falha própria; `Media = null` íntegro marca indisponível sem rematching | T-02, T-10, T-12 |
| CA-10 | Dadas capas manual/externa/original disponíveis, quando resolver normal/off/original-only, então a ordem segue §7.1; preview não muda política e não colide em cache com normal | T-11, T-14 |
| CA-11 | Dado 404/decode externo ou manual corrompida, quando carregar, então fallback é finito, preserva arquivo/pin e usa chave/transporte próprios. Offline/timeout/429 não disparam todas as variantes; cancelamento não inicia próxima | T-12, T-13 |
| CA-12 | Dado vínculo/manual local, quando desligar, usar original ou remover associação, então não há novas operações externas proibidas, o efeito visual segue modo e remover vínculo conserva arquivo/bloqueia reassociação | T-09, T-14, T-16 |
| CA-13 | Dada escrita HTTP/Coil em andamento, quando limpar cache/tudo, então resposta da epoch anterior não repovoa dados/bytes elegíveis; somente assets externos são removidos. Cache clear mantém pins; clear total desliga | T-09, T-10, T-13 |
| CA-14 | Dados pedidos concorrentes, erro SQL ou morte de processo, quando confirmar/retomar, então existe no máximo uma pendência por item/provedor, sem publicação parcial ou sobrescrita de decisão recente | T-07, T-09, T-16 |
| CA-15 | Dado banco novo e snapshots anteriores válidos, quando migrar com FKs, então tabelas iniciam vazias/off, dados herdados são iguais; falha induzida faz rollback sem recriar banco | T-08 |
| CA-16 | Dados backups antigo/novo, IDs remapeados, duplicatas e pins divergentes, quando restaurar offline, então não há rede/ativação pelo backup, remapeamento é inequívoco ou conflito, e pin do destino não é sobrescrito | T-15 |
| CA-17 | Dado relógio falso, quando vencer 7/30 dias ou 24 horas conforme camada, então política respeita frescor/retenção, leitura/falha não estendem TTL e negativo não nasce de erro/incompletude | T-10 |
| CA-18 | Dados `Anime`, `AnimeCover`, request direto, imagem aberta, widget e notificação, quando mudar revisão confirmada, então seleção vigente é reconstruída/atualizada; keyer não faz I/O e renderização não pesquisa | T-11, T-13, T-17 |
| CA-19 | Dado lote opt-in, quando pausar/cancelar/reiniciar, então cursor/fila/budget persistem e itens frescos não repetem; sem opção de dados móveis, trabalho de lote espera rede não medida. Scroll/episódios não enfileiram pesquisa | T-16, T-17 |
| CA-20 | Dado relógio/transporte falsos e clientes AniList concorrentes, quando ocorrer 429/5xx/cancelamento/reinício, então total/suborçamento, espaçamento, teto de emissões, deadline, circuito e cooldown persistido são respeitados | T-03, T-16 |
| CA-21 | Dadas falha, ambiguidade e limpeza, quando navegar por leitor de tela/foco ou consultar logs, então estado/ações/diferenças são compreensíveis sem depender de cor/miniatura; textos localizáveis e sem dados privados nos logs | T-14, T-19 |
| CA-22 | Dado baseline e mesma biblioteca/dispositivo, quando medir fluxos herdados e scroll, então relatório compara memória/consultas/rede/tempos e avalia gates de P-08; sem medição não declarar ausência de regressão | T-17, T-18 |

## 10. Casos extremos e falhas

| Caso | Resultado exigido | Recuperação/limite |
| --- | --- | --- |
| Título vazio após normalização ou atributo ausente | Não criar evidência/filtro; texto não sustenta decisão automática | Oferecer consulta/ID manual; limites de tamanho em P-04 |
| Título curto, tradução aproximada, acentos/alfabetos diferentes | Sinal auxiliar sem falsa certeza | Revisão quando contexto insuficiente; T-04 |
| Coleção, temporada filha, remake, recap, OVA, parte/cour | Preservar escopo e vetos; sem dedução por ordem de relações | Escolha manual avisada; T-04 |
| Páginas restantes, duplicatas entre páginas ou erro parcial | Deduplicar por provedor/ID; marcar incompleta quando aplicável | Sem automático/negativo indevido; continuação explícita |
| HTTP 200 com `errors` | Manter só campos íntegros; distinguir identidade de imagem | Não apagar cache bom com nulo de erro; T-02 |
| Enum desconhecido / datas parciais | Preservar desconhecido/partes válidas | Sem completar ano/formato ou suspender só por enum novo |
| `Media = null` sem erro relevante | ID indisponível, pin mantido, capa original | Correção explícita; não rematching automático |
| DNS/offline/timeout/5xx | Vínculo/cache preservados, erro transitório | Retries finitos/deadline, fila posterior; T-03 |
| 429 HTTP ou GraphQL equivalente | Cooldown compartilhado, sem negativo | Headers válidos ou mínimo 60 s; botão/reinício não furam espera |
| 400/query incompatível, JSON inválido ou contrato quebrado | Erro de contrato; não repetir cegamente | Suspender operação afetada e informar indisponível |
| 401/403 público | Não solicitar token de tracker ou contornar acesso | Suspender/revisão explícita; original |
| Capa sem URL/mesmas URLs em tamanhos distintos | Vínculo válido, deduplicar variantes | Outra variante segura ou original; sem inventar caminhos |
| URL HTTP, credencial, host inseguro/local ou redirect proibido | Não emitir chamada externa indevida | Falha de imagem; outra variante segura/original; allowlist P-02 |
| 404/410 ou bytes inválidos externos | Remover só asset corrompido e bloquear URL/revisão | Próxima variante finita/original; revalidação agendada e deduplicada |
| Arquivo manual corrompido, fonte ausente ou original quebrada | Não apagar arquivo; usar caches/alternativas legítimas | Placeholder final se nenhuma tentativa servir |
| Disco cheio/falha SQL | Rollback, sem confirmação só em memória | Estado anterior e erro local acionável |
| Anime apagado enquanto HTTP responde | Ticket inválido, nenhum vínculo recriado | Cascade e descarte sem retry desse resultado |
| Off/clear/seleção enquanto HTTP ou decode está em voo | Generation/epoch bloqueiam gravação/publicação antiga | Cancelamento mais barreira de cache; T-09/T-13 |
| Processo morre em `SEARCHING` ou muda relógio | Retomar lease vencida, conservar budgets/cooldown | Relógio monotônico no processo e instantes duráveis; política P-07 |
| Restore encontra fonte/URL duplicados ou pin conflitante | Nenhum primeiro registro escolhido por `LIMIT 1` | Revisão/remapeamento explícito sem perder animes/pin de destino |
| Sem dispositivo Android utilizável | Teste manual continua pendente | Não converter baseline de build em aprovação visual/regressão |

O cancelamento de coroutine é fluxo de controle, não falha transitória do provedor. Mudança de título personalizado não é entrada de busca automática. Atualização só da capa original não troca identidade nem dispara GraphQL. Nenhum erro de disponibilidade permite fallback para outro anime ou outro catálogo.

## 11. Testes planejados

### 11.1 Estratégia e evidências

Testar contratos isolados com transporte HTTP/GraphQL falso, dispatcher de coroutines e relógio controlados; matcher com fixtures rotuladas; persistência em SQLDelight com FKs/rollback e snapshots válidos; pipeline com bytes válidos/corrompidos e erro de decoder; UI/worker com restrições e reinício; regressão no dispositivo. Fixtures usam dados sintéticos ou amostras mínimas justificadas, sem espelhar catálogo/imagens no Git ou usar biblioteca privada.

Cada execução futura registra SHA, configuração/política de matcher, entrada/fixture, expectativa, resultado, contagens de chamadas/dados quando pertinentes e limitações. Zero falhas num comando não substitui inspeção do relatório. A sessão documental verifica integridade e cobertura do documento; os testes abaixo **ainda serão implementados/executados**.

| ID | Nível e conjunto de cenários | Oráculo/gate | Sessão |
| --- | --- | --- | --- |
| T-01 | Unidade/contrato provider: busca, ID, filtros, páginas e inputs inválidos | Requests/DTOs corretos; chamadas públicas neutras; opt-in impede emissão | 4 |
| T-02 | Contrato: nulos, parcial por caminho, vazio completo, enum novo, JSON inválido, `Media = null` | Completude/erros corretos; sem negativo indevido ou ID aceito com identidade comprometida; falha apenas de imagem conserva identidade íntegra | 4 |
| T-03 | Transporte com relógio falso: 400/401/403/429/5xx, headers ausentes/inválidos, clock skew, deadlines, circuito, restart e cancelamento | Sequência/tempo/contagem dentro de §7.4; sem retry oculto | 4, integração 9 |
| T-04 | Unidade matcher: japonês/romaji/português/inglês; acentos, distribuição, números, sigla, remake, filme/TV, coleção, partes/temporadas e rejeições | Normalização preserva discriminadores; vetos/abstenção determinísticos | 5 |
| T-05 | Calibração e validação independentes, incluindo falsos candidatos deliberados | Zero falso positivo automático conhecido no conjunto validado; cobertura/abstenção e parâmetros publicados; sem gate, automação off | 5 |
| T-06 | Matcher/revisão: empate, dados ausentes, busca truncada, correção e candidato rejeitado | Revisão sem substituição; completude e rejeição respeitadas | 5, 8 |
| T-07 | Integração SQL: constraints, cascade, várias fontes mesmo remoto, observar lote, pin/rejeição/modo e rollback | Integridade/isolamento, uma associação por anime, sem escrita herdada | 6 |
| T-08 | Migração: banco novo, snapshots de versões anteriores válidas e falha induzida | Igualdade de valores/contagens herdados e defaults off; rollback preserva banco | 6 |
| T-09 | Concorrência: duas buscas, escolha/rejeição/original, remover anime, off/clear e morte de processo durante commit | Só ticket vigente publica; sem recriar/repovoar dados; sem sucesso parcial | 6, integração 7/9 |
| T-10 | Cache com relógio falso: 7/30 dias, 24 h, LRU, negativo, limpeza, falha e offline | Datas não renovadas por leitura/falha; retenção/expiração seletivas | 6, integração 7 |
| T-11 | Resolver: modos, manual/externa/original, variante preferida/duplicada, `Anime`/`AnimeCover`, cache compartilhado | Plano determinístico correto e chaves sem colisão de modo/origem | 7 |
| T-12 | Coil: fetch/decode inválido, manual corrompida, 404/410, ausência, original falha, offline/timeout/429, cache-only e cancelamento | Fallback finito, sem apagar manual, sem contaminação de chave e sem retry de todas URLs em falha do host | 7 |
| T-13 | Integração Coil: headers/redirects, revisão com request aberto, chave explícita, clear durante escrita/decode | Transporte seguro e barreira de epoch comprovada; resultado antigo não reaparece | 7 |
| T-14 | Compose/instrumentação: ativar, buscar/ID, conflito, pin, corrigir/cancelar, preview, modo/remover, clear, fonte local, acessibilidade e localização | Ações/estados correspondem a FU-01–FU-06; foco/semântica/texto utilizáveis | 8 |
| T-15 | Backup round-trip: antigo/novo/leitor sem campo, IDs remapeados, parent/temporadas, duplicatas, pins, provider desconhecido e offline | Sem rede/autoativação; decisões remapeadas ou conflito explícito; riscos `custom*`/manual reportados separadamente | 6 |
| T-16 | WorkManager/fila: rede medida, pause/cancel/restart, lease, idempotência e orçamento agregado tracker/AniChart/AniSkip/provider | Fila/budgets persistem; prioridade/fairness e limites respeitados; sem duplicar trabalho | 9 |
| T-17 | Integração consumidores: biblioteca/busca/histórico/updates/temporadas, detalhe, visualizar/salvar/compartilhar, widget/notificação e scroll | Revisões atualizam seleção; zero busca por renderização/episódio; keyer sem I/O | 7–10 |
| T-18 | Regressão: checklist herdado e benchmark com biblioteca representativa | Comparação contra baseline no mesmo dispositivo; preservação de fluxos/dados e avaliação de P-08 | 10 |
| T-19 | Logs/diagnóstico e estados de falha | Contadores/motivos úteis sem título privado, fonte URL, corpo, credenciais ou telemetria remota nova | 4–10 |

### 11.2 Gates de execução

Na implementação, aplicar `testDebugUnitTest`, `spotlessCheck`, `verifySqlDelightMigration`, `:app:assembleDebug` e `:app:lintDebug` conforme os módulos alterados e o [script de baseline](../../scripts/run-baseline.ps1). Examinar XML/relatórios e snapshots, sem considerar lint limpo apenas por exit 0. Não executar toda a bateria Android para este commit exclusivamente documental.

O [baseline da Sessão 2](../BUILD_BASELINE.md) registra 52 testes herdados passando, verificação SQL sem snapshots históricos, erros herdados de lint e falha de emulação que impediu instalação/regressão manual. Esses resultados não testam o recurso especificado. Migração exige snapshots válidos; backup exige ida e volta; ativação textual exige P-01 resolvida; integração visual/performance exige dispositivo utilizável. Evidências futuras devem dizer quais gates seguem abertos.

## 12. Tarefas das sessões seguintes

A ordem abaixo detalha a divisão já indicada na Etapa B (Sessões 4–10). Não atribui escopo novo às Sessões 11–13, que não está definido na fonte canônica disponível. Cada tarefa prevê implementação e validação correspondentes; a presente entrega não inicia essas sessões.

| Tarefa / sessão | Entrega concreta | Dependências | Conclusão verificável |
| --- | --- | --- | --- |
| S4-01 / 4 — contratos e provider | Tipos compartilhados IF-01/02, DTOs AniList, mapper com parcial/completude e busca/ID mockáveis | Arquitetura aprovada; esta especificação; P-03/P-04 para limites do contrato | T-01/T-02; nenhuma mutação/autenticação do tracker |
| S4-02 / 4 — transporte e quota | Cliente neutro, URL/redirect policy, orçamento compartilhável, falhas, cancelamento e relógio injetável | S4-01; P-02 para habilitar hosts; P-07 para política operacional | T-01/T-03/T-19; limites de emissões demonstrados |
| S5-01 / 5 — matcher puro | Normalização versionada, evidências/vetos, escopo, ranking e rejeições | Tipos S4-01; P-04 para identidade/fingerprint | T-04/T-06 determinísticos; sem rede/SQL |
| S5-02 / 5 — calibração | Fixtures rotuladas separadas de validação, parâmetros e relatório cobertura/abstenção | S5-01; decisão P-01 | T-05 e revisão do gate; até lá `AUTO_MATCH` textual off |
| S6-01 / 6 — persistência | DDL aditivo, queries/índices/adapters, repository Flow em lote, flags/versões/fila e migração livre posterior à 147 | Contratos IF-03/objetos; P-04/P-07 resolvidos para DDL | T-07/T-08; comparação de dados herdados |
| S6-02 / 6 — consistência/cache | Confirmação condicional, locks/leases, TTL/LRU, off e clear com epoch e estado durável | S6-01 | T-09/T-10; respostas antigas recusadas |
| S6-03 / 6 — backup de decisões | Bloco opcional/versionado, remapeamento, mescla e conflitos sem rede | S6-01; P-05 | T-15 antigo/novo/leitor compatível e restauração offline |
| S6-04 / 6 — riscos herdados de restore | Reproduzir omissão de `custom*` e documentar comportamento de manual local; decidir correção em escopo explícito | Baseline e fixtures de backup | Evidência round-trip; não alegar preservação integral enquanto houver perda |
| S7-01 / 7 — resolver/Coil | Plano imutável, modelos/keyers/interceptor/fetcher, transportes e fallback de decode | S4-02, S6-01/02; P-02/P-06 | T-11/T-12/T-13, sem mudança de backgrounds/episódios |
| S7-02 / 7 — consumidores | Integrar requests `Anime`/`AnimeCover` e diretos, Flow/revisão, visualizar/salvar/compartilhar, widget/notificação | S7-01 | T-17; leitura em lote, atualização visível e keyer sem I/O |
| S8-01 / 8 — configurações e revisão | Opt-in, estado/origem/data, busca/ID/páginas, seleção/conflito/rejeição e correção | S4, S5, S6, S7; P-03/P-09 | T-14; sem pré-seleção ambígua ou score apresentado como percentual |
| S8-02 / 8 — controle e acessibilidade | Preview/modo original, remover vínculo/manual distintos, clear cache/tudo, foco/semântica e recursos traduzíveis | S8-01 e comandos IF-03 | T-14/T-19; ações com alcance compreensível |
| S9-01 / 9 — coordenador/worker | Conectar IF-04/07/08, single-flight, fila única, restrições, resumir/pausar/cancelar/retomar lote | S4–S8; P-07 | T-16 com restart/cooldown agregado; lote off inicialmente |
| S9-02 / 9 — gatilhos e coexistência | Habilitar somente gatilhos aprovados, integrar orçamento tracker/AniChart/AniSkip e política rede móvel | S9-01; P-09 para defaults restantes | T-03/T-16/T-17, sem pesquisa em scroll/episódio e sem regressão OAuth |
| S10-01 / 10 — regressão e desempenho | Executar checklist, regressão de dados/backup/fluxos, benchmark comparável e relatório de gates | S4–S9, dispositivo; P-08 | T-17/T-18 e relatórios reais; limitações do baseline explicitadas |
| S10-02 / 10 — consolidação | Registrar resultados por CA, pendências remanescentes e prontidão do incremento | S10-01 e evidências T-01–T-19 | Aceite técnico sustentado por evidência; P-10/P-11 continuam gates de distribuição próprios |

Locais de implementação previstos: `domain/.../metadata`, `data/.../metadata`, SQLDelight em `data`, `app/.../data/metadata/anilist`, integração `app/.../data/coil`, bindings Metro e telas/recursos existentes. Widget consome contrato do domínio sem depender de `app`. Pontos reais de referência incluem [Anime.kt](../../domain/src/main/java/tachiyomi/domain/anime/model/Anime.kt), [animes.sq](../../data/src/main/sqldelight/tachiyomi/data/animes.sq), [AnilistApi.kt](../../app/src/main/java/eu/kanade/tachiyomi/data/track/anilist/AnilistApi.kt), [AnimeImageFetcher.kt](../../app/src/main/java/eu/kanade/tachiyomi/data/coil/AnimeImageFetcher.kt) e [BackupAnime.kt](../../app/src/main/java/eu/kanade/tachiyomi/data/backup/models/BackupAnime.kt). O inventário de consumidores e fronteiras está na Etapa A e na seção 9 da Etapa B.

## 13. Decisões e definições pendentes

Estas pendências não reabrem as decisões arquiteturais aprovadas nem se convertem em requisitos novos. Quando não há responsável nominal registrado, indicar papel/sessão, sem atribuir a uma pessoa por suposição. Resolver cada item com decisão/evidência versionada antes do gate correspondente.

| ID | O que falta definir ou comprovar | Encaminhamento e momento | Comportamento até resolução |
| --- | --- | --- | --- |
| P-01 | Sessão 5 implementou parâmetros propostos, suficiência v1 e fixtures sintéticas separadas; revisão da composição/gate e eventual habilitação pendentes, conforme [contrato/relatório](anime-matcher.md) | S5-02/T-05 implementados; revisão antes de ativar | Automação textual off; seleção manual e ID verificado continuam possíveis |
| P-02 | Allowlist final de imagem/redirect, comprovada com respostas oficiais; `s4.anilist.co`/`s1.anilist.co` são candidatos da Etapa B | Sessão 4; S4-02, integração S7-01 | Sem liberar host por suposição; URLs não validadas não são buscadas |
| P-03 | Sessão 8 implementou ID decimal canônico e HTTPS `anilist.co/anime/<id>[/slug]` com gramática restrita e feedback localizado; [contrato](manual-review.md) | T-01/parser e integração de seleção; validação UI/Android pendente | Somente extração de ID e query fixa; nunca buscar URL digitada |
| P-04 | Fingerprint `identity-v1` da sessão 5 e payload/DDL da [sessão 6](persistence.md) implementados; JSON v1 limitado a 65.536 caracteres e sete tabelas separadas | S4-01/S6-01 implementados localmente | Não preencher atributos ausentes por inferência; validar evolução posterior do payload |
| P-05 | Campo raiz protobuf 600/bloco v1, remapeamento conservador e pendências duráveis implementados; fixtures de leitor anterior ignoram o campo opcional | S6-03/T-15; apresentação de conflitos na sessão 8 | Leitura anterior testada por fixture dos campos relevantes, sem promessa universal para todos os forks/versões |
| P-06 | Interceptor/chaves/cancelamento/barreira implementados, testes de cadeia sintética e DiskCache físico; decoder/UI Android, benchmark e saúde durável/revalidação de imagem continuam pendentes | Sessão 7 implementada localmente; limites no [contrato](cover-resolution.md) | Não confundir contrato JVM com aceite visual/performance; original permanece fallback |
| P-07 | Sessão 6 adotou lease de 60 s, três emissões por operação durável e barreira de escrita de imagens; fairness, rodadas/lote e integração do transporte ficam na sessão 9 | Sessões 6/9; defaults na [persistência](persistence.md) | Não confundir primitiva de orçamento persistido com quota agregada já integrada ao HTTP |
| P-08 | Biblioteca/dispositivo de benchmark, métricas/limites aceitáveis de regressão e critérios quantitativos de desempenho | Preparação e revisão Sessão 10, antes de julgar benchmark | Medir/reportar; não prometer metas numéricas ou desempenho já aprovado |
| P-09 | Sessão 8 implementou configurações/revisão e strings en/pt-BR; revisão visual/acessível pendente. Elegibilidade de gatilhos e agendamento/periodicidade continuam futuros | Produto/UI na sessão 8; automação somente na sessão 9 autorizada | Opt-in inicialmente off; preferência móvel de lote off; ativar não pesquisa biblioteca |
| P-10 | Adequação do produto, retenção e uso de imagens aos termos do provedor antes de escala/distribuição | Proprietário, gate herdado da Etapa B; sem contato externo nesta entrega | Especificação não certifica permissão de uso/distribuição |
| P-11 | Dispositivo utilizável, regressão manual pendente, lint herdado e comportamento real de restore `custom*`/manual; identidade Android/publicação continuam em seus registros próprios | Sessões 6/10 e [baseline](../BUILD_BASELINE.md)/[identidade](../IDENTITY.md) | Não alegar dados integralmente preservados, release pronta ou instalação testada |

A aprovação da Etapa B não escolheu pesos de matching nem validou hosts, migrações, restore, composição Coil, métricas ou termos. Os demais valores numéricos aqui descritos vêm dos defaults da arquitetura aprovada; qualquer alteração futura deve registrar motivo e atualizar contrato, testes e critérios afetados.

## 14. Verificação desta entrega

Esta entrega compreende documento funcional e atualização dos registros de aprovação/progresso. A revisão confronta requisitos, interfaces, modelos, fluxos, critérios, casos extremos, testes e tarefas com a Etapa B e os pontos herdados consultados; verifica também links locais, âncoras, referências entre IDs, cercas de código e whitespace do diff.

Não foram implementados ou executados cliente, matcher, DDL/migração, worker, UI, calibração, backup, testes Android ou benchmark nesta entrega. Não houve nova consulta AniList/API/CDN, instalação, push, PR ou release. As referências externas necessárias ao desenho permanecem na Etapa B; esta especificação não atualiza nem certifica suas condições atuais.

O resultado da validação documental está registrado em [PROGRESS.md](../PROGRESS.md); o commit local é identificado no histórico Git. A especificação fica disponível para revisão; a execução e o aceite das sessões futuras dependem das entregas e evidências acima.
