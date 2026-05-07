# PDV – Refactor Técnico

## Objetivos
- Modernizar a UI do PDV mantendo 100% das funcionalidades existentes.
- Aumentar usabilidade, responsividade e performance.
- Introduzir modo de tela cheia dedicado e novos recursos de filtro/ordenação.

## Alterações Principais
- Teclado virtual usando Popover (Atlantafx) com posicionamento configurável.
- Busca com filtro alfanumérico e feedback de contagem de resultados.
- Validação e limites de desconto com política consolidada (DiscountPolicy).
- Ordenação de produtos (Nome, Preço asc/desc) e filtro “Apenas em stock”.
- Ações rápidas de quantidade (+/−) no carrinho.
- Modo Tela Cheia (F11) com botão dedicado na barra superior.

## Pontos de Integração
- PdvView: estrutura visual, atalhos, validações e novos controles.
- DiscountPolicy: lógica de teto de descontos para testes independentes.
- VirtualNumpadPopup: renderização via Popover.

## Atalhos
- F2: Caixa
- F3: Foco na pesquisa
- F4: Pagamento
- F5: Recarregar produtos
- F11: Alternar tela cheia

## Testes
- DiscountPolicyTest valida os limites por tipo/categoria/imposto.

## Considerações de Performance
- Renderização por TilePane mantida; filtragem/ordenação aplicadas no cliente.
- Atualizações incrementais da UI e contadores.

## Próximos Passos
- Externalizar política de descontos e parâmetros visuais para configurações.
- Avaliar virtualização de catálogo para coleções muito grandes.
