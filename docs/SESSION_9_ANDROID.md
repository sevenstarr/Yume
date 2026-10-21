# Sessão 9 — validação no Android

20/10/2026. Rodada inicial parcial seguida de ampliação expressamente autorizada pelo proprietário no seu aparelho. Testes/documentação assistidos pelo Codex; sessão 10 não iniciada. Implementação já integrada pela [PR #2](https://github.com/sevenstarr/Yume/pull/2), merge `4f956a00f231e61856036cad61977457d1fca47d`. Esta rodada não altera código funcional nem instala outro APK.

## Ambiente e método

Galaxy A05s, Android 15/API 35. APK ARM64 debug `0.20.0.1-8260`, SHA-256 `0863d71b91fcdf70077d3cd19bd526ba8e2984cb3ab4f79fa0a6ea7cab85f4c3`, conferido com o snapshot da sessão 9. ADB autorizado, navegação UIAutomator condicionada ao foco do pacote correto e a um dump novo. SQLite schema 150, cópias privadas com WAL e verificação de integridade antes/depois. Agregados e referências às evidências privadas em [SESSION_9_ANDROID.json](evidence/SESSION_9_ANDROID.json).

O proprietário autorizou consultas reais dos favoritos ao AniList e, separadamente, uma chamada controlada do job diário ignorando prazo/carga. Após rejeição inicial pela revisão automática, a autorização específica foi obtida antes dessa chamada. Não foram alterados relógio ou banco do WorkManager para antecipar o prazo.

## Resultados observados

| Caso | Resultado |
| --- | --- |
| Navegação | Biblioteca, configurações/metadados, detalhes, fontes, histórico e downloads acessíveis |
| Idempotência inicial | Três lotes de seis itens ignorados, zero erros e zero tentativas do provider; cache/TTL |
| Fila sem rede | Seis pendentes, zero tentativas; pausa preservou fila, retomada manteve pendentes e cancelamento marcou os seis como ignorados |
| Reinício do processo | Force-stop e reabertura preservaram pausa e os seis pendentes |
| Opt-out global | Removeu fila e cancelou trabalhos; reativação exigiu confirmação, sem iniciar varredura automática |
| Bateria | Descarga/nível baixo simulados mantiveram lote pendente sem tentativas; liberação da condição permitiu processamento; simulação restaurada |
| Atualização real | Consulta de ID já aceito avançou atualização completa e preservou pin. Item com erro histórico também atualizou; causa anterior desconhecida |
| Revisão manual | Busca retornou candidatos; proposta não substituiu associação. OK desabilitado até reconhecimento; proposta cancelada |
| Preferências | Modo de capa original alternado e restaurado; gatilho de novos favoritos desligado/religado; diálogos de limpeza abertos e cancelados |
| Novo favorito | Item temporário entrou como sétimo pendente enquanto pausado; retirado após teste, sem vincular tracking |
| Reboot com fila | Sete pendentes e pausa persistiram, zero tentativas. Retomada concluiu 7/7 ignorados, zero erros; item retirado descartado sem nova emissão AniList |
| Reboot com lote concluído | Biblioteca, decisões e preferências persistiram |
| Diário | WorkManager com atraso/intervalo de 86.400.000 ms, carga e bateria não baixa. Chamada respeitando restrições aguardou; chamada forçada autorizada recusada pelo WorkManager antes do prazo, sem novo lote |
| Fonte ampliada | Controles visíveis/legíveis em captura a 130%; escala original restaurada |
| Reprodução | Player exibiu vídeo; logs registraram decoder/quadro de vídeo. Cerca de 19 segundos reproduzidos, saída normal |
| Download | Iniciou e atingiu 4%; cancelamento deixou fila vazia. Resíduo vazio específico removido; ausência de arquivo parcial conferida |
| Logs | Seis amostras sem FATAL EXCEPTION; amostras sobrepostas/descontínuas, sem comprovar ausência geral de crashes/ANRs |

## Preservação e restauração

Integridade SQLite aprovada antes/depois. Seis favoritos originais com linhas integralmente preservadas; IDs/pins, rejeições e modos de capa mantidos. Nenhuma linha preexistente de episódios, histórico, tracking, categorias, vínculos de categorias ou botões personalizados removida ou modificada.

A consulta à fonte atualizou um registro público não favorito e acrescentou 37 episódios ao catálogo local (393 → 430). A reprodução acrescentou uma linha de histórico; remoção pela UI limpou sua data, mantendo a linha inativa (5 → 6). Progresso temporário voltou a zero/não visto. Esses efeitos permanecem documentados; não se declara igualdade de todas as tabelas na rodada ampliada.

Estado final: seis favoritos originais, fila de metadados concluída, nenhum download ativo/parcial. Opt-in global e novos favoritos ligados, diário/dados móveis do app restaurados. Android: Wi-Fi/dados móveis ligados, escala 0.8, timeout 60000 ms e manter tela ligada 0, conforme valores originais. Simulação de bateria e força de idle desativadas; app na biblioteca. Tela mantida ligada durante os testes, comportamento original restaurado ao encerrar.

## Limitações e pendências

- Diário natural após 24 horas e restrições do lote criado por esse disparador ainda não observados.
- Doze profundo não alcançado: tentativa parou em QUICK_DOZE_DELAY e foi revertida.
- Sem benchmark de biblioteca grande, bateria prolongada, TalkBack, instalação limpa ou limpeza destrutiva seguida de restauração.
- Reprodução curta não valida áudio, episódio completo ou todos os codecs. Download iniciado/cancelado não valida conclusão nem reprodução offline.
- Alguns testes de rádio concluíram por cache/reconexão; resultados de fila pendente referem-se às cópias que confirmaram pendentes e zero tentativas. Sem estresse/quota real do servidor.
- Achado pendente: subtítulo `metadata_mobile_data_explanation` ainda afirma que sincronização em lote não está disponível. Sem correção funcional nesta rodada.

## Evidências e entrega

Comandos usados incluem consultas ADB de pacote/versão, hash do APK instalado, run-as para cópias dos bancos/preferências pertinentes, UIAutomator, entrada por texto observado, screencap, rádio/tela/bateria, force-stop, reboot, JobScheduler, deviceidle e amostras de logcat/exit-info. Comparação local com Python/sqlite3; avaliador verificou invariantes e gerou JSON agregado. Sem suíte instrumentada Android.

Bancos, telas, logs e auxiliares em `build/session9-android/`, ignorado. Não publicar dados pessoais, identificadores do aparelho ou preferências gerais. Capturas da tentativa inicial coincidente com uso do celular não são evidência do app.

Entrega documental: revisão integral do diff, JSON validado, invariantes aprovadas e git diff --check. Build/JVM locais não repetidos por não haver mudança funcional; evidência anterior de 298 testes e CI permanece separada. Relatórios publicados na branch `fix/session-09-android-validation`; proprietário autorizou PR para `yume-develop` e merge após as verificações aplicáveis. Resultado final da integração registrado na PR. Sem início da sessão 10.
