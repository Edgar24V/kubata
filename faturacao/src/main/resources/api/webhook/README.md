# Webhooks Kubata

Os eventos são enviados para a URL configurada em INTEGRACAO_WEBHOOK_URL.

## Assinatura

Cada pedido inclui:

- X-Kubata-Webhook-Id
- X-Kubata-Webhook-Timestamp
- X-Kubata-Webhook-Signature

A assinatura usa HMAC-SHA256 sobre:

timestamp + "." + payload

O resultado é enviado como:

sha256=<hex>

O consumidor deve validar a assinatura com INTEGRACAO_WEBHOOK_SECRET e aplicar uma política de expiração do timestamp para reduzir o risco de replay.

## Envelope

Cada evento segue o formato:

{
  "id": "event-id",
  "type": "document.copy.created",
  "version": "1",
  "timestamp": "2026-09-29T12:00:00Z",
  "empresaId": null,
  "data": {}
}

## Eventos já ligados

- webhook.test
- document.copy.created
- document.third_party_issuance.registered

## Entrega confiável

As entregas são persistidas em kubata_webhook_delivery.

Falhas de rede e respostas HTTP 408, 425, 429 e 5xx entram em retry automático até cinco tentativas.

O intervalo usa backoff progressivo. Entregas que ficam presas em SENDING por mais de cinco minutos são recuperadas pelo dispatcher.

## API de gestão

- GET /api/v1/webhooks/deliveries
- GET /api/v1/webhooks/stats
- POST /api/v1/webhooks/test
- POST /api/v1/webhooks/deliveries/{id}/retry

Estes endpoints exigem a API Key do Kubata.
