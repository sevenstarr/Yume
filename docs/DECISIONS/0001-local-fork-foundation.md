# ADR 0001 — Fundação local com histórico upstream preservado

Data: 19/10/2026. **Status: adotado para organização local.** Publicação remota e identidade de runtime permanecem pendentes. A proposta de arquitetura de metadados da sessão 0 não é transformada em contrato aceito por este ADR.

Atualização posterior, na mesma data: criação do fork público `sevenstarr/Yume` autorizada pelo proprietário e executada, com origem verificada e remote `origin` configurado. O envio dos commits locais e a identidade de runtime continuam pendentes. O contexto e as decisões abaixo registram o momento da fundação inicial.

## Contexto

O proprietário autorizou iniciar a sessão 1 e confirmou o nome Yume. O workspace contém um clone do Animiru e relatórios da sessão 0 fora do Git. Conta de destino, identidade visual e pacote final ainda não foram confirmados.

## Decisão

- Reutilizar o clone, preservar SHA/autoria upstream e obter o histórico completo sem mover a base auditada.
- Criar `main` no SHA `0b9bb0a347aa7bcee226d8782d98d9abafc34b32` e trabalhar em `chore/session-1-foundation`.
- Nomear o remote da origem `upstream`; adicionar `origin` somente após confirmar/criar o fork real.
- Versionar os relatórios relevantes da sessão 0 em `docs/` e os snapshots históricos de README/CONTRIBUTING/CODE_OF_CONDUCT em `docs/upstream/`.
- Usar Yume em documentação/templates e manter código, banco, dependências, recursos e workflows inalterados nesta fundação.
- Identificar os commits locais do Codex como automação, sem usar a identidade GitHub do proprietário como autor presumido.

## Consequências

O diff é pequeno por categoria e revisável; o build de referência ainda pode usar a base exata. A pasta local continua chamada `Animiru` para evitar movimentação desnecessária. A branch `main` não recebe a fundação automaticamente: integração e push dependem de revisão/autorização. O aceite integral da sessão 1 não pode ser declarado enquanto faltam o destino do fork e a revisão das decisões de identidade/publicação.
