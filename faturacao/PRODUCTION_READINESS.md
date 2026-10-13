# Kubata Faturação — preparação para produção

## Estado desta implementação

Esta branch melhora os contratos técnicos da integração AGT, o rastreio do pedido, a validação do estado fiscal, a segurança das assinaturas e o comportamento das migrações. **Não constitui certificação nem prova de homologação do Kubata pela AGT.** Não emitir facturas fiscais reais antes de concluir os passos operacionais e os testes abaixo.

## Configuração necessária

Definir os valores no gestor de segredos/ambiente do servidor; nunca no repositório:

- `SPRING_PROFILES_ACTIVE=prod`
- `KUBATA_AGT_API_ENABLED=true`
- `KUBATA_AGT_API_URL=https://sifp.minfin.gov.ao/sigt/fe/v1`
- `KUBATA_AGT_SOFTWARE_VALIDATION_NUMBER=<número oficial atribuído pela AGT>`
- `KUBATA_AGT_EMITTER_NIF=<NIF real da empresa emitente>`
- `KUBATA_AGT_EMITTER_COMPANY_NAME=<denominação legal registada>`
- `KUBATA_AGT_SIGNATURE_TYPE=RSA_SHA256`
- `KUBATA_AGT_CERTIFICATE_PATH=<caminho protegido para o certificado PKCS#12>`
- `KUBATA_AGT_CERTIFICATE_PASSWORD=<palavra-passe obtida do gestor de segredos>`
- `KUBATA_AGT_ALLOW_DEV_SIGNATURES=false`
- `KUBATA_AGT_SIMULATION_ENABLED=false`

O certificado/chave usado para assinar os hashes fiscais e os payloads JWS deve corresponder à chave registada/aceite no processo AGT do software. Não reutilizar chaves de desenvolvimento nem criar chaves automaticamente em produção.

## Critérios obrigatórios antes de emissão real

1. Obter a validação/credenciação aplicável do software, confirmar o número de validação atribuído e concluir os testes de homologação com a AGT.
2. Validar os payloads do Kubata contra o schema oficial em ambiente de homologação: séries/números, documentos corrigidos, impostos e códigos de isenção, retenções, arredondamento, datas, assinaturas, pedido duplicado e consulta assíncrona.
3. Confirmar que os resultados de `registarFactura` são tratados como recepção do pedido; apenas `documentStatus = V` em `obterEstado` confirma a validação fiscal. `I` significa documento inválido; códigos de pedido 7/8/9 não significam sucesso.
4. Rever os fluxos de notas de crédito/débito, anulação, pagamentos, devoluções, estoque e lançamentos contabilísticos com contabilista/responsável fiscal e utilizadores de homologação.
5. Executar a suite Maven completa, smoke tests SQLite e PostgreSQL, testes de concorrência de numeração de séries e testes de recuperação após falha de rede/AGT.
6. Configurar cópias de segurança, monitorização de falhas/retries, relógio do servidor, controlo de acesso, rotação de certificados/segredos e retenção de auditoria.

## Ambientes

- Desenvolvimento: a simulação pode ser explicitamente activada; cada resposta é marcada `SIMULATED` e não gera código fictício de validação fiscal.
- Produção: simulação e assinatura de desenvolvimento estão desactivadas. A emissão/submissão falha de forma explícita quando a configuração fiscal real estiver incompleta.

Documentação técnica de referência: portal oficial do parceiro AGT, serviços **Registar Factura** e **Consultar Estado da Factura**.
