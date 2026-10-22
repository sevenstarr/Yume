# Marca Yume

Direção aprovada pelo proprietário em 21/10/2026: símbolo ¥, preto e amarelo, minimalista. Construção vetorial original assistida pelo Codex; não deriva dos traços do ícone upstream. O símbolo é uma referência visual escolhida pelo proprietário, sem afirmação de registro ou exclusividade sobre o caractere monetário.

- Preto: `#080808` (RGB 8, 8, 8).
- Amarelo: `#FFD600` (RGB 255, 214, 0).
- [Master SVG](../.github/assets/logo.svg) e [PNG 512](../.github/assets/logo.png). Sem fonte externa: o símbolo usa três polígonos geométricos.
- Launcher adaptativo: fundo preto e símbolo amarelo, sem dupla aplicação de inset. Marca dentro da zona central segura de 66/108; launcher redondo usa a mesma geometria.
- Ícone temático e notificações: silhueta monocromática; o sistema/tela controla a cor. Splash: amarelo sobre preto. Os nomes legados dos recursos foram mantidos para preservar consumidores internos.

Manter proporção quadrada, as duas barras horizontais e margem livre; não esticar ou introduzir gradientes/sombras. Para aplicações externas, usar o fundo preto para preservar contraste. O conjunto foi inspecionado localmente em 16, 32, 64, 128 e 256 px; a renderização Android, máscaras de fabricantes e ícones temáticos ainda precisam de aparelho. Exportações auxiliares locais não são dependência do build.

Identidade Android: `io.github.sevenstarr.yume`, sufixos de variantes preservados e namespace interno `eu.kanade.tachiyomi` conservado. A identidade visual não muda automaticamente todos os temas escolhidos pelo usuário. Updater desabilitado em todas as distribuições até um canal próprio aprovado.
