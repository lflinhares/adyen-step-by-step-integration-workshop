package com.adyen.workshop.controllers;

import com.adyen.model.notification.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.util.HMACValidator;
import com.adyen.workshop.configurations.ApplicationConfiguration;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.SignatureException;

/**
 * REST controller for receiving Adyen webhook notifications
 */
@RestController
public class WebhookController {
    private final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final ApplicationConfiguration applicationConfiguration;

    private final HMACValidator hmacValidator;

    @Autowired
    public WebhookController(ApplicationConfiguration applicationConfiguration, HMACValidator hmacValidator) {
        this.applicationConfiguration = applicationConfiguration;
        this.hmacValidator = hmacValidator;
    }

    // Step 16 - Validate the HMAC signature using the ADYEN_HMAC_KEY
    @PostMapping("/webhooks")
    public ResponseEntity<String> webhooks(@RequestBody String json) throws Exception {

        log.info("Received: {}", json);
        NotificationRequest notificationRequest = NotificationRequest.fromJson(json);

        // A Adyen manda os eventos dentro de uma lista de itens
           notificationRequest.getNotificationItems().forEach(item -> {
        
        String eventCode = item.getEventCode();
        boolean success = item.isSuccess();
        String originalReference = item.getOriginalReference(); // O PSP original
        

            log.info("Recebido Webhook EventCode: {} | Sucesso: {} | Referência Original: {}", eventCode, success,
                    originalReference);

            // Filtra os tipos de webhooks de modificação
            switch (eventCode) {
                case "AUTHORISATION_ADJUSTMENT":
                    if (success) {
                        log.info("✅ Ajuste de valor do pre-auth concluído com sucesso.");
                    } else {
                        log.error("❌ Ajuste de valor falhou. Motivo: {}", item.getReason());
                    }
                    break;

                case "CAPTURE":
                    if (success) {
                        log.info("✅ O dinheiro foi capturado com sucesso e está a caminho da sua conta.");
                    }
                    break;

                case "CAPTURE_FAILED":
                    log.error("❌ Captura falhou! Motivo: {}", item.getReason());
                    break;

                case "CANCELLATION":
                case "TECHNICAL_CANCEL":
                    if (success) {
                        log.info("✅ O pagamento foi cancelado e o limite do cartão do cliente foi liberado.");
                    }
                    break;

                case "REFUND":
                    if (success) {
                        log.info("✅ Reembolso efetuado para o cliente com sucesso.");
                    } else {
                        log.error("❌ Falha no reembolso. Motivo: {}", item.getReason());
                    }
                    break;

                case "REFUND_FAILED":
                case "REFUNDED_REVERSED":
                    log.error("❌ Reembolso falhou ou foi revertido pelo banco do cliente.");
                    break;
            }
        });

        return ResponseEntity.ok("[accepted]");
    }
}