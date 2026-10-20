# Cliente público AniList para metadados

O app ainda não possui um provider independente de tracking para buscar metadados de capas. Esta mudança adiciona contratos de domínio e um cliente público isolado, com busca paginada/ID, erros parciais, URLs restritas, opt-in desativado por padrão, cancelamento e retries/quota finitos.

Nenhum consumidor de UI/worker é ativado. Não modifica fontes, episódios, tracking, banco ou backup. Código novo Yume assistido pelo Codex; histórico e atribuições upstream preservados.

Validação e limitações: [registro de comandos](evidence/SESSION_4_COMMANDS.md) e [contrato implementado](metadata/anilist-client.md). Integração de budget durável/consumidores herdados permanece nas Sessões 6/9; renderização e regressão Android nas etapas próprias.

Rascunho local para revisão, sem abertura de PR ou envio remoto.
