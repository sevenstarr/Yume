# Sessão 9 — rodada Android parcial

20/10/2026. Testes autorizados no aparelho do proprietário, interrompidos a seu pedido para preservar o uso restante. Não inicia a sessão 10. Trabalho de documentação/testes assistido pelo Codex.

| Verificação | Resultado observado |
| --- | --- |
| Identidade do APK | ARM64 debug, versão 0.20.0.1-8260, SHA-256 `0863d71b91fcdf70077d3cd19bd526ba8e2984cb3ab4f79fa0a6ea7cab85f4c3`, idêntico ao snapshot da sessão 9 |
| Ambiente | Samsung Galaxy A05s, Android 15/API 35; ADB autorizado; USB carregando |
| Banco | Schema 150; integridade SQLite aprovada antes/depois |
| Navegação | Biblioteca, menu, configurações e metadados acessíveis |
| Idempotência | Três lotes, cada um com 6/6 ignorados, zero erros e zero tentativas do provider; geração 10 → 13 |
| Preservação | Todas as tabelas mantiveram seus hashes, exceto as duas tabelas operacionais da fila; inclui 165 registros de animes, 393 episódios, histórico, tracking e decisões/cache de metadados |
| Diário | Configuração existente gerou WorkManager enfileirado, atraso e intervalo de 24 horas, carga e bateria não baixa; execução futura não observada |
| Restauração | Wi-Fi e dados móveis do Android ligados; preferências originais de metadados restauradas; opção original de manter tela ligada restaurada |

O lote anterior do usuário tinha quatro itens para revisão, um ignorado e um erro, com máximo de três tentativas; sua causa não foi diagnosticada nesta rodada. Os novos lotes usaram cache/TTL e não reproduziram o erro. Não limpar cache ou metadados para forçar tráfego sem planejamento.

Achado: o subtítulo de dados móveis ainda informa que sincronização em lote não está disponível. Reproduzível em Configurações → External metadata; chave `metadata_mobile_data_explanation`. Correção pendente.

Os testes de rede foram inconclusivos: Wi-Fi foi desligado, mas o cache permitiu conclusão imediata. Não declarar restrição de conectividade validada. Pausa, retomada, cancelamento, processo morto, reboot, Doze, opt-out, novos favoritos, atualização de ID remoto, playback/download, TalkBack e bateria permanecem pendentes.

Comandos efetivamente usados: `adb devices -l`, consultas de versão/pacote, hash binário do APK instalado, `run-as` para cópia privada dos bancos e preferências específicas de metadados, `uiautomator dump`, `input tap`, `screencap`, `svc wifi disable/enable` e `svc power stayon usb`. Cópias SQLite verificadas localmente com Python `sqlite3` e comparadas por tabela. Não foram executados testes instrumentados, coleta sistemática de crash logs ou medição de performance; não se declara cobertura nem ausência de crashes.

Evidências brutas privadas e roteiro auxiliar: `build/session9-android/` (ignorado). Não publicar bancos, telas de outros apps, títulos pessoais, identificadores do dispositivo ou preferências gerais. A primeira tentativa de navegação coincidiu com uso do celular; os toques foram suspensos, o proprietário liberou o aparelho e o roteiro passou a exigir foco do pacote correto e dump novo antes de coletar UI. Capturas dessa tentativa não são evidência do app.

Verificação desta entrega documental: diff integral e `git diff --check`; sem alteração funcional, sem repetição de build/JVM. Próxima rodada depende da disponibilidade e autorização do proprietário; sem publicação remota nesta rodada.
