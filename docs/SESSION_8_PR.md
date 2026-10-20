# feat: add metadata settings and confirmed manual selection

Não havia interface para ativar o enriquecimento ou corrigir um ID associado. Adiciona configurações e painel nos detalhes do anime com pesquisa editável/páginas/ID/link oficial, candidatos com títulos/ano/formato/miniatura e proposta validada por ID. Somente confirmação com reconhecimento fixa o ID; cancelar preserva o vínculo anterior. Refresh renova o mesmo ID; original/remoção/limpeza mantêm dados da fonte e capas locais.

Controle inicialmente off, preferência móvel de lote off e nenhum gatilho/worker novo. Correção de namespace do cliente AniList para `anilist`; revisão/epoch/lease impedem gravações obsoletas, e off/clear cancelam consultas. Strings en/pt-BR. Código/documentação novos assistidos pelo Codex.

Validação e resultados efetivamente executados em [SESSION_8_COMMANDS.md](evidence/SESSION_8_COMMANDS.md) e [SESSION_8.json](evidence/SESSION_8.json). Gate pendente: fluxo real em Android/TalkBack/foco e regressão visual, sem dispositivo conectado. Orçamento agregado/fila continuam na sessão 9; sem benchmark nem garantia de distribuição. Este é um rascunho local, sem push ou PR remoto.
