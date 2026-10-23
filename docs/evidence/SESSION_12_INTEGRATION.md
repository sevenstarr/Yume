# Publicação e integração da sessão 12

22/10/2026, America/Sao_Paulo. Proprietário autorizou encerrar PR #5, publicar
a sessão 12 e realizar merge em `yume-develop`. Continuação da mesma sessão.

## Base e operações

- PR substituta criada: [#6](https://github.com/sevenstarr/Yume/pull/6), base
  `yume-develop`, compare `feature/session-12-public-docs`, cabeça inicial
  `432b3d00cdb6af01fcb23a3e798fe214810596f9`; seis commits/75 arquivos.
  Branch enviada sem force-push e PR anexada ao chat. Atualização documental
  posterior registra o vínculo antes de validar a cabeça final para merge.
- Mapa externo lido por contexto/resumo e subgrafo de workflow/build, anterior
  às mudanças atuais. Documentos/fontes/Git atuais conferidos; mapa fora do Git.
- HEAD inicial `26d290bf4`, branch `feature/session-12-public-docs`, somente
  `docs/prompt.md` não rastreado preexistente, preservado.
- Fetch inicial restrito falhou; repetição autorizada passou. Merge da base
  `origin/yume-develop=25d50a506` retornou `Already up to date`.
- PR #5 confirmada aberta e depois fechada sem merge via conector GitHub,
  conforme pedido. Nenhuma branch removida ou histórico reescrito.

## Correção do bloqueio de CI

O run anterior `39877855871` falhou antes do build por `sdkmanager` fora do PATH.
O [instalador oficial da imagem](https://github.com/actions/runner-images/blob/main/images/ubuntu/scripts/build/install-android-sdk.sh)
define `${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager`; inventários
oficiais Ubuntu 24.04/26.04 listam as ferramentas/SDKs requeridos. A correção
mantém Ubuntu 26.04 e chama esse executável explicitamente nos dois workflows.
Raiz vem de ANDROID_HOME, ANDROID_SDK_ROOT ou padrão da imagem; a presença é
obrigatória. GITHUB_ENV/GITHUB_PATH recebem raiz/caminhos para passos seguintes.
Falha de instalação continua bloqueando o job. Sem autoaprovação de licenças,
nova Action ou mudança em permissões, secrets, checks ou gates de assinatura.

Validação aprovada: actionlint dos dois workflows (label Ubuntu 26.04 declarado
localmente), sintaxe `bash -n` de 14 blocos de execução e quatro casos de smoke
do passo SDK com raiz contendo espaços, fallback de variável, executável
ausente e retorno de erro do instalador. Casos usam executável sintético local,
sem instalar SDK ou chamar AniList. Logs/scripts auxiliares em build ignorado.
Os resultados JVM/Android build anteriores estão separados em
[SESSION_12_COMMANDS](SESSION_12_COMMANDS.md) e no registro da sessão 11; esta
correção de workflow não é nova validação funcional Android.

O primeiro extrator tentou PyYAML, indisponível no runtime; a extração foi feita
somente sobre os blocos `run` conhecidos, com biblioteca padrão, e o YAML inteiro
validado separadamente pelo actionlint. Primeiro smoke Bash restrito falhou por
acesso ao diretório; repetição autorizada passou nos quatro casos. Shellcheck/
pyflakes não executados. `git diff --check` aprovado; diff da correção revisado.

Envio e PR somente após checks locais e revisão do diff. CI remoto será
consultado na cabeça final; merge autorizado condicionado aos checks obrigatórios
e sincronização com a base. Links, cabeça, runs e resultado de integração ficam
na PR e na entrega, sem alegação prévia de sucesso. Não publicar release nem
promover para `yume-main` nesta continuação.
