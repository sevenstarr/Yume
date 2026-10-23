# Sincronização automática e em lote — Sessão 9

**Leitura atualizada na sessão 12:** os resultados abaixo pertencem à sessão 9.
O erro herdado de locale e a identidade/updater foram tratados posteriormente;
o [relatório de qualidade](../TEST_REPORT.md) e a
[arquitetura atual](../ARCHITECTURE.md) consolidam esse estado. Limites de
Android/bateria continuam abertos; CI remoto da sessão 11 falhou antes do
build conforme [distribuição](../BUILD_DISTRIBUTION.md).

Implementação nova assistida pelo Codex. [ADR 0008](../DECISIONS/0008-durable-metadata-sync.md).

## Uso e controles

Em **Configurações → Metadados externos**, ative o opt-in global, inicialmente desligado. Isso não inicia uma varredura. **Sincronizar metadados da biblioteca** cria um lote explícito dos favoritos atuais. Os controles mostram itens concluídos/total, atualizados, destinados à revisão, erros e ignorados. “Na fila ou em execução” inclui espera por restrições Android, cooldown e backoff; não significa tráfego contínuo.

**Sincronizar novos favoritos** é opt-in separado, persistido em SQL. Somente novas inclusões pelo repositório normal entram na fila; ativar a opção não percorre os favoritos antigos. Remover e readicionar um favorito pode criar outra tentativa; repetir uma atualização do mesmo favorito não reinicia seu orçamento. Backup/restauração não dispara uma varredura por esse hook. **Sincronizar diariamente durante a carga** começa off, tem atraso inicial de 24 horas e periodicidade de 24 horas, sujeitos ao Android. Sem dados móveis por padrão: usa rede não medida. Toda tarefa exige bateria não baixa; o lote criado pelo disparador diário conserva exigência de carga, inclusive depois de pausa/reinício.

**Pausar** retém pendências e contagens, cancela transporte e suspende o disparador diário até retomada. **Retomar** muda a geração e continua os itens pendentes. **Cancelar este lote** encerra a geração atual e marca suas pendências como ignoradas; novos favoritos não revivem esse trabalho cancelado. Gatilhos opcionais continuam configurados para futuros lotes. Para impedir qualquer sincronização, desligue o opt-in global. Off/clear invalidam o trabalho anterior e não religam resultados antigos. A limpeza total remove também a fila, mantendo os originais e os arquivos manuais.

Uma associação aceita consulta apenas seu ID. Dados completos recentes são ignorados por sete dias; resultados de busca e indisponibilidade têm intervalo conservador de um dia. Original-only e bloqueios explícitos não são alterados. Sem vínculo, pesquisa-se uma página usando o título original, com cache durável. Resultados ficam para revisão: **não há associação automática por texto**, independentemente do score. Um lote não baixa binários de capas nem acessa fontes, episódios, tracking ou progresso remoto.

## Persistência e recuperação

Migração aditiva `149.sqm`, schema 150; snapshot 149 sintético derivado do schema 148 e da migração 148, sem dados pessoais. `metadata_sync_control` guarda geração/epoch/status/opções e `metadata_sync_queue` guarda ID local, resultado, número de tentativas e horário mínimo. FK remove itens quando o anime é excluído. Uma fila nova substitui somente o histórico operacional do lote anterior; decisões de metadados e biblioteca são preservadas.

Executor único e mutex compartilhado impedem fatias concorrentes. Cada fatia lê um item por vez, até 20 itens ou três minutos antes de iniciar outro. O provider continua limitado por deadline. Havendo pendências, o worker encadeia uma continuação única com espera mínima de 30 segundos, estendida até o próximo horário/cooldown; fatias bem-sucedidas não acumulam backoff exponencial. Erros operacionais usam retry exponencial do WorkManager e pausam o lote após falhas repetidas. Não há thread dormindo aguardando conectividade/cooldown entre fatias. Três tentativas por item são gravadas **antes** do provider. Falhas transitórias adiam 60/120 segundos, respeitando horário posterior informado pelo serviço. Falhas finais contam como erro.

Revisão manual e sincronização usam leases/CAS; uma lease ativa não pode ser roubada. Depois da expiração, o coordenador pode iniciar outra tentativa limitada pelo contador durável da fila. Geração/status também são conferidos dentro das transações que aplicam metadados/cache. Cancelar/pausar recusa respostas atrasadas; `finally` não cancelável libera somente a lease pertencente à operação. Se o processo morre depois de persistir o resultado, mas antes de marcar a fila, a reexecução encontra TTL recente e ignora a consulta. Não se promete entrega exatamente uma vez de HTTP diante de morte do processo.

## Quota e isolamento

`AniListHostInterceptor` conta emissões de rede reais no transporte público, tracker AniList, AniChart e conversão AniSkip. A janela, limite reduzido por headers e cooldown são duráveis em armazenamento local dedicado. Política inicial: teto agregado de 12/min e espaçamento de cinco segundos, mais conservador que o default preliminar de 24/min; também satisfaz o suborçamento de 12/min para metadados. O servidor pode reduzir o ritmo. Há uma requisição em voo até EOF/close do corpo, com precedência de tracking/player/revisão que estejam aguardando sobre lote. A prioridade usa uma tag local e não transmite headers adicionais. Não se interrompe uma requisição já iniciada para ceder lugar.

`429`, remaining zero, `Retry-After` em segundos/data e reset prorrogam o bloqueio conservador, usando a data do servidor quando disponível. Cinco falhas transitórias abrem circuito por cinco minutos; nova falha na sondagem amplia até uma hora. Falhas/cooldown sobrevivem ao reinício. O provider público encaminha o status GraphQL à política compartilhada; os clientes herdados preservam seu parsing e têm HTTP/headers/erros de transporte observados pelo interceptor. Cancelamento interrompe espera antes de nova emissão; falha no armazenamento impede HTTP. A quota não é apagada ao limpar metadados, nem acompanha backup/restauração de decisões.

No processo, espaçamento e cooldown também usam relógio monotônico, evitando liberação antecipada por salto do relógio civil. A janela persistida usa timestamps civis para recuperação, com tratamento conservador de emissões no futuro. O transporte público consulta o bloqueio compartilhado antes de criar HTTP e devolve falha tipada com `retryAt`, permitindo ao worker adiar em vez de consumir seu deadline esperando um circuito de vários minutos. A contagem SQL é conservadora e pode incluir uma tentativa que terminou antes de emitir HTTP.

O provider público permanece sem cookies/Bearer/redirects; adicionar o interceptor neutro não copia interceptors da fonte ou tracker. O interceptor não muda payloads, autenticação ou operações herdadas. Só limita o host exato; AniSkip/Simkl/CDNs e demais hosts mantêm o transporte atual. Outros aplicativos no mesmo IP continuam fora do controle do Yume. Não existe teste de estresse contra o AniList real.

## Verificação e limites

Testes JVM usam SQLite real e provider/transporte simulados: fila grande, idempotência/TTL, pin e originais, cancelamento/pausa, geração/CAS, reinício/reabertura, lease abandonada, orçamento finito, indisponibilidade de rede, cooldown, inclusão opt-in, restrição de carga persistida e migração. Testes do governor usam relógio e armazenamento injetáveis, sem esperas reais ou chamadas externas.

WorkManager, Doze, restrições reais de rede/carga, reinício do Android, bateria e acessibilidade/UI ainda exigem dispositivo. Reabertura JDBC prova persistência de dados; não substitui execução Android. O lint integral final concluiu com os três erros herdados de locale; três avisos novos `UseKtx` permanecem porque o retorno Boolean de `Editor.commit()` é conferido para falhar fechado. Gates de identidade/termos/distribuição continuam registrados no progresso. Nenhuma funcionalidade das sessões 10–13 é presumida aprovada.
