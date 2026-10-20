# ADR 0005 — Persistência de decisões e caches separados

20/10/2026. Implementação local da sessão 6, autorizada pelo proprietário. Código e documentação novos assistidos pelo Codex.

O banco SQLDelight existente recebe a migração aditiva `148.sqm` (schema 149). Nenhuma migração anterior ou coluna herdada é alterada. `anime_metadata` guarda uma decisão versionada por anime, incluindo pin, rejeições, identidade, preferência, datas e falha tipada. Payload JSON limitado a 65.536 caracteres evita acoplamento do esquema SQL a cada novo sinal do matcher. O MVP suporta AniList; novos provedores exigem evolução explícita do contrato.

Controle global, leases/orçamento por item, detalhes públicos, páginas/negativos, índice de imagens e pendências de restauração têm tabelas próprias. Opt-in começa off. Revisão/generation/epoch e identidade original são conferidos em transação antes de confirmar resultados. Rede não participa de transações. Leases duram 60 segundos; retomada conserva emissões, com teto de três por operação. O coordenador futuro precisará consumir esse contrato e integrar quota/cooldown ao transporte, ainda em memória na sessão 4.

Pins não expiram com o cache. Detalhes ficam frescos sete dias e elegíveis por menos de 30 dias. Consultas e negativos válidos duram menos de 24 horas, limitados a 200 páginas por LRU. Erros/parciais não substituem cache íntegro nem renovam data de verificação. Limpar cache avança epoch e mantém decisões; limpar tudo desliga, mantém cooldown e remove somente a camada nova.

Imagens usam o cache Coil existente e chaves `yume-metadata-v1:anilist:<id>:<hash-url>:<revisão>`. Um índice durável permite eviction seletiva. A barreira de escrita rejeita epoch/revisão vencidas e aguarda/removerá escrita antiga antes de concluir limpeza; callbacks devem apenas gravar bytes, e publicar somente quando o resultado for elegível. Não há segundo diretório de imagens. A integração dos requests/fallback/decode é sessão 7.

Backup usa o campo protobuf 600, antes livre no modelo raiz, com bloco opcional v1. Somente decisões e identidade de origem/contexto são exportadas, sem IDs locais, snapshots, URLs de imagens ou estado operacional. Opções de exportar/importar começam desmarcadas. Importação acontece após animes/relações, sem rede/opt-in; duplicatas, pins de destino e provedores desconhecidos não são sobrescritos. Pendências são persistidas para revisão futura. Dados restaurados exigem revalidação antes de imagens externas.

Correção localizada no restore herdado preserva `custom*` e mantém valores originais em colunas originais; personalizações existentes prevalecem. Não adiciona backup de bytes de capas manuais, ausentes no formato herdado. A [documentação de persistência](../metadata/persistence.md) delimita testes e recuperação: rollback é apenas para cópia descartável, não suporte a downgrade do aplicativo.
