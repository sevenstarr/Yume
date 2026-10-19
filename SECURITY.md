# Segurança do Yume

## Versões e canal de relato

Não existe release Yume nem versão declarada como suportada. A fundação local ainda não passou pelo build de referência, testes de segurança ou instalação.

O canal privado de vulnerabilidades deverá ser configurado e validado pelo proprietário antes da abertura do projeto ao público. Se o repositório futuro disponibilizar **Security → Report a vulnerability**, use esse canal. Sua existência e monitoramento ainda não foram confirmados. Não publique detalhes exploráveis em uma issue; aguarde um canal privado confirmado, sem enviar vulnerabilidades específicas do Yume para os canais upstream.

Um relato deve conter a versão/commit, ambiente, impacto, passos mínimos e evidências com dados sensíveis removidos. Não envie tokens, keystores, senhas, backups pessoais ou dumps completos de banco. Prazos de resposta e política de versões suportadas serão definidos antes da primeira distribuição.

## Desenvolvimento e distribuição

- Mantenha credenciais e material de assinatura fora do Git. `.gitignore` é prevenção, não substitui revisão do conteúdo e histórico.
- Se um segredo for publicado, revogue/rotacione primeiro e coordene a limpeza com o proprietário; apagar o arquivo no commit seguinte não remove o histórico.
- Não executar código de PR externo com segredos de assinatura. Os workflows herdados ainda precisam da revisão prevista na sessão 11.
- Nenhuma chave de release foi criada. Não distribuir builds que usem a identidade ou o updater do Animiru como se fossem Yume.
- O enriquecimento futuro deve ser opt-in, minimizar dados enviados e evitar transportar headers de fontes ou tokens de trackers ao carregar imagens externas.
