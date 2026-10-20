# ADR 0003 — Cliente público AniList isolado

Data: 20/10/2026. Status: adotado na implementação da Sessão 4 autorizada pelo proprietário. Trabalho assistido pelo Codex.

## Contexto

A arquitetura aprovada no [ADR 0002](0002-metadata-enrichment-architecture.md) requer provider público separado do tracker. A [especificação](../metadata/functional-specification.md) define S4-01/S4-02, T-01/T-02 e a parte local de T-03/T-19. Tracking herdado utiliza OAuth/interceptors de fonte e não pode ser requisito para capas.

## Decisão

Usar modelos/provider no módulo `domain` e DTOs/adapter/transporte no `app`, sem novo módulo/dependência. `AniListMetadataClient` singleton Metro compartilha um orçamento entre seus providers. Derivar o OkHttp já existente, conservando recursos de transporte, mas removendo cookies/cache/interceptors/authenticators e bloqueando redirects/follow-ups. JSON vem da DI existente. Chamadas são suspensas, canceláveis, limitadas a três emissões e 45 s por operação; clock/transporte são injetáveis nos testes.

Ausência íntegra, falha de contrato e parcial são resultados distintos; campos danificados não chegam como evidência íntegra de identidade. Imagens ficam restritas ao host/caminho público observado, sem inventar dimensões ou baixar binários. Limites exatos de input/headers/circuito estão no [contrato implementado](../metadata/anilist-client.md).

O provider tem gate desativado por padrão e nenhum consumidor funcional conectado nesta sessão. O orçamento é compartilhável em processo; persistência e integração de todos os consumidores AniList permanecem nas Sessões 6/9 antes de ativar enriquecimento. Não introduzir agora SQL, workers ou alteração de retries do tracker só para aparentar conclusão desses gates.

## Consequências

Testes do cliente não dependem de conectividade, login, UI nem base do usuário. Há um adaptador compilável para o matcher futuro, sem modificar metadados originais. O uso isolado não prova CA-20 integral: cooldown/circuito/contadores não são duráveis e clientes herdados ainda não compartilham orçamento. Testes de restore, Coil, fila/restart, instalação e performance continuam nas etapas próprias; termos/direitos seguem pendentes para distribuição.
