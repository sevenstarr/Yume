# ADR 0002 — Camada separada de enriquecimento de metadados

Data: **19/10/2026**. **Status: aceito como arquitetura da Etapa B pelo proprietário.** Trabalho assistido pelo Codex/IA. Sem implementação.

## Registro de aprovação

Em 19/10/2026, após o commit `cb86e7ff7471b4d2c0bdbc952b51b6ba6d722bd0`, o proprietário solicitou: “Usando a arquitetura aprovada na Etapa B, produza a especificação funcional detalhada da Sessão 3.” Essa instrução registra a aprovação da direção arquitetural e autoriza a especificação e seu commit local. A [especificação funcional](../metadata/functional-specification.md) detalha requisitos, contratos, modelos, fluxos, aceite, testes, tarefas e pendências; seu aceite funcional e a execução das sessões seguintes não são presumidos.

As seções abaixo preservam o contexto da proposta original. Onde mencionam aprovação pendente, esse estado histórico foi superado por este registro. Calibração, allowlist, testes e condições de distribuição continuam pendentes nos gates próprios.

## Contexto

A [Etapa A](../metadata/current-architecture.md), no commit `4fdc01724`, encontrou identidade e detalhes de fontes, capas manuais, pipeline Coil, tracking autenticado e consultas auxiliares AniList, mas nenhum sistema de associação externa para capas. Os requisitos da Sessão 3 pedem contratos, precedência manual, persistência, estados e fallback antes de implementar. O pedido atual autoriza a proposta e seu commit documental.

## Decisão proposta

Adotar a [especificação de arquitetura](../metadata/proposed-architecture.md) como proposta canônica para revisão:

1. Overlay opt-in, desligado por padrão, nos módulos existentes `domain`, `data` e `app`; preservar originais, personalizações, fontes, episódios, downloads e progresso.
2. AniList público como único provedor do MVP, com adapter independente de OAuth/tracking e transporte neutro. Outros provedores exigem revisão própria, sem fallback entre catálogos.
3. Identidade local por `Anime.id`, reconciliação por fonte/URL/contexto e identidade remota por provedor/ID. Matching conservador e determinístico; resultados ambíguos exigem escolha humana. Automação textual só após calibração e validação separadas.
4. Associação/pin duráveis, snapshots e buscas descartáveis, SQLDelight aditivo, revisões/generation/epoch para impedir escrita obsoleta. Nenhuma alteração de esquema executada neste ADR.
5. Resolver local observável e plano imutável para Coil: manual > externa manual > externa automática validada > original. Modo original explícito; fallback inclui erro de decoder. Bytes externos permanecem no cache Coil com chaves e limpeza seletivas.
6. Coordenador cancelável e fila própria, orçamento AniList compartilhado entre consumidores, retries finitos, erros tipados e lote separado do opt-in básico.
7. Migração aditiva posterior à 147, com numeração reconfirmada; backup opcional de decisões locais, sem catálogo/cache ou binários, com remapeamento e conflitos explícitos.

## Alternativas e consequências

Gravar diretamente título/capa da fonte comprometeria reversão e refresh; reusar o tracker imporia autenticação indevida; primeiro resultado/fuzzy score isolado agravaria falsos positivos; novo backend/módulo agregador adicionaria infraestrutura sem necessidade no MVP. Comparações detalhadas, inclusive AniList/Kitsu/TMDB e opções de cache/Coil, estão nas seções 3 e 11 da proposta.

Aceitar esta direção implica menor cobertura automática em fontes com poucos atributos e maior cuidado com estados, invalidação e observação visual. Em troca, escolhas são auditáveis, erros externos não alteram identidade e a integração preserva fronteiras herdadas.

## Condições de aceite e execução

Revisar a proposta completa, contratos e plano de migração; calibrar matching antes de ativar automação textual; executar a estratégia de testes nas sessões correspondentes. Confirmar allowlist e adequação aos termos antes do uso público em escala. A aprovação deste ADR, quando ocorrer, não comprova testes nem autoriza publicação. O status permanece **proposto** até decisão explícita; esta entrega termina na documentação e commit local.
