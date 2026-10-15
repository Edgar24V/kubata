# Faturação Kubata — Estado de preparação para produção

## Estado actual

Esta tranche reforça regras de ciclo de vida fiscal, registo de recebimentos e protecção da integração AGT. **Não significa que o Kubata esteja certificado pela AGT ou autorizado a emitir documentos fiscais em produção.** Essa declaração depende de validação externa, credenciais reais, contrato oficial da API e testes de conformidade.

## Alterações desta tranche

- Só documentos em rascunho podem ser editados ou emitidos.
- Emissão valida cliente, linhas, quantidades, preço, taxa de imposto e total antes de consumir o próximo número.
- A série não pode alterar silenciosamente o tipo de documento.
- Os recebimentos são persistidos como recibos, bloqueiam a factura durante o cálculo do saldo e não permitem receber acima do saldo aberto.
- Uma factura só passa a paga quando os recibos registados liquidam exactamente o seu total.
- O descarte de rascunhos foi separado da anulação fiscal.
- Uma anulação de documento emitido exige motivo detalhado e um motivo fiscal explícito; o sistema não apaga o documento emitido.
- O stock só é estornado se o documento original tiver movimentado stock na emissão.
- A submissão AGT é agendada após confirmação da transacção local; o callback actualiza o estado numa transacção separada.
- O modo de simulação é identificado como tal e não gera códigos de validação fictícios.
- O envio real fica bloqueado quando faltam endpoint HTTPS, API key, NIF do emitente, número de validação do software ou certificado PKCS#12/RSA.

## Verificações automatizadas

Executar a partir da raiz do repositório:

```powershell
mvn -B -pl faturacao -am test
```

A aprovação desse comando deve ser verificada no ambiente de desenvolvimento e na pipeline CI. Os testes unitários não substituem testes integrados com a AGT.

## Bloqueadores antes da utilização fiscal real

1. **Credenciais e certificação:** obter e configurar o número de validação do software e as credenciais que forem efectivamente atribuídas à empresa/software pela AGT. Não inventar identificadores.
2. **Contrato técnico AGT:** confirmar junto da AGT o endpoint autorizado, autenticação, esquema dos pedidos e respostas, limites, códigos de validação, erros, retries e o ambiente de homologação. A URL permanece por configurar intencionalmente enquanto não for confirmada.
3. **Certificado de assinatura:** instalar o certificado fiscal PKCS#12 autorizado e guardar a sua palavra-passe em armazenamento de segredos, nunca no repositório. Verificar ciclo de rotação e backups da chave.
4. **Séries documentais:** implementar/confirmar registo e comunicação de séries, estabelecimento, exercício, tipo fiscal, contingência e numeração conforme instruções da AGT. O número interno de recibo deste projecto, por si só, não equivale a um recibo fiscal certificado.
5. **Regras fiscais e arredondamento:** validar taxas, isenções, retenções, regime do emitente, notas de crédito/débito, diferenças de arredondamento e cálculo da base tributável contra especificação oficial e casos de homologação.
6. **SAF-T (AO):** validar exportação XML completa com XSD oficial, totais, impostos, utilizadores, séries, documentos, movimentos e assinaturas; executar os validadores oficiais aplicáveis.
7. **Testes de integração:** cobrir concorrência na numeração, idempotência de submissões, indisponibilidade e retries da AGT, resposta duplicada, rejeição, anulação, recibos parciais, reembolso/estorno, reconciliação contabilística e backup/restauro.
8. **Auditoria e segurança:** garantir actor, data/hora, empresa/exercício, motivo e trilho imutável para emissão, anulação e rectificação; restringir permissões no servidor, não apenas no ecrã.
9. **Revisão legal e operacional:** validar os processos com contabilista responsável e obter confirmação da conformidade/validação exigida pela AGT antes de emitir em ambiente real.

## Âmbito funcional a fechar para paridade operacional com PRIMAVERA

- Clientes, artigos/serviços, impostos, descontos e retenções.
- Orçamentos, encomendas e pró-formas com conversão controlada.
- Facturas, facturas-recibo, vendas a dinheiro, recibos e cobranças parciais.
- Notas de crédito/débito e anulações com referência ao documento original.
- Guias de remessa/transporte e integração com movimentos de stock.
- Séries documentais, permissões, auditoria e fecho do exercício.
- Impressão/PDF, envio por correio electrónico, pesquisas e relatórios por período/cliente/imposto.
- Integração contabilidade/tesouraria/PDV e reconciliação.
- SAF-T (AO), validação de esquemas e gestão de falhas/reenvios AGT.

A conclusão de cada item deve exigir implementação, testes automatizados e evidência de validação; não apenas a presença de um ecrã.
