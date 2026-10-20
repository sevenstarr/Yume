# feat: persist metadata decisions and cache

Sem persistência, pins e decisões desapareciam entre reinícios e respostas atrasadas poderiam sobrescrever escolhas futuras. A camada SQLDelight agora separa decisões de caches, confirma escritas por revisão/generation/epoch/identidade e oferece TTL, negativos, limpeza seletiva e backup opcional com remapeamento conservador. A migração 148 é aditiva e mantém o opt-in off.

O restore herdado ignorava `custom*` mesmo quando presentes no arquivo. A projeção e a escrita agora conservam esses valores, com prioridade às personalizações locais e sem copiar valores de exibição às colunas originais. Capas manuais continuam sem backup de bytes. O índice/barreira de imagens reutiliza Coil; requests visuais, fallback e UI de enriquecimento ficam nas sessões seguintes.

Validação e limitações: [registro de comandos](evidence/SESSION_6_COMMANDS.md), [snapshot](evidence/SESSION_6.json) e [contrato](metadata/persistence.md). Testes usam SQLite real no JVM, transporte ausente/fake e fixture de schema sintética; não substituem regressão em Android. Lint integral do app permanece inconclusivo desde a sessão 4. Matching textual automático e integração de quota com o transporte continuam desativados/pendentes.

Código/documentação novos assistidos pelo Codex; recursos herdados mantêm autoria e avisos. Somente commit local, sem push/PR remoto/tag/release.
