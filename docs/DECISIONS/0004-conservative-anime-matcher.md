# ADR 0004 — Matcher conservador e proposta de calibração

Data: 20/10/2026. Status: implementação local adotada para S5-01/S5-02; parâmetros propostos, automação textual desativada. Pedido do proprietário: executar a sessão 5 e fazer commit depois. Código/documentação novos assistidos pelo Codex.

## Problema e decisão

Resultados textuais de AniList não bastam para identificar uma temporada/edição. O domínio recebe identidade original, candidatos e completude agregada e emite uma decisão pura, com evidências, vetos, motivo e versões. Normalização não destrói números nem palavras de edição. Ano/formato confiáveis são exigidos além de igualdade conservadora; busca incompleta, coleção, empate, rejeição ou ausência de contexto não autorizam associação.

O fingerprint usa serialização por comprimento, SHA-256 e versões explícitas. Todos os campos originais/contextuais definidos em [anime-matcher.md](../metadata/anime-matcher.md) são materiais; nenhuma revisão de capa entra no hash. Não normalizar URL evita unir itens distintos.

Associações aceitas são estáveis. Atualização conserva o ID, mudança material de associação automática exige revisão e correção mantém a escolha anterior até confirmação. Seleção manual requer resposta de identidade íntegra do provider e reconhecimento das diferenças; persistência atômica será implementada na Sessão 6.

Uma busca determinística na grade de parâmetros utiliza apenas fixtures de calibração; outro arquivo valida o resultado. Configuração `s5-calibration-v1` é uma **proposta desativada**. A grade não aprende suficiência nem fatores auxiliares v1. Sucesso em cenários sintéticos não aprova ativação ou demonstra generalização para títulos reais. Registros por caso, cobertura e abstenção acompanham a entrega.

## Consequências

- Sem rede/SQL e sem nova dependência, os testes são determinísticos e reproduzíveis.
- A exigência de ano/formato/escopo confiáveis resulta em muitas abstenções; `SAnime` sem esses atributos seguirá para revisão.
- Aliases contraditórios, números diferentes e duplicatas divergentes falham conservadoramente. Não há tradução livre, equivalência presumida por popularidade ou tolerância de ano.
- Não há integração ativa, persistência, worker ou mudança da biblioteca. Ativação depende da revisão do gate P-01 e das integrações futuras; `automationApproved` continua false na proposta versionada.
- Limites de texto/fingerprint estão definidos para o domínio; DDL, payload persistente e garantias concorrentes de P-04 continuam na próxima sessão.
