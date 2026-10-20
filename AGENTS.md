# Protocolo obrigatório de continuidade entre sessões

Antes de iniciar qualquer sessão de desenvolvimento:

1. Leia "prompt.md", "docs/PROGRESS.md" e os documentos de arquitetura relevantes.
2. Execute "git status" para verificar o estado do diretório de trabalho.
3. Execute "git log -5 --oneline" para identificar os últimos commits.
4. Execute "git show --stat HEAD" para compreender o escopo do último commit.
5. Quando necessário, execute "git show HEAD" e inspecione os arquivos modificados para compreender decisões de implementação.
6. Verifique se os requisitos e critérios de aceitação da sessão anterior foram cumpridos.
7. Identifique dependências, pendências e possíveis conflitos com a sessão atual.
8. Não modifique ou descarte alterações não commitadas sem autorização.
9. Caso encontre inconsistências críticas na implementação anterior, informe o problema antes de prosseguir.

Ao finalizar cada sessão:

1. Execute os testes e verificações aplicáveis.
2. Revise o diff completo das alterações realizadas.
3. Atualize "docs/PROGRESS.md" com as implementações, decisões, testes, limitações e pendências.
4. Crie um commit contendo exclusivamente as alterações pertinentes à sessão.
5. Utilize uma mensagem de commit descritiva, seguindo a convenção adotada no projeto.
6. Informe o hash do commit criado, os arquivos alterados e o resultado dos testes.
7. Aguarde autorização para iniciar a próxima sessão.

Regra fundamental: nunca considere uma sessão concluída apenas porque existe um commit. Confirme que seus critérios de aceitação foram satisfeitos.

# Protocolo de gerenciamento de branches, sincronização e integração Git

Este protocolo complementa a continuidade entre sessões. A conclusão e o commit local não autorizam publicação, abertura de PR, integração ou início da próxima sessão.

## 1. Estrutura de branches

- `yume-main`: branch principal, destinada exclusivamente a versões estáveis e releases aprovadas.
- `yume-develop`: branch de integração, contendo funcionalidades concluídas, testadas e aprovadas para desenvolvimento.
- `feature/session-XX-descricao`: branches temporárias para funcionalidades de cada sessão, com número de dois dígitos.
- `fix/descricao`: branches para correções isoladas de bugs.

Toda branch temática deve ser criada a partir da versão mais recente de `origin/yume-develop`. Não realizar commits diretamente em `yume-develop` ou `yume-main`.

## 2. Inicialização de uma sessão

Antes de qualquer implementação:

1. Executar `git fetch origin` para atualizar as referências remotas.
2. Verificar se `origin/yume-develop` está disponível; se o fetch falhar, não presumir que a referência local está atualizada.
3. Criar uma nova branch temática baseada diretamente em `origin/yume-develop`.
4. Confirmar que contém todas as funcionalidades previamente integradas.
5. Inspecionar `AGENTS.md`, `prompt.md`, `docs/PROGRESS.md` e a arquitetura relevante, além de executar as verificações de continuidade acima.

```sh
git fetch origin
git switch -c feature/session-09-sync origin/yume-develop
```

Não iniciar uma nova sessão a partir de uma branch temática anterior, salvo autorização explícita. Preservar alterações não commitadas; não trocar a base nem transportá-las para outra sessão silenciosamente.

## 3. Desenvolvimento e validação

1. Respeitar exclusivamente o escopo autorizado da sessão.
2. Realizar commits pequenos, descritivos e logicamente organizados.
3. Preservar funcionalidades existentes.
4. Executar os testes apropriados para os componentes modificados.
5. Atualizar `docs/PROGRESS.md` com decisões, alterações, testes, riscos e pendências.
6. Não realizar commits diretamente nas branches de integração ou principal.

A conclusão da implementação não autoriza automaticamente sua integração.

## 4. Sincronização com yume-develop

Antes de abrir ou atualizar uma pull request, incorporar a versão mais recente da integração:

```sh
git fetch origin
git switch feature/session-09-sync
git merge origin/yume-develop
```

Caso ocorram conflitos:

1. Identificar os arquivos e componentes afetados.
2. Resolver preservando o comportamento existente e as novas funcionalidades.
3. Não descartar alterações de outras sessões sem justificativa.
4. Solicitar autorização quando houver conflitos de requisitos ou decisões arquiteturais.
5. Executar novamente os testes relevantes.
6. Registrar as resoluções em `docs/PROGRESS.md`.

Não utilizar `git push --force` como procedimento padrão de sincronização.

## 5. Critérios para abertura de pull requests

- Base: `yume-develop`.
- Compare: branch temática da sessão.

Abrir uma PR somente quando o escopo autorizado estiver implementado, o build e os testes executáveis relevantes estiverem aprovados, a branch estiver sincronizada com a versão conhecida mais recente de `origin/yume-develop`, a documentação estiver atualizada e não houver conflitos de merge conhecidos. Documentar limitações e verificações não realizadas.

A descrição deve apresentar mudanças, evidências, riscos, testes e critérios de aceite. Abrir a PR somente com autorização do proprietário, conforme o protocolo original. O envio ao remoto também permanece sujeito ao `prompt.md`.

## 6. Proteção e integração

Configurar regras de proteção para `yume-develop` e `yume-main`, quando disponíveis e mediante autorização para alterar a configuração do repositório, conforme `prompt.md`:

- Exigir pull requests para integração.
- Exigir aprovação das verificações automatizadas obrigatórias.
- Exigir atualização da branch antes do merge, ou mecanismo equivalente de validação da integração.
- Impedir bypass das proteções quando possível.

Nenhuma PR deve ser integrada enquanto houver verificações obrigatórias pendentes ou falhando. Se `yume-develop` receber novos commits durante a revisão, atualizar e revalidar a branch temática antes da integração. O merge somente deverá ocorrer após autorização do proprietário.

## 7. Encerramento da sessão

O encerramento local segue o protocolo de continuidade acima, com testes, revisão integral do diff, documentação e commit exclusivo da sessão. Se a integração ainda não foi autorizada ou executada, registrar essa pendência sem declarar a sessão integrada.

Após a integração bem-sucedida:

1. Confirmar que os commits foram incorporados à `yume-develop`.
2. Verificar o resultado das verificações automatizadas relevantes.
3. Confirmar o estado final da documentação.
4. Excluir a branch temática somente se autorizado e sem dependências pendentes.
5. Utilizar a versão atualizada de `origin/yume-develop` como base da próxima sessão, após autorização para iniciá-la.

Nunca excluir uma branch que contenha alterações ainda não integradas sem autorização explícita.

## 8. Promoção para yume-main

`yume-main` não deve receber implementações experimentais diretamente. Promover alterações de `yume-develop` somente após conclusão dos critérios de aceite aplicáveis, validação de regressão, revisão de documentação e versionamento e aprovação explícita do proprietário.

A criação de tags, releases e publicação de APKs permanece sujeita às regras de autorização do `prompt.md`.

## 9. Princípio fundamental

Cada sessão deve ser desenvolvida isoladamente, validada contra a versão atual da branch de integração e incorporada sem comprometer funcionalidades anteriormente aprovadas.

A ausência de conflitos Git não equivale à ausência de regressões funcionais. Testes automatizados e verificações de integração continuam obrigatórios conforme a infraestrutura disponível.

## Localização dos documentos neste workspace

O repositório Git fica em `Animiru/`. O prompt mestre existente está em `C:/Users/Administrator/Downloads/prompt.md`; enquanto não houver uma cópia no repositório, ler esse arquivo. O progresso e a arquitetura canônicos são `Animiru/docs/PROGRESS.md` e `Animiru/docs/ARCHITECTURE.md`. O `AGENTS.md` da raiz do workspace deve permanecer sincronizado com o `AGENTS.md` versionado no repositório.
