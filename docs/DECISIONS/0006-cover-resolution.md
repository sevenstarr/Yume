# ADR 0006 — Resolução de capas antes do keying e fallback de decode

20/10/2026. Implementação local da sessão 7 autorizada pelo proprietário; código/documentação novos assistidos pelo Codex. Direção aprovada no ADR 0002; resultados visuais e de performance ainda exigem dispositivo.

Resolver puro e índice de IDs ativos produzem a seleção, sem alterar a URL original ou pesquisar catálogo durante renderização. Uma observação SQL em lote, compartilhada e fora da thread principal, alimenta Compose, widgets e carregamentos imperativos. Uma solução limitada ao fetcher não cobriria erros tardios de decoder, nem atualizaria tiles já carregados.

O interceptor Coil antecede o engine/keying. Manual, externa e original têm chaves distintas; cada fallback passa novamente pela cadeia seguinte, preservando contexto, target e size resolver. Preview e preferência original-only ignoram manual/externa. Arquivo manual corrompido é conservado. Backgrounds/thumbnails/modelos genéricos não mudam de semântica. Exportação usa a página e a política capturadas ao clicar.

Assets externos usam somente o cache Coil existente, com revisão `fetchedAt` do snapshot público na chave, para compartilhar bytes entre fontes. Revisão local e epoch continuam obrigatórias na barreira durável. A barreira serial da sessão 6 foi substituída por oito permits, permitindo loads concorrentes; clear/prune adquirem todos antes da eviction final. Locks temporários por asset impedem downloads simultâneos duplicados. Mudanças de seleção cancelam o load em andamento e resultados antigos não são publicados.

Transporte CDN neutro tem allowlist restrita ao host/caminho já verificados, recusa redirects, limita bytes/timeouts e não leva estado de fontes/tracker. Falhas individuais permitem variantes do mesmo remoto; falha do host deixa as demais em cache-only. Quarentena limitada a 512 chaves e cooldown de imagens são em memória; persistência de saúde/revalidação e reset explícito de tentativa continuam pendentes. Não usar o circuito GraphQL para falha de imagem.

O opt-in permanece off e o gate de automação textual continua fechado. Ações de ver/usar original pertencem à reversibilidade da sessão 7; configurações globais, busca/seleção remota e sync ficam nas sessões 8/9. Não foram adicionadas tabelas/migrações ou imagens binárias neste incremento, apenas queries sobre o schema 149. Detalhes, consumidores e limites de prova em [cover-resolution.md](../metadata/cover-resolution.md).

O modo original pode ser persistido mesmo sem decisão/associação prévia, com opt-in off: revisão nula representa criação somente na ausência de decisão; reversão/alteração existente mantém CAS. Mudança de origem suspende o asset associado, conservando a preferência explícita de original. O helper Compose usa seleção inicial estável para evitar reconstruir requests desnecessariamente no default off.
