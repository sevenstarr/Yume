# Segurança do Yume

## Versões e canal de relato

Não existe release Yume nem versão declarada como suportada. Builds e testes de desenvolvimento têm evidências próprias em `docs/`; não equivalem a uma certificação de segurança ou ao aceite completo Android.

O [canal privado de vulnerabilidades](https://github.com/sevenstarr/Yume/security/advisories/new) foi habilitado e confirmado pela API em 21/10/2026, sob autorização do proprietário. Use **Security → Report a vulnerability**; não publique detalhes exploráveis em uma issue nem envie vulnerabilidades específicas do Yume aos canais upstream. A configuração confirma a disponibilidade do canal, sem prometer SLA de monitoramento/resposta.

Um relato deve conter a versão/commit, ambiente, impacto, passos mínimos e evidências com dados sensíveis removidos. Não envie tokens, keystores, senhas, backups pessoais ou dumps completos de banco. Prazos de resposta e política de versões suportadas serão definidos antes da primeira distribuição.

## Desenvolvimento e distribuição

- Mantenha credenciais e material de assinatura fora do Git. `.gitignore` é prevenção, não substitui revisão do conteúdo e histórico.
- Se um segredo for publicado, revogue/rotacione primeiro e coordene a limpeza com o proprietário; apagar o arquivo no commit seguinte não remove o histórico.
- Não executar código de PR externo com segredos de assinatura. Os workflows herdados ainda precisam da revisão prevista na sessão 11.
- Nenhuma chave de release foi criada. Não distribuir builds que usem a identidade ou o updater do Animiru como se fossem Yume.
- O enriquecimento implementado é opt-in e usa transporte público/CDN separado dos headers de fontes e tokens de trackers. Veja [dados e privacidade](docs/PRIVACY.md); revisões adicionais e validação Android permanecem necessárias.
