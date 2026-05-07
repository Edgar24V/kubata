# Manual do Utilizador - KUBATA Faturação

Bem-vindo ao sistema **KUBATA Faturação**. Este manual irá guiá-lo pelas principais funcionalidades do software.

## Acesso e Navegação

### Login
1.  Abra o aplicativo KUBATA Faturação.
2.  Insira seu nome de usuário e senha (se solicitado).
3.  O Dashboard inicial será exibido com um resumo das atividades.

### Menu Principal
- **Dashboard**: Visão geral de vendas, stock e alertas.
- **Inventário**: Cadastro de produtos e controle de estoque.
- **Clientes**: Gestão de clientes e histórico de compras.
- **Faturas**: Emissão e consulta de documentos fiscais.
- **Recibos**: Gestão de pagamentos.
- **PDV (Frente de Caixa)**: Venda rápida para operadores.
- **Perfis**: Configurações de acesso e usuários.

## Gestão de Inventário

### Cadastrar Novo Produto
1.  Vá para a aba **Inventário**.
2.  Clique em **Novo Produto** (ou use o atalho `Ctrl+N`).
3.  Preencha os campos obrigatórios:
    - **Nome**: Descrição do produto.
    - **Código de Barra**: Identificador único (pode ser gerado automaticamente).
    - **Preço (AOA)**: Preço de venda unitário.
    - **Unidade de Medida**: Selecione a unidade (UN, KG, L, M, CX).
    - **Stock Mín/Máx**: Defina os limites para alertas de reposição.
4.  Clique em **Salvar**.

### Ajuste de Estoque (Entrada/Saída Manual)
1.  No **Inventário**, selecione um produto.
2.  Clique em **Movimentação** ou clique com o botão direito sobre o produto.
3.  Escolha o tipo de operação:
    - **Entrada (+)**: Para adicionar itens ao estoque (compra, devolução).
    - **Saída (-)**: Para remover itens (perda, avaria, consumo interno).
4.  Insira a quantidade, armazém de destino/origem e uma observação.
5.  Clique no botão correspondente para confirmar.

### Relatórios de Inventário
- **Exportar CSV**: Gera uma planilha com a lista atual de produtos e quantidades.
- **Exportar SAFT-AO**: Gera o arquivo XML padrão para a Autoridade Geral Tributária (AGT). Clique no botão "Exportar SAFT-AO" na barra superior.

## Gestão de Fornecedores

1.  Acesse o módulo **Fornecedores** no menu lateral (se disponível) ou via atalho.
2.  Para cadastrar: Clique em **Novo** (`Ctrl+N`).
    - Informe o **NIF** (Número de Identificação Fiscal) corretamente.
    - Preencha endereço e contato.
3.  Para avaliar: Utilize o campo de observações ou avaliação interna para registrar o desempenho do fornecedor.

## Vendas e Faturação (PDV)

### Frente de Caixa (PDV)
1.  Acesse o módulo **PDV**.
2.  Escaneie o código de barras ou digite o nome do produto.
3.  Ajuste a quantidade conforme necessário.
4.  Selecione o cliente (opcional).
5.  Finalize a venda escolhendo a forma de pagamento (Dinheiro, Multicaixa, Transferência).
6.  A fatura/recibo será impressa automaticamente.

## Manutenção e Segurança

### Backup de Dados
- O sistema realiza backups automáticos diariamente às 02:00 da manhã.
- Os arquivos de backup são salvos na pasta configurada pelo administrador do sistema.
- Em caso de falha, contate o suporte técnico para restauração.

### Suporte Técnico
Para dúvidas ou problemas não cobertos neste manual, entre em contato com o suporte local ou consulte a documentação técnica.

---
**KUBATA Faturação** - Tecnologia a serviço do seu negócio em Angola.
