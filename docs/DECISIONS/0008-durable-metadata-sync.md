# ADR 0008 — Sincronização durável e orçamento compartilhado

20/10/2026. Implementado na sessão 9 autorizada pelo proprietário. Trabalho novo assistido pelo Codex.

## Contexto

As sessões anteriores forneceram provider, decisões SQL/CAS, cache e revisão manual. Ainda faltavam fila recuperável, gatilhos e contagem agregada de chamadas físicas dos consumidores AniList existentes. A aprovação de associação automática textual continua ausente.

## Decisão

- Acrescentar duas tabelas próprias pela migração 149, schema 150, sem alterar tabelas de biblioteca/episódios. Fila deduplicada por ID local, geração, epoch, três tentativas duráveis por item, próximo horário e resultados agregados. Nenhuma fila ou quota entra no backup de decisões.
- Usar o WorkManager existente com trabalhos únicos, rede conectada ou não medida conforme preferência, bateria não baixa e carga obrigatória para lotes diários. O disparador diário somente cria a fila; o executor único conserva a exigência de carga durante todo esse lote. Fatias sequenciais de até 20 itens e aproximadamente três minutos, sem foreground service ou wake lock próprios. O Android decide quando executar.
- A inclusão de favorito usa um hook mínimo na transação já existente do repositório; somente a transição de não favorito para favorito enfileira, quando o opt-in global e o gatilho específico estão ligados. Não observar/carregar toda a biblioteca em memória para detectar inclusões. Atualizações de fontes e episódios conservam seus fluxos.
- Reservar uma tentativa SQL antes de entrar no provider. Reaquisição de lease expirada exige geração ativa e revisão observada; não rouba uma revisão manual em andamento. CAS de gravação verifica também a geração/status do lote. Pausa/cancelamento recusam resultados atrasados mesmo antes de o observador cancelar a coroutine. Morte de processo não reinicia o limite de tentativas.
- Atualizar somente o ID já aceito, preservando pins. Pular metadados recentes (sete dias), original-only, bloqueios e resultados textuais recentes (um dia). Uma página de pesquisa serve para revisão e nunca autoriza associação automática. Não buscar páginas indefinidamente nem baixar capas em lote.
- Colocar o mesmo interceptor de rede nos quatro consumidores próprios de `graphql.anilist.co`: transporte público de metadados, tracker AniList, AniChart e conversão AniSkip. Outros hosts passam intactos. OAuth, mutations e fluxos de tracking permanecem nos clientes existentes.
- Persistir janela de emissões físicas e cooldown em SharedPreferences dedicado, com commit antes de enviar; falha de persistência impede a emissão. Limite local conservador de 12/min, espaçamento mínimo de cinco segundos, redução pelo header de limite e bloqueio por `429`, `Retry-After`, reset ou remaining zero. Cada retry/follow-up que chega à rede passa pelo mesmo orçamento. Não é garantia da quota de um IP compartilhado com outros aplicativos.
- Adotar inicialmente o teto agregado mais conservador de 12/min para todos os consumidores, em vez do default preliminar de 24/min com suborçamento 12/min; assim, o suborçamento também é respeitado. Uma única emissão em voo até EOF/close do corpo. Pedidos interativos (tracking/player/revisão) aguardando quota têm precedência sobre lote, identificado por tag local que não é enviada ao servidor. Não há preempção de HTTP já iniciado; tráfego interativo contínuo pode adiar lote.
- Persistir falhas/cooldown de circuito no mesmo armazenamento. Cinco falhas transitórias abrem cinco minutos; sondagem falha dobra a espera até uma hora, sucesso reinicia a sequência. O provider público encaminha falhas GraphQL e sucesso lógico; respostas HTTP/erros de transporte são observados na camada de rede. Nenhuma falha de CDN entra nesse circuito.

## Consequências

As chamadas herdadas podem esperar pelo orçamento compartilhado; cancelamento é consultado durante a espera. O provider mantém seus deadlines/retries/circuit breaker anteriores. O contador por ticket ainda reserva o teto de três emissões por chamada do provider; o interceptor registra as emissões físicas. Três tentativas de fila permitem no máximo nove emissões do provider por item naquele lote. Cache pode reduzir esse número a zero.

Reinício retoma pendências e mantém preferências off por padrão. Pausa suspende o lote e o disparador diário até retomada; cancelamento encerra o lote atual, mas não desliga gatilhos opcionais para futuros lotes. Off/limpeza invalidam trabalho por epoch; limpar enriquecimento não zera a quota compartilhada do host. Uma nova ação manual de lote cria outra geração, sem apagar biblioteca ou decisões.

Há apenas um processo de execução do WorkManager e um governor compartilhado entre os consumidores no processo principal; não se promete coordenação de quota com outros aplicativos/processos externos. Não há benchmark de bateria, Doze, reboot ou UI em Android nesta máquina sem dispositivo. Esses gates continuam explícitos no progresso. Bibliotecas grandes são verificadas com SQLite e provider simulado; isso não mede consumo real de rede/bateria.

## Referências

- [Contrato de sincronização](../metadata/background-sync.md).
- [Definição e restrições de WorkRequests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work), consultada nesta sessão.
- [Trabalho único e cancelamento](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work), consultada nesta sessão.
