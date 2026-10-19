# Checklist de regressão — Sessão 2

Base upstream: `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`. Preencher com SHA do APK, checksum, ABI, Android/API, dispositivo, data e executor. **Nenhum item está aprovado por inspeção estática.** Estado inicial de todos os casos: não executado; consultar `BUILD_BASELINE.md` para o bloqueio vigente.

Usar perfil/emulador descartável, biblioteca sintética e conteúdo de teste autorizado. Não importar backup pessoal, desinstalar o app original ou executar atualização oferecida pelo updater. O pacote debug herdado é `xyz.Quickdev.Animiru.mi.dev`: se já existir, verificar assinatura e dados antes de qualquer instalação. `adb install -r` pode alterar um app com esse pacote; usar somente o emulador dedicado vazio.

| ID | Fluxo e execução | Resultado esperado / evidência a guardar |
|---|---|---|
| M01 | Instalar o APK debug compatível com a ABI em perfil vazio; abrir, fechar e reabrir | Instalação e inicialização sem crash; guardar saída do instalador, versão/assinatura e captura da tela |
| M02 | Navegar biblioteca vazia, categorias, grades e lista; reiniciar | Estados vazios corretos, navegação e preferências preservadas |
| M03 | Em fonte autorizada, buscar por título com e sem resultado e abrir detalhes | Sem crash; título, capa original e episódios disponíveis; distinguir erro da fonte de defeito do app |
| M04 | Adicionar dois animes à biblioteca, favoritar, categorizar, filtrar e ordenar | Entradas e categorias persistem após reinício; nenhum duplicado inesperado |
| M05 | Abrir detalhes, episódios, temporadas e capa; editar informações manualmente | Relações corretas; originais e personalizações preservados após refresh/reinício |
| M06 | Ver lista de extensões; instalar uma extensão de teste confiável e navegar sua fonte | Extensão carrega e busca funciona; registrar versão/repositório sem credenciais; não substituir extensões pessoais |
| M07 | Reproduzir mídia sintética/local autorizada; pausar, buscar posição, retomar e trocar episódio | Player estável, áudio/vídeo sincronizados e posição/progresso corretos após reabrir |
| M08 | Marcar episódio visto/não visto, reproduzir parcialmente e consultar histórico | Progresso, contagens e histórico consistentes após reinício |
| M09 | Atualizar detalhes e biblioteca; repetir com rede indisponível | Atualização não duplica episódios nem perde favoritos/progresso; falha de rede visível e recuperável |
| M10 | Baixar mídia autorizada, pausar/retomar, reproduzir offline e excluir só esse download | Estado/fila coerentes; arquivo offline utilizável; exclusão não remove biblioteca ou histórico |
| M11 | Criar backup sintético com categorias, favoritos, histórico, progresso, temporadas e todos os campos `custom*`; restaurar em segundo perfil vazio | Comparar valores e contagens antes/depois; guardar tabela e hashes dos backups fora do Git; investigar R09, sem assumir que personalizações sobrevivem |
| M12 | Restaurar novamente o mesmo backup sintético e testar relação pai/filho e diferentes fontes com URLs iguais | Sem duplicação indevida ou vínculo incorreto; erro de backup inválido é recuperável |
| M13 | Verificar widgets/notificações com biblioteca de teste e abrir o anime correspondente | Capas e destinos corretos, sem crash; respeitar permissão de notificações |
| M14 | Repetir biblioteca/detalhes/progresso sem rede, com cache e depois sem cache | Dados locais preservados, placeholders/erros coerentes e recuperação ao reconectar |
| M15 | Inspecionar configuração de atualização e identificação do build sem instalar atualização | Registrar endpoint/estado e limitações da identidade herdada; nenhuma release Yume inferida |

## Registro de execução

Para cada caso: `ID | data | SHA/APK SHA256 | dispositivo/API/ABI | passos e dados sintéticos | observado | passou/falhou/bloqueado/não executado | evidência/defeito`.

Build e testes unitários não substituem esta execução. Os testes `MigratorTest` cobrem estratégias de migração do aplicativo; não demonstram migração SQLDelight nem restauração de backup. Não declarar cobertura percentual sem relatório gerado.
