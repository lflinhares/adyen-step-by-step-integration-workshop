const clientKey = document.getElementById("clientKey").innerHTML;
const { AdyenCheckout, Dropin } = window.AdyenWeb;

// Starts the (Adyen.Web) AdyenCheckout with your specified configuration by calling the `/paymentMethods` endpoint.
async function startCheckout() {
    try {
        // Step 8 - Retrieve the available payment methods
        const paymentMethodsResponse = await fetch("/api/paymentMethods", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            }
        }).then(response => response.json());

        const configuration = {
            paymentMethodsResponse: paymentMethodsResponse,
            clientKey,
            locale: "en_US",
            countryCode: 'NL',
            environment: "test",
            showPayButton: true,
            translations: {
                'en-US': {
                    'creditCard.securityCode.label': 'CVV/CVC'
                }
            },
            // Step 10 - Add the onSubmit handler by telling it what endpoint to call when the pay button is pressed.
            onSubmit: async (state, component, actions) => {
                console.info("onSubmit", state, component, actions);
                try {
                    if (state.isValid) {
                        const url = window.location.pathname === "/checkout" ? "/api/payments" : window.location.pathname === "/checkout-subscription" ? "/api/subscription-create" : "/api/preauthorisation";
                        const { action, order, resultCode } = await fetch(url, {
                            method: "POST",
                            body: state.data ? JSON.stringify(state.data) : "",
                            headers: {
                                "Content-Type": "application/json",
                            }
                        }).then(response => response.json());

                        if (!resultCode) {
                            console.warn("reject");
                            actions.reject();
                        }

                        actions.resolve({
                            resultCode,
                            action,
                            order
                        });
                    }
                } catch (error) {
                    console.error(error);
                    actions.reject();
                }
            },
            onPaymentCompleted: (result, component) => {
                console.info("onPaymentCompleted", result, component);
                //handleOnPaymentCompleted(result, component);
            },
            onPaymentFailed: (result, component) => {
                console.info("onPaymentFailed", result, component);
                handleOnPaymentFailed(result, component);
            },
            onAdditionalDetails: async (state, component, actions) => {
                console.info("onAdditionalDetails", state, component);
                try {
                    const { resultCode } = await fetch("/api/payments/details", {
                        method: "POST",
                        body: state.data ? JSON.stringify(state.data) : "",
                        headers: {
                            "Content-Type": "application/json",
                        }
                    }).then(response => response.json());

                    if (!resultCode) {
                        console.warn("reject");
                        actions.reject();
                    }

                    actions.resolve({ resultCode });
                } catch (error) {
                    console.error(error);
                    actions.reject();
                }
            }
        };

        // Optional configuration for cards
        const paymentMethodsConfiguration = {
            card: {
                showBrandIcon: true,
                hasHolderName: true,
                holderNameRequired: true,
                name: "Credit or debit card",
                amount: {
                    value: 9998,
                    currency: "EUR",
                },
                placeholders: {
                    cardNumber: '1234 5678 9012 3456',
                    expiryDate: 'MM/YY',
                    securityCodeThreeDigits: '123',
                    securityCodeFourDigits: '1234',
                    holderName: 'Developer Relations Team'
                }
            }
        };

        // Start the AdyenCheckout and mount the element onto the `payment`-div.
        const adyenCheckout = await AdyenCheckout(configuration);
        const dropin = new Dropin(adyenCheckout, { paymentMethodsConfiguration: paymentMethodsConfiguration }).mount(document.getElementById("payment"));
    } catch (error) {
        console.error(error);
        alert("Error occurred. Look at console for details.");
    }
}

function handleOnPaymentCompleted(response) {
    switch (response.resultCode) {
        case "Authorised":
            window.location.href = "/result/success";
            break;
        case "Pending":
        case "Received":
            window.location.href = "/result/pending";
            break;
        default:
            window.location.href = "/result/error";
            break;
    }
}
// Step 10 - Function to handle payment failure redirects
function handleOnPaymentFailed(response) {
    switch (response.resultCode) {
        case "Cancelled":
        case "Refused":
            window.location.href = "/result/failed";
            break;
        default:
            window.location.href = "/result/error";
            break;
    }
}

function handlePaySubscription() {

    const recurringDetailRef = document.getElementById("recurringDetailRef").value;
    const shopperReference = document.getElementById("shopperReference").value;

    if (!recurringDetailRef) {
        alert("Please enter a recurringDetailRef");
        return;
    }

    fetch("/api/subscription-payment", {
    method: "POST",
    headers: {
        "Content-Type": "application/json" // 👈 Add this line!
    },
    body: JSON.stringify({
        recurringDetailRef: recurringDetailRef,
        shopperReference: shopperReference
    })
})
.then(response => response.json())
.then(result => {
    if (result.resultCode === "Authorised") {
        document.body.innerHTML += "<p>Subscription payment successful!</p>";
    } else {
        document.body.innerHTML += "<p>Subscription payment failed: " + result.resultCode + "</p>";
    }
})
.catch(error => {
    console.error(error);
    alert("Error occurred. Look at console for details.");
});
}

function handleCancelSubscription() {
    const recurringDetailRef = document.getElementById("recurringDetailRef").value;

    fetch("/api/subscription-cancel", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            recurringDetailRef: recurringDetailRef,
            shopperReference: "test-shopper-1234" // Lembre-se: tem que ser o mesmo do cliente!
        })
    })
    .then(response => response.json())
    .then(result => {
        // A Adyen retorna response = "[detailSuccessfullyDisabled]" quando dá certo
        if (result.response === "[detail-successfully-disabled]") {
            alert("Assinatura cancelada com sucesso!");
        } else {
            alert("Erro ao cancelar: " + result.response);
        }
    })
    .catch(error => console.error("Erro:", error));
}


function getModificationData() {
    const pspReference = document.getElementById("paymentPspReference").value;
    const value = parseInt(document.getElementById("modificationValue").value, 10);
    
    if (!pspReference) {
        alert("Please enter a valid PSP Reference!");
        return null;
    }
    
    // Retorna exatamente no formato que a classe ModificationRequestDTO do Java espera
    return {
        paymentPspReference: pspReference,
        value: value
    };
}

function handleCapture() {
    const data = getModificationData();
    if (!data) return;

    fetch("/api/capture", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        document.getElementById("modification-result").innerHTML = 
            "✅ Capture requested! Status: " + result.status + " (Check webhooks for final result)";
    })
    .catch(error => console.error(error));
}

function handleModifyAmount() {
    const data = getModificationData();
    if (!data) return;

    fetch("/api/modify-amount", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        document.getElementById("modification-result").innerHTML = 
            "✅ Amount Modification requested! Status: " + result.status + " (Check webhooks)";
    })
    .catch(error => console.error(error));
}

function handleCancelPreAuth() {
    const data = getModificationData();
    if (!data) return;

    fetch("/api/cancel", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data) // A API de cancel não precisa estritamente do 'value', mas enviamos junto sem problemas
    })
    .then(response => response.json())
    .then(result => {
        document.getElementById("modification-result").innerHTML = 
            "✅ Cancel requested! Status: " + result.status + " (Check webhooks)";
    })
    .catch(error => console.error(error));
}

function handleRefund() {
    const data = getModificationData();
    if (!data) return;

    fetch("/api/refund", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        document.getElementById("modification-result").innerHTML = 
            "✅ Refund requested! Status: " + result.status + " (Check webhooks)";
    })
    .catch(error => console.error(error));
}

// Step 9 - Call the function to

startCheckout();