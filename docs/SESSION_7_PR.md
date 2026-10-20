# feat: resolve enriched covers with finite fallback

Capas associadas ainda não eram consumidas pelas telas. O resolver compartilhado agora alimenta `Anime`/`AnimeCover` antes do keying Coil, com prioridade manual/externa/original, isolamento de transporte CDN e fallback também após erro de decode. Revisão/epoch, cancelamento, cache-only e limpeza seletiva impedem publicação de tentativas antigas. Writes diferentes podem ocorrer em paralelo e o mesmo asset é deduplicado.

Tiles e widgets observam mudanças locais; o visualizador permite ver/usar original de modo reversível. Visualizar, salvar e compartilhar respeitam capa/background e a seleção capturada. Fontes, tracking e player mantêm seus contratos; opt-in/automação permanecem off, sem pesquisa no scroll.

Validação e limites em [comandos](evidence/SESSION_7_COMMANDS.md), [snapshot](evidence/SESSION_7.json) e [contrato](metadata/cover-resolution.md). Testes JVM de cadeia usam resultados sintéticos; DiskCache físico é exercitado separadamente. Decoder/UI Android e benchmark de scroll ainda pendentes por falta de dispositivo. Somente commit local, sem push/PR remoto/tag/release. Código/documentação novos assistidos pelo Codex.
