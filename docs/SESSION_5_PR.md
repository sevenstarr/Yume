# Correspondência conservadora de animes

O cliente AniList retorna candidatos sem resolver identidade de temporada/edição. Esta mudança adiciona normalização versionada e um matcher puro que compara títulos/aliases e contexto confiável, registra ausências/conflitos, veta incompatibilidades e exige busca completa, suficiência e margem para propor associação automática.

Vínculos aceitos conservam ID em atualizações; pins manuais resistem a rematching e correção mantém o vínculo anterior. A seleção manual exige resposta de identidade íntegra do provider. Fingerprint e limites do domínio estão documentados para a próxima etapa de persistência.

Grade de parâmetros calibrada em arquivo separado da validação, com falsos candidatos deliberados e relatório por caso. **A configuração permanece proposta/desativada**: fixtures sintéticas não provam precisão em dados reais nem aprovam o gate de automação. Sem integração ativa, SQL, backup, UI, worker ou alteração no tracking.

Validação e limitações: [comandos](evidence/SESSION_5_COMMANDS.md), [relatório de matching](evidence/SESSION_5_MATCHING.txt) e [contrato](metadata/anime-matcher.md). Código/documentação novos Yume assistidos pelo Codex, com créditos upstream preservados.

Rascunho local, sem abertura de PR ou envio remoto.
