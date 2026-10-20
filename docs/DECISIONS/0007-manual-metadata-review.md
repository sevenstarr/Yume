# ADR 0007 — Revisão explícita de metadados no primeiro plano

Data: 20/10/2026. Estado: implementado localmente no escopo da sessão 8 autorizada; aceite UI/Android pendente. Código e documentação novos assistidos pelo Codex.

## Contexto

Cliente, matcher, persistência e capas já existem, mas não havia controles para ativar ou corrigir uma associação. O coordenador de lote e o orçamento agregado pertencem à sessão 9. Pin manual, original-only, CAS/epoch e dados herdados precisam conservar sua semântica.

## Decisão

Adicionar configurações pesquisáveis e um painel por anime via navegação Voyager/Metro. `MetadataReviewActions` concentra ações suspensas explicitamente solicitadas, sem instalar worker/gatilho nem habilitar matching textual. Pesquisa produz candidatos; uma leitura por ID produz proposta temporária; confirmação reconhecida grava pin sob revisão/epoch/contexto. Permitir review em original-only preserva a reversão até a escolha final.

Reutilizar SQL/Coil e controle global existentes. Acrescentar ticket de revisão e encerramento condicional, sem DDL ou versão nova. Conservar o estado anterior ao cancelar; leases expiradas só recebem novo orçamento por ação manual explícita. Links seguem gramática restrita e viram IDs, nunca destinos de HTTP. Preview de miniatura usa CDN neutro e não persiste bytes. Preferência móvel é armazenada, sem ativar lote ainda indisponível.

## Consequências

Falso positivo pode ser removido/corrigido sem editar SQL; pin anterior permanece até confirmação. Off/clear cancelam consultas; erros sanitizados e escopo de limpeza são localizáveis. Falta de quota agregada continua gate para lote/distribuição, sem alegar conclusão da sessão 9. Testes de domínio/JDBC não substituem Android/TalkBack/performance; limites detalhados no [contrato](../metadata/manual-review.md).
