# Provedor, imagens e contas externas

21/10/2026. Revisão preliminar de prontidão, assistida pelo Codex; não é confirmação jurídica nem autorização do provedor.

## Evidência consultada

Os [termos oficiais AniList](https://github.com/AniList/docs/blob/master/docs/guide/terms-of-use.md) e a [política oficial de limites](https://github.com/AniList/docs/blob/master/docs/guide/rate-limiting.md) foram relidos nesta rodada. Os termos restringem coleta em massa, uso como armazenamento/backup e serviços concorrentes de listas/tracking. Também estabelecem condições comerciais; a situação comercial do mantenedor não foi inferida. A política de limites mantém aviso de capacidade reduzida de 30 requisições por minuto.

O código novo consulta somente os itens solicitados ou pertencentes à biblioteca sob opt-in, guarda decisões locais e cache limitado, e não implementa catálogo espelhado. O orçamento físico compartilhado de 12/min é mais conservador que o teto publicado; essa margem não elimina políticas ou a necessidade de tratar respostas 429. O namespace público e os hosts de imagens permanecem restritos aos já verificados e testados, sem ampliar a allowlist nesta rodada.

O aplicativo herdou recursos de biblioteca e tracking. Esses fatos impedem concluir automaticamente que o produto está fora da restrição relativa a serviços concorrentes. A documentação da API tampouco fornece uma licença geral sobre todas as imagens retornadas. Não foram incorporadas capas de catálogo ao repositório, nem enviado contato ao provedor.

**Gate de distribuição mantido:** obter confirmação de enquadramento e das condições aplicáveis antes de escala/distribuição. A autorização do proprietário para trabalhar no repositório não equivale à autorização do AniList ou dos titulares das imagens.

## Minuta para revisão do proprietário

Destinatário a confirmar no canal oficial do provedor. Nenhum envio realizado.

> Assunto: Enquadramento do uso da API e de capas no projeto Yume
>
> Estamos desenvolvendo o Yume, um fork Android público derivado do Animiru/Aniyomi. O aplicativo reúne uma biblioteca local e possui integrações de tracking herdadas. O incremento novo permite pesquisar e escolher metadados e capas do AniList para itens dessa biblioteca, com ativação explícita, seleção manual para ambiguidades, cache limitado e um orçamento compartilhado de 12 requisições por minuto. Não mantemos um catálogo espelhado nem distribuímos imagens de catálogo no repositório.
>
> Antes de distribuir esse incremento, gostaríamos de confirmar se esse uso é permitido pelas restrições sobre serviços concorrentes e quais condições se aplicam ao cache/exibição de capas. Podemos fornecer os detalhes técnicos e o repositório público para avaliação. Também precisamos esclarecer eventuais exigências de atribuição e licenciamento. A situação comercial e o canal de distribuição serão informados pelo mantenedor.

Enviar a mensagem exige autorização específica de comunicação e conta remetente escolhida pelo proprietário. Não inventar receita, direitos ou compromissos.

## OAuth herdado

Trocar o pacote Android não cria clientes OAuth nos provedores. O manifesto/trackers conservam os callbacks herdados `animiru://`, que exigem inventário e registros próprios antes de distribuição; não foi feita substituição cega que quebraria login. Credenciais pessoais/tokens não devem ser pedidos ou versionados. A validação real de login e coexistência continua separada dos testes de build.

Os cadastros externos são uma dependência não resolvível apenas por modificação local. A assinatura de release deverá permanecer sob controle do proprietário, fora do Git; nenhuma chave foi criada nesta rodada.
