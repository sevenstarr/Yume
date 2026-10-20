# Persistência, cache e backup — Sessão 6

Implementação assistida pelo Codex em 20/10/2026. Contrato em `MetadataRepository`, implementação SQLDelight singleton no módulo `data`, eviction seletiva de Coil no `app`. Nenhum consumidor de matching/coordenador ou request visual externo é ativado por esta entrega.

## Dados e comandos

- `metadata_control`: opt-in off, epoch monotônica, cooldown monotônico por prazo UTC.
- `anime_metadata`: FK com cascade ao anime, payload JSON tipado de decisão e revisão/generation. Inclui origem `(source,url)`, identidade/fingerprint, pin, rejeições (até 100), modo original/bloqueio, evidências versionadas, estado, consulta/tentativa e última falha tipada. Remotos não são globalmente únicos.
- `metadata_work`: um lease de 60 s por item, epoch e até três emissões. Reinício/lease vencida não zera orçamento. Não é ainda a fila de prioridades da sessão 9.
- `metadata_media_cache`: snapshots públicos íntegros compartilhados por provedor/ID; detalhes frescos por sete dias, elegíveis por menos de 30.
- `metadata_lookup_cache`: chave SHA-256 de consulta, página, locale, filtros e versões; fingerprint local; TTL de 24 horas e LRU de 200 páginas. Negativo só nasce de página 1 íntegra/vazia com término confirmado. Campo ausente/erro/incompletude não produz negativo.
- `metadata_image_index`: chaves externas do Coil, sem bytes. Namespace distingue o remoto, hash da URL e revisão; sem ID local, permitindo compartilhar bytes entre fontes.
- `metadata_restore_pending`: decisões inválidas, inativas, conflitantes ou sem remapeamento. Sua apresentação/solução manual fica na sessão 8.

`begin` verifica o anime original e captura revisão/generation/epoch/fingerprint/stamp original. Deduplica lease e respeita cooldown persistido. `recordEmission` limita a operação a três emissões; não substitui autorização de rede/quota por host. O futuro coordenador deve encerrar operações ou iniciar retry explícito; leases esgotadas não geram loops automáticos.

Seleção manual valida resposta do provider e reconhecimento das diferenças. Refresh aceita somente o ID já associado e payload íntegro. Matcher é recalculado localmente com automação textual desativada, sem aceitar score arbitrário do chamador. Atualizações originais, clear/off, remoção, revisão antiga, lease vencida e exclusão de anime recusam resultados antigos. Escritas e notificações SQL acontecem juntas após commit. `observe(ids)` faz consulta em lote; telas devem compartilhar assinaturas conforme sessão 7.

Leitura/falha não renova TTL nem `lastCheckedAt`. Relógio anterior à data gravada torna o cache inelegível; `prune(now)` remove dados expirados e aplica LRU. O futuro coordenador deve chamar prune durante atividade autorizada; não foi instalado agendamento. Offline pode consultar decisões e snapshots sem dependência de provider/rede. Imagens dependem de bytes existentes no Coil e de integração futura de fallback.

`clearCache` avança epoch, cancela leases locais, remove snapshots/páginas e evicta somente chaves externas indexadas. Preserva pins, rejeições e dados herdados. `clearAll` também desliga e remove decisões/pendências, conservando proteção de cooldown. A barreira `storeImage` registra a chave antes de I/O, verifica elegibilidade novamente e remove bytes de escrita obsoleta/cancelada. Nenhum callback pode publicar seleção visual antes da confirmação; a sessão 7 deve integrar esse contrato em todos os requests externos.

## Backup e recuperação

`Backup.metadataDecisions` usa protobuf 600, bloco v1 com tags explícitas. As opções de exportação/restauração são independentes e inicialmente desmarcadas; exigem biblioteca selecionada. Arrays de opções antigos continuam aceitos. O bloco contém somente fonte/URL/contexto de pai/temporada, provedor/ID, pin/modo/bloqueio/rejeições. Não transporta ID de banco, payload/URLs de catálogo, imagens, erros, orçamento nem preferência global.

Importação ocorre após os jobs de anime/pais. Exige fonte/URL exatas e contexto único; nunca usa `BackupAnime.id` para associar metadados. Decisões existentes do destino prevalecem; duplicatas divergentes no arquivo não dependem da ordem. Provedor desconhecido fica inativo. O bloco é local e nunca ativa o recurso; snapshots não são restaurados e o vínculo exige revalidação. Conflitos são guardados e relatados no log de restore, sem conteúdo sensível de servidor.

O restore herdado foi corrigido em dois pontos: projeção protobuf → domínio e escrita SQL/mescla. Os seis campos `custom*` agora passam pelo fluxo; valores locais prevalecem. Inserção/atualização usam `og*`, evitando gravar título/autor personalizados como originais. Capas locais manuais continuam fora do backup de bytes herdado; este trabalho não promete recuperá-las em outro dispositivo.

A base `data/src/main/sqldelight/148.db` é um snapshot **sintético com esquema e defaults herdados**, produzido pelo gerador SQLDelight e reduzido às tabelas herdadas; não é banco de usuário ou prova de todas as migrações históricas. A verificação do SQLDelight compara upgrade dessa base com schema novo. Testes adicionais povoam registros herdados e exercitam upgrade/persistência/restore.

Recuperação: conservar backup pré-upgrade e exportar decisões antes de descartar dados novos. O SQLDelight não oferece downgrade do app. [rollback-metadata-148.sql](../../scripts/sql/rollback-metadata-148.sql) remove somente estruturas novas em uma **cópia descartável**, preservando as tabelas herdadas; não alterar `user_version` nem executar esse script no banco pessoal. Para falha de upgrade, voltar à cópia integral anterior com a versão compatível do app.

Evidências finais e limites de dispositivo/Coil estão em [SESSION_6_COMMANDS.md](../evidence/SESSION_6_COMMANDS.md).
