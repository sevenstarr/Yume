# feat: add durable metadata batch synchronization

O enriquecimento passa a ter fila SQL recuperável e sincronização opt-in da biblioteca. Lotes atualizam o ID já aceito e pesquisam candidatos para revisão, preservando pins, originais e personalizados. Associação automática por texto continua desativada.

Branch `feature/session-09-metadata-sync`, baseada em `origin/yume-develop`. Preserva separadamente o commit documental preexistente `a4bb6431c` de alinhamento do protocolo Git/workflows; o commit funcional contém exclusivamente a sessão nove.

- WorkManager em fatias, continuação sem backoff por sucesso, restrições de rede/bateria/carga e gatilhos separados para novos favoritos e atualização diária (off por padrão).
- Progresso, pausa/retomada/cancelamento, três tentativas duráveis por item e geração/CAS contra respostas atrasadas. Novo gatilho não revive pendências canceladas.
- Quota AniList compartilhada com tracker, AniChart e conversão AniSkip: 12 emissões/min, relógio monotônico, uma requisição em voo, prioridade interativa, cooldown e circuito persistidos. Payloads/autenticação e outros hosts preservados.
- Migração aditiva 149/schema 150 com snapshot sintético, textos en/pt-BR, contrato e ADR 0008. Implementação assistida pelo Codex.

Validação local: **298 testes em 20 suítes, zero falhas/erros/ignorados**; build debug, migrações SQLDelight, Spotless e lint data/widgets aprovados. Lote SQLite de 2.000 itens até conclusão, reinício/leases abandonadas, tentativas finitas, cancelamento transacional, prioridade concorrente, fechamento do corpo HTTP e mudanças do relógio. Scripts de upgrade/rollback preservam 11/18 tabelas existentes. Cinco APKs debug locais; assinatura v2 do universal verificada.

Limitações: lint integral final concluiu (exit 0), mas contém os três erros `NonObservableLocale` herdados e 205 avisos; três novos `UseKtx` preservam o uso explícito de `Editor.commit()` para verificar falha de persistência da quota. Uma tentativa anterior do detector AndroidX falhou internamente; não houve supressões. Nenhum dispositivo Android disponível para UI/TalkBack, Doze/reboot ou bateria. Testes usam provider/HTTP/relógio simulados, sem tráfego AniList/CDN real. Nenhuma release, tag, alteração da identidade Android ou início da sessão 10.

Evidências: [snapshot](evidence/SESSION_9.json), [comandos e tentativas](evidence/SESSION_9_COMMANDS.md), [contrato](metadata/background-sync.md).
