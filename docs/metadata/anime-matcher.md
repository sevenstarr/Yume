# Normalização e correspondência — Sessão 5

20/10/2026. Implementação nova assistida pelo Codex, sobre `74a670cc0eb27563a80f54fe0378ab95dc6e5ce9`. Escopo S5-01/S5-02 e T-04/T-05/T-06 da [especificação](functional-specification.md). Decisões em [ADR 0004](../DECISIONS/0004-conservative-anime-matcher.md).

## Contrato implementado

`domain/.../metadata/model/MetadataMatch.kt` contém identidade local, escopo, associação, entrada, configuração, evidências e decisão. `AnimeMatcher`, `MetadataTitleNormalizer` e `MetadataManualSelection` são funções de domínio sem rede, SQL, dependência Android direta, tradução ou alteração do tracking. Não há consumidor ativo nem registro de matcher na DI nesta sessão.

O chamador fornece o conjunto inteiro de até 80 candidatos e a completude agregada de **todas** as variantes/páginas planejadas. `searchComplete=false` representa falha, truncamento ou orçamento insuficiente; vazio incompleto nunca produz negativo. A execução das até quatro buscas e uma leitura adicional continua responsabilidade do futuro coordenador. Duplicatas idênticas são reduzidas por ID; cópias divergentes do mesmo ID são retiradas e tornam a busca incompleta. Isso inclui divergências de campos visuais, uma escolha conservadora que poderá aumentar a revisão. Ordenação por veto, score, provedor e ID é estável e não depende da ordem de chegada.

`LocalAnimeIdentity` usa ano/formato somente quando disponíveis em contexto estruturado confiável. Não extrai ano do título nem episódios da extensão. Escopo padrão `UNKNOWN` impede automação; `WORK`, `CHILD_SEASON` e `COLLECTION` são explícitos. Filho exige número de temporada confiável e marcador remoto concordante. `season` AniList é ignorado pelo matcher porque indica estação de lançamento. Ano remoto usa `startDate.year`, com fallback para `seasonYear`; discordância entre os dois veta automação.

Resultados: `AUTO_MATCH`, `AMBIGUOUS`, `INSUFFICIENT_EVIDENCE`, `NOT_FOUND`, `INCOMPLETE_SEARCH`, `REFRESH_EXISTING`, `REVIEW_EXISTING` e `ORIGINAL_ONLY`. Somente `AUTO_MATCH` expõe `automaticId`. O resultado inclui motivo, fingerprint, versões e `retainedId` quando houver vínculo. Confiança final alta só acompanha `AUTO_MATCH`; confiança individual do candidato descreve seus sinais, sem autorizar associação. Scores são unidades de ranking, não percentuais de certeza. Vetos/rejeições permanecem no diagnóstico; um futuro consumidor deve ocultar rejeitados da lista normal e reexibi-los apenas por ação manual.

## Normalização `title-nfc-v1`

- Preserva `raw`; aplica NFC, espaços Unicode equivalentes, caixa `Locale.ROOT` e separação de pontuação, sem apagar palavras ou números.
- Retira repetidamente apenas sufixos ancorados `legendado`, `dublado`, `subbed`, `dubbed`, `HD`, `480p`, `720p`, `1080p`, `2160p`, com espaços, delimitadores ou colchetes/parênteses. Nunca retira o título inteiro. Não remove conteúdo genérico de parênteses nem palavras no meio do título.
- Produz no máximo duas consultas determinísticas: título conservado e título sem sufixo, quando diferentes. NFKC e remoção de acentos são apenas comparação auxiliar; não geram novas consultas nem equivalência automática. Não há transliteração inferida.
- Preserva `II`, números, anos, subtítulos, `Final`, `Movie`, `OVA` e `Special`. Extrai edição somente em contextos reconhecidos: `Season N`, `Nth Season`, `Temporada N`/`N Temporada`, `Part`/`Parte`/`Cour`, palavras de edição e numeral romano final. Romano final é discriminador, **não** inferência de número de temporada. Conversões não reconhecidas resultam em abstenção.
- Ano/formato incompatíveis, coleção, edição conflitante ou ID AniList inválido vetam automação. Um alias sem marcador não pode esconder edição ou números de outro título. Edições diferentes entre aliases são conflito. Ausência é registrada separadamente e não pontua como concordância.

Limites explícitos: título local/remoto de até 500 unidades UTF-16, URL original até 4096, até 103 títulos por candidato (três principais + cem sinônimos), ano estruturado 1–9999 e temporada positiva. Entradas excedentes geram `IllegalArgumentException`, sem truncar a identidade silenciosamente. IDs aceitos nesta versão são apenas namespace `anilist` e inteiro positivo canônico de 32 bits. Os limites integram a parcela de P-04 resolvida nesta sessão; DDL/DTO persistente continuam futuros.

## Ranking e suficiência `matcher-v1`

Melhor título entre romaji, inglês, nativo e sinônimos: igualdade conservadora vale o peso inteiro; igualdade NFKC vale 0,9; remoção de acentos 0,8; demais comparações usam Dice de conjuntos de tokens multiplicado por 0,7. Essas constantes definem sinais auxiliares do algoritmo v1, sem interpretação probabilística. Ano e formato somam seus pesos somente por igualdade conhecida. Edição atua como guarda de suficiência/veto, sem bônus numérico.

Suficiência v1 exige igualdade conservadora, **ano e formato conhecidos e concordantes**, escopo de obra/filho conhecido, edição explícita concordante quando presente e título com mais de três pontos de código. Siglas em caixa alta com até seis unidades UTF-16 exigem revisão. Ausência de contexto, aproximação, largura Unicode ou acentos isolados não autorizam automação, qualquer que seja o score. Um título canônico/alias exato acompanhado desse contexto pode ser elegível; a suíte não prova que o contexto de uma extensão real será suficiente ou correto.

Busca íntegra sem candidato acima do limiar de revisão e sem vetos elegíveis produz `NOT_FOUND`. Com candidato plausível sem suficiência, retorna revisão por evidência insuficiente. Margem insuficiente ou empate retorna `AMBIGUOUS`. Correção de vínculo e bloqueio após remoção nunca retornam `AUTO_MATCH`.

## Calibração reproduzível e gate

Fixtures TSV versionadas em `domain/src/test/resources/metadata/`: `calibration.tsv` e `validation.tsv`. São cenários **sintéticos**, com IDs e atributos editoriais, sem consultas ao catálogo, dados pessoais ou promessa de correspondência a obras reais. Contêm japonês, português, inglês e romaji, acentos/largura, distribuição, números/romanos, filhos, partes/cour, filme/TV, OVA/especial, remake/recap/final, escopo desconhecido/coleção, rejeição, empates, proximidade e buscas incompletas. Validação usa outras identidades/títulos e inclui candidatos falsos deliberados; rótulos e fixtures foram elaborados nesta mesma implementação, sem anotação humana independente.

`MetadataCalibrationTest` escolhe **somente no conjunto de calibração** uma grade de pesos título {50,60,70}, ano {15,20}, formato {10,20}, limiar alto {70,80,90}, revisão {30,40}, margem {5,10,20}, descartando limiares acima da soma possível. Exclui configurações com qualquer automação contrária ao rótulo, maximiza acertos positivos e desempata por menor soma de pesos, maior limiar alto, menor margem e menor limiar de revisão. A regra de suficiência e os fatores auxiliares são fixos no algoritmo v1, não aprendidos pela grade. Esta calibração limitada seleciona uma proposta de ranking; não estima probabilidades ou desempenho de produção.

`s5-calibration-v1`: título **50**, ano **15**, formato **10**, limiar alto **70**, revisão **30**, margem **10**. Nenhum rótulo de validação participa da escolha. Relatório por caso e totais em [SESSION_5_MATCHING.txt](../evidence/SESSION_5_MATCHING.txt); comandos/resultados em [SESSION_5_COMMANDS.md](../evidence/SESSION_5_COMMANDS.md).

`ProposalV1.automationApproved=false` e configuração ausente impedem toda associação textual automática. Testes usam uma cópia habilitada para medir o que seria associado. Passar nas fixtures não altera esse default nem ativa o provider/UI. P-01 tem parâmetros propostos e evidência de regressão sintética; revisão de composição/suficiência e eventual habilitação continuam pendentes. Amostra pequena e estruturada não demonstra zero falsos positivos no catálogo real. Não foi medida cobertura de linhas/branches.

## Identidade estável e seleção manual

Fingerprint `identity-v1`: SHA-256 de campos UTF-8 prefixados pelo comprimento em bytes e `:`, com comprimento `-1` para nulo, nesta ordem: versão de fingerprint, normalização, animeId, sourceId, URL **exata**, título **bruto**, ano, formato, escopo, parentId e seasonNumber. Inteiros usam decimal, enums usam nome. Output `identity-v1:` + 64 hex minúsculos. Toda mudança nesses campos é material por política conservadora v1, incluindo mudanças cosméticas de título/URL. Capas não entram no fingerprint; versões de matcher/configuração são campos distintos da decisão/chave de cache futura. Hash não deve ser tratado como anonimização nem registrado em telemetria nova.

Vínculo existente renova somente seu ID, mesmo ausente dos resultados novos. Pin manual conserva ID diante de mudança material e informa o motivo; vínculo automático com fingerprint divergente exige revisão sem troca. Correção gera proposta mantendo `retainedId`. `ORIGINAL_ONLY` prevalece. Sem gravação, o matcher não oferece garantia transacional: a Sessão 6 deverá persistir decisão/pin e conferir revisão/generation/epoch atomicamente.

`MetadataManualSelection.propose` aceita um `MetadataResult.Success` do provider cuja identidade esteja íntegra, ID canônico exatamente igual ao solicitado e ANIME já validado pelo contrato do cliente. Diferenças de título/contexto/vetos exigem `differencesAcknowledged=true`. Resultado parcial só de imagem não impede identidade válida. Falha, ausência, identidade parcial ou outro ID não produzem pin. O contrato opera sobre ID, não sobre URL arbitrária; parser de links/UI permanece P-03/Sessão 8. Rejeição pode ser superada apenas no fluxo manual explícito do futuro chamador.

## Continuidade

Sessão 6: persistência/cache/migração/backup, incluindo pins, rejeições, invalidação e confirmação condicional. Sessões 8/9: UI, proveniência de contexto/tracker, coordenador de pesquisa, limites agregados e sincronização. Não houve mudança nos dados originais, tracking, player, extensões, SQL, backup ou fluxo visual. Lint integral e testes em Android continuam com as limitações registradas nas sessões anteriores.
