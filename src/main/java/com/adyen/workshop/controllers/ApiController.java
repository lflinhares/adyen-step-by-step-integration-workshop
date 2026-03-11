package com.adyen.workshop.controllers;

import com.adyen.service.checkout.ModificationsApi;
import com.adyen.model.checkout.PaymentAmountUpdateRequest;
import com.adyen.model.checkout.PaymentAmountUpdateResponse;
import com.adyen.model.checkout.PaymentCaptureRequest;
import com.adyen.model.checkout.PaymentCaptureResponse;
import com.adyen.model.checkout.PaymentCancelRequest;
import com.adyen.model.checkout.PaymentCancelResponse;
import com.adyen.model.checkout.PaymentRefundRequest;
import com.adyen.model.checkout.PaymentRefundResponse;
import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.recurring.DisableRequest;
import com.adyen.model.recurring.DisableResult;
import com.adyen.service.RecurringApi;
import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.RequestOptions;
import com.adyen.model.checkout.*;
import com.adyen.workshop.configurations.ApplicationConfiguration;
import com.adyen.service.checkout.PaymentsApi;
import com.adyen.service.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for using the Adyen payments API.
 */
@RestController
public class ApiController {
    private final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ApplicationConfiguration applicationConfiguration;
    private final PaymentsApi paymentsApi;

    public ApiController(ApplicationConfiguration applicationConfiguration, PaymentsApi paymentsApi) {
        this.applicationConfiguration = applicationConfiguration;
        this.paymentsApi = paymentsApi;
    }

    // Step 0
    @GetMapping("/hello-world")
    public ResponseEntity<String> helloWorld() throws Exception {
        return ResponseEntity.ok()
                .body("This is the 'Hello World' from the workshop - You've successfully finished step 0!");
    }

    // Step 7
    @PostMapping("/api/paymentMethods")
    public ResponseEntity<PaymentMethodsResponse> paymentMethods() throws IOException, ApiException {
        var paymentMethodsRequest = new PaymentMethodsRequest();
        paymentMethodsRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());

        log.info("Retrieving available Payment Methods from Adyen {}", paymentMethodsRequest);
        var response = paymentsApi.paymentMethods(paymentMethodsRequest);
        log.info("Payment Methods response from Adyen {}", response);
        return ResponseEntity.ok().body(response);
    }

    // Step 9 - Implement the /payments call to Adyen.
    @PostMapping("/api/payments")
    public ResponseEntity<PaymentResponse> payments(@RequestBody PaymentRequest body) throws IOException, ApiException {
        var paymentRequest = new PaymentRequest();

        var amount = new Amount()
                .currency("EUR")
                .value(9998L);
        paymentRequest.setAmount(amount);
        paymentRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        paymentRequest.setChannel(PaymentRequest.ChannelEnum.WEB);

        paymentRequest.setPaymentMethod(body.getPaymentMethod());

        var orderRef = UUID.randomUUID().toString();
        paymentRequest.setReference(orderRef);
        // The returnUrl field basically means: Once done with the payment, where should
        // the application redirect you?
        paymentRequest
                .setReturnUrl("https://miniature-guide-xgpxv4q746jc6v47-8080.app.github.dev/handleShopperRedirect");

        // Step 12 3DS2 Redirect - Add the following additional parameters to your
        // existing payment request for 3DS2 Redirect:
        // Note: Visa requires additional properties to be sent in the request, see
        // documentation for Redirect 3DS2:
        // https://docs.adyen.com/online-payments/3d-secure/redirect-3ds2/web-drop-in/#make-a-payment
        var authenticationData = new AuthenticationData();
        authenticationData.setAttemptAuthentication(AuthenticationData.AttemptAuthenticationEnum.ALWAYS);
        paymentRequest.setAuthenticationData(authenticationData);

        // Change the following lines, if you want to enable the Native 3DS2 flow:
        // Note: Visa requires additional properties to be sent in the request, see
        // documentation for Native 3DS2:
        // https://docs.adyen.com/online-payments/3d-secure/native-3ds2/web-drop-in/#make-a-payment
        // authenticationData.setThreeDSRequestData(new
        // ThreeDSRequestData().nativeThreeDS(ThreeDSRequestData.NativeThreeDSEnum.PREFERRED));
        // paymentRequest.setAuthenticationData(authenticationData);

        paymentRequest.setOrigin("https://miniature-guide-xgpxv4q746jc6v47-8080.app.github.dev/");
        paymentRequest.setBrowserInfo(body.getBrowserInfo());
        paymentRequest.setShopperIP("192.168.0.1");
        paymentRequest.setShopperInteraction(PaymentRequest.ShopperInteractionEnum.ECOMMERCE);

        var billingAddress = new BillingAddress();
        billingAddress.setCity("Amsterdam");
        billingAddress.setCountry("NL");
        billingAddress.setPostalCode("1012KK");
        billingAddress.setStreet("Rokin");
        billingAddress.setHouseNumberOrName("49");
        paymentRequest.setBillingAddress(billingAddress);

        // Step 11 - Optionally add the idempotency key
        var requestOptions = new RequestOptions();
        requestOptions.setIdempotencyKey(UUID.randomUUID().toString());

        log.info("PaymentsRequest {}", paymentRequest);
        var response = paymentsApi.payments(paymentRequest, requestOptions); // add RequestOptions here
        log.info("PaymentsResponse {}", response);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/api/preauthorisation")
    public ResponseEntity<PaymentResponse> preauthorisation(@RequestBody PaymentRequest body)
            throws IOException, ApiException {
        var paymentRequest = new PaymentRequest();

        var amount = new Amount()
                .currency("EUR")
                .value(5000L); // Substitua pelo valor que deseja reservar
        paymentRequest.setAmount(amount);

        paymentRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        paymentRequest.setChannel(PaymentRequest.ChannelEnum.WEB);

        paymentRequest.setPaymentMethod(body.getPaymentMethod());

        var orderRef = UUID.randomUUID().toString();
        paymentRequest.setReference(orderRef);

        paymentRequest
                .setReturnUrl("https://miniature-guide-xgpxv4q746jc6v47-8080.app.github.dev/handleShopperRedirect");

        var authenticationData = new AuthenticationData();
        authenticationData.setAttemptAuthentication(AuthenticationData.AttemptAuthenticationEnum.ALWAYS);
        paymentRequest.setAuthenticationData(authenticationData);

        paymentRequest.setOrigin("https://miniature-guide-xgpxv4q746jc6v47-8080.app.github.dev/");
        paymentRequest.setBrowserInfo(body.getBrowserInfo());
        paymentRequest.setShopperIP("192.168.0.1");
        paymentRequest.setShopperInteraction(PaymentRequest.ShopperInteractionEnum.ECOMMERCE);

        var billingAddress = new BillingAddress();
        billingAddress.setCity("Amsterdam");
        billingAddress.setCountry("NL");
        billingAddress.setPostalCode("1012KK");
        billingAddress.setStreet("Rokin");
        billingAddress.setHouseNumberOrName("49");
        paymentRequest.setBillingAddress(billingAddress);

        // =========================================================
        // 2. AJUSTE PARA PRE-AUTH:
        // Informar a Adyen que esta é uma Pré-autorização
        // =========================================================
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("authorisationType", "PreAuth");
        paymentRequest.setAdditionalData(additionalData);
        // =========================================================

        // Step 11 - Optionally add the idempotency key
        var requestOptions = new RequestOptions();
        requestOptions.setIdempotencyKey(UUID.randomUUID().toString());

        log.info("PaymentsRequest {}", paymentRequest);
        var response = paymentsApi.payments(paymentRequest, requestOptions); // add RequestOptions here
        log.info("PaymentsResponse {}", response);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/api/modify-amount")
    public ResponseEntity<PaymentAmountUpdateResponse> modifyAmount(@RequestBody ModificationRequestDTO body)
            throws Exception {
        Client client = new Client(applicationConfiguration.getAdyenApiKey(), Environment.TEST);
        ModificationsApi modificationsApi = new ModificationsApi(client);

        PaymentAmountUpdateRequest request = new PaymentAmountUpdateRequest();
        request.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        request.setReference(UUID.randomUUID().toString()); // Sua própria referência de ajuste

        Amount amount = new Amount().currency("EUR").value(body.getValue());
        request.setAmount(amount);

        PaymentAmountUpdateResponse response = modificationsApi.updateAuthorisedAmount(body.getPaymentPspReference(),
                request);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/api/capture")
    public ResponseEntity<PaymentCaptureResponse> capturePayment(@RequestBody ModificationRequestDTO body)
            throws Exception {
        Client client = new Client(applicationConfiguration.getAdyenApiKey(), Environment.TEST);
        ModificationsApi modificationsApi = new ModificationsApi(client);

        PaymentCaptureRequest request = new PaymentCaptureRequest();
        request.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        request.setReference(UUID.randomUUID().toString()); // Sua própria referência de captura

        // Você pode capturar o valor total do Pre-auth ou fazer uma captura parcial
        Amount amount = new Amount().currency("EUR").value(body.getValue());
        request.setAmount(amount);

        PaymentCaptureResponse response = modificationsApi.captureAuthorisedPayment(body.getPaymentPspReference(),
                request);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/api/cancel")
    public ResponseEntity<PaymentCancelResponse> cancelPayment(@RequestBody ModificationRequestDTO body)
            throws Exception {
        Client client = new Client(applicationConfiguration.getAdyenApiKey(), Environment.TEST);
        ModificationsApi modificationsApi = new ModificationsApi(client);

        PaymentCancelRequest request = new PaymentCancelRequest();
        request.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        request.setReference(UUID.randomUUID().toString());

        PaymentCancelResponse response = modificationsApi
                .cancelAuthorisedPaymentByPspReference(body.getPaymentPspReference(), request);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/api/refund")
    public ResponseEntity<PaymentRefundResponse> refundPayment(@RequestBody ModificationRequestDTO body)
            throws Exception {
        Client client = new Client(applicationConfiguration.getAdyenApiKey(), Environment.TEST);
        ModificationsApi modificationsApi = new ModificationsApi(client);

        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        request.setReference(UUID.randomUUID().toString());

        Amount amount = new Amount().currency("EUR").value(body.getValue());
        request.setAmount(amount);

        PaymentRefundResponse response = modificationsApi.refundCapturedPayment(body.getPaymentPspReference(), request);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/api/subscription-payment")
    public ResponseEntity<PaymentResponse> subscriptionCharge(@RequestBody SubscriptionChargeRequest body)
            throws IOException, ApiException {

        PaymentRequest paymentRequest = new PaymentRequest();

        Amount amount = new Amount().currency("EUR").value(500L);
        paymentRequest.setAmount(amount);
        paymentRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        paymentRequest.setReference(UUID.randomUUID().toString());
        paymentRequest.setShopperReference(body.getShopperReference());

        CardDetails cardDetails = new CardDetails();
        cardDetails.setType(CardDetails.TypeEnum.SCHEME); // Type must be scheme for cards
        cardDetails.setStoredPaymentMethodId(body.getRecurringDetailRef());

        // Wrap it in the CheckoutPaymentMethod required by Adyen v20+
        paymentRequest.setPaymentMethod(new CheckoutPaymentMethod(cardDetails));

        paymentRequest.setRecurringProcessingModel(PaymentRequest.RecurringProcessingModelEnum.SUBSCRIPTION);
        paymentRequest.setShopperInteraction(PaymentRequest.ShopperInteractionEnum.CONTAUTH);

        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setIdempotencyKey(UUID.randomUUID().toString());

        PaymentResponse response = paymentsApi.payments(paymentRequest, requestOptions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/subscription-create")
    public ResponseEntity<PaymentResponse> subscriptionCreate(@RequestBody PaymentRequest body)
            throws IOException, ApiException {
        var paymentRequest = new PaymentRequest();

        var amount = new Amount()
                .currency("EUR")
                .value(0L);
        paymentRequest.setAmount(amount);
        paymentRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
        paymentRequest.setChannel(PaymentRequest.ChannelEnum.WEB);
        paymentRequest.setRecurringProcessingModel(PaymentRequest.RecurringProcessingModelEnum.SUBSCRIPTION);
        paymentRequest.setShopperReference("test-shopper-1234");
        paymentRequest.setStorePaymentMethod(true);
        paymentRequest.setPaymentMethod(body.getPaymentMethod());

        var orderRef = UUID.randomUUID().toString();
        paymentRequest.setReference(orderRef);

        paymentRequest
                .setReturnUrl("https://miniature-guide-xgpxv4q746jc6v47-8080.app.github.dev/handleShopperRedirect");

        var authenticationData = new AuthenticationData();
        authenticationData.setAttemptAuthentication(AuthenticationData.AttemptAuthenticationEnum.ALWAYS);
        paymentRequest.setAuthenticationData(authenticationData);

        // Change the following lines, if you want to enable the Native 3DS2 flow:
        // Note: Visa requires additional properties to be sent in the request, see
        // documentation for Native 3DS2:
        // https://docs.adyen.com/online-payments/3d-secure/native-3ds2/web-drop-in/#make-a-payment
        // authenticationData.setThreeDSRequestData(new
        // ThreeDSRequestData().nativeThreeDS(ThreeDSRequestData.NativeThreeDSEnum.PREFERRED));
        // paymentRequest.setAuthenticationData(authenticationData);

        paymentRequest.setOrigin("https://miniature-guide-xgpxv4q746jc6v47-8080.app.github.dev/");
        paymentRequest.setBrowserInfo(body.getBrowserInfo());
        paymentRequest.setShopperIP("192.168.0.1");
        paymentRequest.setShopperInteraction(PaymentRequest.ShopperInteractionEnum.ECOMMERCE);

        var billingAddress = new BillingAddress();
        billingAddress.setCity("Amsterdam");
        billingAddress.setCountry("NL");
        billingAddress.setPostalCode("1012KK");
        billingAddress.setStreet("Rokin");
        billingAddress.setHouseNumberOrName("49");
        paymentRequest.setBillingAddress(billingAddress);

        // Step 11 - Optionally add the idempotency key
        var requestOptions = new RequestOptions();
        requestOptions.setIdempotencyKey(UUID.randomUUID().toString());

        log.info("PaymentsRequest {}", paymentRequest);
        var response = paymentsApi.payments(paymentRequest, requestOptions); // add RequestOptions here
        log.info("PaymentsResponse {}", response);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/api/subscription-cancel")
    public ResponseEntity<DisableResult> subscriptionCancel(
            @RequestBody CancelSubscriptionRequest body)
            throws IOException, ApiException {

        DisableRequest disableRequest = new DisableRequest();

        // 1. Informa a conta Adyen
        disableRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());

        // 2. Informa qual cliente é o dono do token
        disableRequest.setShopperReference(body.getShopperReference());

        // 3. Informa qual é o token (cartão salvo/assinatura) que será deletado
        disableRequest.setRecurringDetailReference(body.getRecurringDetailRef());

        // NOTA: Como você precisa da RecurringApi (e hoje você só tem a PaymentsApi
        // injetada),
        // você precisará instanciá-la (ou injetá-la no construtor igual fez com a
        // PaymentsApi).
        // Caso você tenha a apiKey no applicationConfiguration, pode fazer assim:
        Client client = new Client(applicationConfiguration.getAdyenApiKey(), Environment.TEST);
        RecurringApi recurringApi = new RecurringApi(client);

        // 4. Faz a requisição para desativar o token
        DisableResult response = recurringApi.disable(disableRequest);

        return ResponseEntity.ok(response);
    }

    // Step 13 - Handle details call (triggered after Native 3DS2 flow)
    @PostMapping("/api/payments/details")
    public ResponseEntity<PaymentDetailsResponse> paymentsDetails(@RequestBody PaymentDetailsRequest detailsRequest)
            throws IOException, ApiException {

        return ResponseEntity.ok().body(null);
    }

    // Step 14 - Handle Redirect 3DS2 during payment.

    @GetMapping("/handleShopperRedirect")
    public RedirectView redirect(@RequestParam(required = false) String payload,
            @RequestParam(required = false) String redirectResult) throws IOException, ApiException {
        var paymentDetailsRequest = new PaymentDetailsRequest();

        PaymentCompletionDetails paymentCompletionDetails = new PaymentCompletionDetails();

        // Handle redirect result or payload
        if (redirectResult != null && !redirectResult.isEmpty()) {
            // For redirect, you are redirected to an Adyen domain to complete the 3DS2
            // challenge
            // After completing the 3DS2 challenge, you get the redirect result from Adyen
            // in the returnUrl
            // We then pass on the redirectResult
            paymentCompletionDetails.redirectResult(redirectResult);
        } else if (payload != null && !payload.isEmpty()) {
            paymentCompletionDetails.payload(payload);
        }

        paymentDetailsRequest.setDetails(paymentCompletionDetails);

        var paymentsDetailsResponse = paymentsApi.paymentsDetails(paymentDetailsRequest);
        log.info("PaymentsDetailsResponse {}", paymentsDetailsResponse);

        // Handle response and redirect user accordingly
        var redirectURL = "https://miniature-guide-xgpxv4q746jc6v47-8080.app.github.dev/result/"; // Update your url
                                                                                                  // here by replacing
                                                                                                  // `http://localhost:8080`
        // with where your application is hosted (if needed)
        switch (paymentsDetailsResponse.getResultCode()) {
            case AUTHORISED:
                redirectURL += "success";
                break;
            case PENDING:
            case RECEIVED:
                redirectURL += "pending";
                break;
            case REFUSED:
                redirectURL += "failed";
                break;
            default:
                redirectURL += "error";
                break;
        }
        return new RedirectView(redirectURL + "?reason=" + paymentsDetailsResponse.getResultCode());
    }
}

class SubscriptionChargeRequest {
    private String shopperReference;
    private String recurringDetailRef; // <-- Changed this to match frontend
    private String cvc;

    public String getShopperReference() {
        return shopperReference;
    }

    public void setShopperReference(String shopperReference) {
        this.shopperReference = shopperReference;
    }

    public String getRecurringDetailRef() {
        return recurringDetailRef;
    }

    public void setRecurringDetailRef(String recurringDetailRef) {
        this.recurringDetailRef = recurringDetailRef;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }
}

class CancelSubscriptionRequest {
    private String shopperReference;
    private String recurringDetailRef;

    public String getShopperReference() {
        return shopperReference;
    }

    public void setShopperReference(String shopperReference) {
        this.shopperReference = shopperReference;
    }

    public String getRecurringDetailRef() {
        return recurringDetailRef;
    }

    public void setRecurringDetailRef(String recurringDetailRef) {
        this.recurringDetailRef = recurringDetailRef;
    }
}

class ModificationRequestDTO {
    private String paymentPspReference; // O ID gerado pela Adyen no momento do Pre-Auth
    private Long value; // O novo valor (para Capture, Refund ou Amount Update)

    public String getPaymentPspReference() {
        return paymentPspReference;
    }

    public void setPaymentPspReference(String paymentPspReference) {
        this.paymentPspReference = paymentPspReference;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}