# Cliente AniList — Sessão 4

20/10/2026. Código novo Yume, assistido pelo Codex. Base `48b23e78dd1b07526603b15b31bed8fd43caaf62`; branch `feat/session-4-anilist-client`.

## Contrato implementado

`domain/.../metadata` contém modelos e `MetadataProvider`, sem Android/UI/SQL no contrato. `AniListProvider` implementa busca paginada e leitura exata de ID. Retorna títulos romaji/inglês/nativo, sinônimos, ID MAL separado, formato, data parcial, estação/ano, episódios, link oficial e variantes de capa. Enums novos são desconhecidos. O adapter só aceita ANIME com ID positivo e pelo menos um título; ID retornado divergente não é substituído silenciosamente.

Busca usa query fixa, variáveis separadas, `perPage: 20` e `currentPage`/`hasNextPage`. Não usa `total`/`lastPage`: a [documentação oficial de paginação](https://github.com/AniList/docs/blob/master/docs/guide/graphql/pagination.md) alerta que esses totais não são confiáveis. Filtros opcionais são formato/estação/ano fornecidos pelo chamador; o cliente não deduz atributos do título.

Decisões de contrato P-04 nesta etapa: consulta aparada de 1–300 unidades UTF-16; páginas 1–50 por chamada explícita; ano 1–9999; ID decimal canônico no namespace `ANILIST`, de 1 a `Int.MAX_VALUE`. Não há conversão implícita de ID MAL nem parser de links digitados; P-03 permanece para o fluxo de escolha manual. Filtros não autenticam o tracker.

`Success.complete` informa integridade da resposta; `identityComplete` distingue erro apenas de imagem de identidade comprometida. `hasNextPage` conserva a necessidade de continuar a pesquisa. Um `Success` de uma página não autoriza matching automático. Erros por caminho removem campos afetados; candidato com erro de identidade é omitido da busca e torna o resultado incompleto. Falha apenas de capa conserva identidade, mas retira as variantes desse registro. Falha de data parcial descarta a data afetada conservadoramente. Mensagens arbitrárias do servidor não entram no domínio/logs. Nulo íntegro de `Media` vira `NotFound`; JSON inválido, campo raiz ausente, erro sem caminho ou contrato incompatível viram falha de contrato, nunca ausência válida.

## Transporte e orçamento

`AniListMetadataClient` é singleton Metro exposto no `AppGraph`; construir o grafo não faz rede. `provider()` é desativado por padrão; um consumidor futuro deverá fornecer a política opt-in vigente. Nenhuma tela/worker/ação herdada chama o cliente nesta sessão. O gate é conferido antes da chamada, ao obter o lock e após esperar quota. Cancelamento invalida a chamada real inclusive durante leitura do corpo. A futura invalidação off/clear por epoch continua sendo responsabilidade do coordenador/repository.

O transporte deriva de `NetworkHelper.client` para compartilhar pool, dispatcher, DNS/TLS/proxy, removendo interceptors de fonte/Cloudflare/logging, cookies, cache HTTP e authenticators. Envia apenas POST ao endpoint fixo `https://graphql.anilist.co`, JSON, Accept e User-Agent público; não importa tokens OAuth nem headers da extensão. Redirects são recusados. Timeouts connect/read/call: 10/20/30 segundos; resposta limitada a 2 MiB antes da desserialização.

O corpo é marcado one-shot para impedir follow-ups transparentes. Essa decisão também cobre o caso `503` com `Retry-After: 0`: o [código oficial do OkHttp 5.5.0](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/http/RetryAndFollowUpInterceptor.kt) pode repetir esse status mesmo com `retryOnConnectionFailure(false)`. Cada retry explícito cria nova chamada/corpo e consome orçamento.

Todas as instâncias produzidas pelo singleton compartilham um lock/orçamento de processo: uma requisição em voo, espaçamento mínimo 2,5 s, até 12 emissões em janela móvel de 60 s. Headers mais restritivos diminuem o teto; aumentos não elevam o default. Falhas de rede/5xx têm no máximo três emissões, backoffs 2/4 s com jitter 0–500 ms e deadline total 45 s incluindo espera pelo lock/quota. Cinco falhas transitórias consecutivas abrem circuito de 5 min; sondagem falhando aumenta a duração até 1 h. Sucesso reinicia o circuito; 429 tem cooldown próprio.

HTTP/GraphQL 429 retorna adiamento tipado sem retry imediato. O maior prazo entre `Retry-After` (segundos ou HTTP-date), reset e bloqueio local prevalece, com jitter; sem prazo válido, espera mínima de 60 s. `Date` remoto corrige diferença de relógio no cálculo relativo e espera usa relógio monotônico. `Remaining: 0` também bloqueia novas emissões. HTTP 400/401/403/404/410, redirects, JSON inválido e falha de contrato não sofrem retry cego. Repetições de respostas íntegras idênticas são reaproveitadas por 5 s, com até 32 entradas em memória por provider; falhas/parciais não são reaproveitadas. Essa contenção de repetição não substitui o cache persistente da Sessão 6.

**Limites de integração:** o orçamento ainda não engloba tracker/AniChart/AniSkip; não há prioridade/fairness entre esses consumidores. Cooldown, circuito e contadores ainda não sobrevivem à morte do processo. Persistência será integrada na Sessão 6 e orçamento agregado/fila na Sessão 9, antes de ativar o recurso. T-03 de transporte local é exercitado aqui; restart/orçamento agregado de CA-20/T-16 continuam pendentes. Não há promessa de quota global de 24/min cumprida por clientes herdados nesta entrega isolada.

## Verificação do serviço e imagens

Consulta pública única em 20/10/2026 09:40:20 UTC: HTTP 200, `X-RateLimit-Limit: 30`, `Remaining: 29`. Query de `Media(id: 1, type: ANIME)` confirmou todos os campos usados, três URLs de capa e `bannerImage`. Apenas payload público transitório foi lido; nenhuma imagem/binário de catálogo foi baixada ou versionada. Os testes usam fixtures sintéticas e transporte falso, sem acesso externo.

Fontes oficiais foram relidas pelo repositório AniList porque o site de documentação retornou 403. A [política de rate limiting](https://github.com/AniList/docs/blob/master/docs/guide/rate-limiting.md) mantém aviso de 30/min, headers de quota, cooldown e proteção contra rajadas. Os [termos de uso](https://github.com/AniList/docs/blob/master/docs/guide/terms-of-use.md) restringem coleta em massa e usos concorrentes e mantêm condições comerciais; esta sessão não resolve o enquadramento para distribuição.

A [referência Media](https://github.com/AniList/docs/blob/master/docs/reference/object/media.md) expõe banner como uma URL opcional; não define variantes/dimensões fixas. A [referência MediaCoverImage](https://github.com/AniList/docs/blob/master/docs/reference/object/mediacoverimage.md) oferece `extraLarge`, `large`, `medium`, com fallback de extraLarge para large quando indisponível. Não se deduz resolução em pixels desses rótulos. Banner não é consumido no MVP de capas.

P-02 fica restrita ao host efetivamente observado: HTTPS `s4.anilist.co`, porta 443, sem credenciais/fragmento e caminho `/file/anilistcdn/media/anime/cover/`. Variantes com mesma URL são deduplicadas. Link público aceito somente no formato exato `https://anilist.co/anime/<id>` com barra final opcional. Outros hosts/paths retornados não são buscados. O futuro transporte Coil ainda deverá verificar redirects/DNS/bytes; validar uma string aqui não comprova segurança de download/renderização, que permanece na Sessão 7.

## Testes e continuidade

`AniListProviderTest` usa relógio, sleep e transporte injetados para campos, paginação, erros parciais, nulos, JSON inválido, gate, repetição, falhas e budgets. `AniListTransportTest` usa `Call.Factory`/callbacks simulados para inspecionar request público, configuração neutra, cancelamento, limite do corpo e propagação de I/O, sem servidor/rede. Resultados reais, tentativas e limitações estão em [SESSION_4_COMMANDS.md](../evidence/SESSION_4_COMMANDS.md).

O lint desta sessão foi interrompido após 20 min retido na travessia PSI de comentários do detector `BidirectionalText`, sem relatório atual concluído. Não foi desativado nenhum detector nem alterado o baseline. O relatório antigo não comprova ausência de achados novos; a validação integral de lint permanece pendente.

Próxima etapa: Sessão 5, normalização/matching/calibração, usando os modelos desta entrega. SQL/backup, UI/Coil e worker continuam fora desta implementação.
