# Kubata Faturação — preparação para produção

## Estado desta implementação

Esta branch melhora os contratos técnicos da integração AGT, o rastreio do pedido, a validação do estado fiscal, a segurança das assinaturas e o comportamento das migrações. **Não constitui certificação nem prova de homologação do Kubata pela AGT.** Não emitir facturas fiscais reais antes de concluir os passos operacionais e os testes abaixo.

## Configuração necessária

Definir os valores no gestor de segredos/ambiente do servidor; nunca no repositório:

- `SPRING_PROFILES_ACTIVE=prod`
- `KUBATA_AGT_API_ENABLED=true`
- `KUBATA_AGT_API_URL=https://sifp.minfin.gov.ao/sigt/fe/v1`
- `KUBATA_AGT_API_USERNAME=<username Basic Auth atribuído pela AGT>`
- `KUBATA_AGT_API_PASSWORD=<password Basic Auth atribuído pela AGT>`
- `KUBATA_AGT_SOFTWARE_VALIDATION_NUMBER=<número oficial atribuído pela AGT>`
- `KUBATA_AGT_EMITTER_NIF=<NIF real da empresa emitente>`
- `KUBATA_AGT_EMITTER_COMPANY_NAME=<denominação legal registada>`
- `KUBATA_AGT_SIGNATURE_TYPE=RSA_SHA256`
- `KUBATA_AGT_CERTIFICATE_PATH=<caminho protegido para o certificado PKCS#12 fiscal do contribuinte>`
- `KUBATA_AGT_CERTIFICATE_PASSWORD=<palavra-passe obtida do gestor de segredos>`
- `KUBATA_AGT_SOFTWARE_CERTIFICATE_PATH=<caminho protegido para o PKCS#12 do produtor do software>`
- `KUBATA_AGT_SOFTWARE_CERTIFICATE_PASSWORD=<palavra-passe obtida do gestor de segredos>`
- `KUBATA_AGT_ALLOW_DEV_SIGNATURES=false`
- `KUBATA_AGT_SIMULATION_ENABLED=false`

O certificado fiscal do contribuinte e o certificado do produtor do software são configurações e chaves privadas distintas: o segundo assina `jwsSoftwareSignature`; o primeiro assina os payloads JWS fiscais do contribuinte e é usado no cálculo de hash conforme a implementação do módulo. O Kubata falha em produção se ambos não estiverem configurados com chaves públicas diferentes. Confirmar com a AGT que cada certificado e respectivo uso estão registados/aceites no processo de validação do software. Não reutilizar chaves de desenvolvimento nem criar chaves automaticamente em produção.

## Critérios obrigatórios antes de emissão real

1. Obter a validação/credenciação aplicável do software, confirmar o número de validação atribuído e concluir os testes de homologação com a AGT.
2. Validar os payloads do Kubata contra o schema oficial em ambiente de homologação: séries/números, documentos corrigidos, impostos e códigos de isenção, retenções, arredondamento, datas, assinaturas, pedido duplicado e consulta assíncrona.
3. Confirmar que os resultados de `registarFactura` são tratados como recepção do pedido; apenas `documentStatus = V` em `obterEstado` confirma a validação fiscal de uma submissão. `I` significa documento inválido; códigos de pedido 7/8/9 não significam sucesso. Quando não existe `requestID` devido a uma falha de comunicação, a consulta oficial `consultarFactura` pode ser usada para reconciliar o número fiscal; o Kubata não deve reenviar automaticamente um documento de resultado desconhecido. A resposta `validationStatus = P` significa válida com penalização e deve permanecer identificada para análise fiscal.
4. Rever os fluxos de notas de crédito/débito, anulação, pagamentos, devoluções, estoque e lançamentos contabilísticos com contabilista/responsável fiscal e utilizadores de homologação.
5. Executar a suite Maven completa, smoke tests SQLite e PostgreSQL, testes de concorrência de numeração de séries e testes de recuperação após falha de rede/AGT.
6. Configurar cópias de segurança, monitorização de falhas/retries, relógio do servidor, controlo de acesso, rotação de certificados/segredos e retenção de auditoria.

## Ambientes

- Desenvolvimento: a simulação pode ser explicitamente activada; cada resposta é marcada `SIMULATED` e não gera código fictício de validação fiscal.
- Produção: simulação e assinatura de desenvolvimento estão desactivadas. A emissão/submissão falha de forma explícita quando a configuração fiscal real estiver incompleta.

Documentação técnica de referência: portal oficial da AGT, serviços **Registar Factura**, **Consultar Estado da Factura** e **Consultar Factura**.

## Limites funcionais assumidos nesta fase

- O tipo interno `RECIBO` continua mapeado para `RC` com linhas. Nesta branch, `AR` (Aviso de Cobrança/Recibo) e `RG` (Recibo) passam a ter mapeamento explícito para o schema de produção AGT v2: sem `lines`, com `paymentReceipt.sourceDocuments` e a propriedade `OriginatingON` com a capitalização exacta da API. O fluxo UI de AR/RG é deliberadamente restrito a uma factura FT ou nota de débito ND já liquidada por um único recibo interno que corresponda ao total, sem retenção na fonte. Recebimentos parciais/múltiplos, notas de crédito como origem, retenções e liquidação de vários documentos continuam bloqueados até haver alocação fiscal explícita por linha e testes de homologação. A aceitação do pedido ou a existência de `requestID` não significa validação fiscal: deve ser consultado o estado oficial.
- As notas de crédito/débito levam agora `referenceInfo` para o documento de origem. A correcção de uma factura rejeitada continua bloqueada na interface porque requer criar um documento novo, com nova numeração e referência explícita ao documento rejeitado.
- A anulação continua bloqueada: não marcar localmente como anulada nem submeter novamente a factura normal. A operação oficial específica `anularFactura` precisa do payload/assinatura exactos e validação de homologação.
- O arredondamento do IVA por linha foi alinhado com a regra AGT de arredondar por excesso ao cêntimo. Testar em homologação os casos de IVA, descontos, retenções, notas de crédito/débito e reconciliação de totais antes de emitir documentos reais.

## CI observado nesta branch

No commit `b317178e1555f0a9fbd6bd330aad31f379905077`, o GitHub Actions run [#1857](https://github.com/Edgar24V/kubata/actions/runs/38744976736) terminou com sucesso: o build completo do reactor, o job dedicado `mvn -B -pl faturacao -am clean test` e os testes do Administrator/dependências passaram. Durante a validação foi corrigido o import em falta de `HashMap` e removidas duas migrações V97 redundantes: a V94 já cria e preenche `desconto_percentual` em SQLite e PostgreSQL. A UI de Contas a Receber também foi corrigida para não gravar cada pagamento duas vezes e para guardar o método escolhido.

CI verde valida os testes automatizados executados no runner; não equivale a homologação fiscal. Continuam obrigatórios os testes de instalação/actualização de bases SQLite e PostgreSQL, concorrência e recuperação, bem como a homologação AGT com credenciais e certificados oficiais.

## Rotação de credenciais e histórico Git

O ficheiro local serializado de chave foi removido da versão actual e o tipo de ficheiro foi adicionado ao `.gitignore`. **Remover um ficheiro num commit não o elimina dos commits anteriores.** Se esse ficheiro continha uma chave real/activa, considerá-la exposta: revogar/substituir a credencial e coordenar uma limpeza do histórico Git antes de publicar ou distribuir um novo histórico. Não reutilizar essa chave em produção.
