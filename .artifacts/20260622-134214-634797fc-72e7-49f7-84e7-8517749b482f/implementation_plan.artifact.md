# PayOS Online Payment Integration Plan

This plan outlines the integration of PayOS as an online payment gateway for the Matcha Vibe app, replacing the current simulated QR payment. The integration uses Firebase Cloud Functions as a secure bridge between the Android app and PayOS APIs.

## User Review Required

> [!IMPORTANT]
> - **Webhook URL**: After deploying the Cloud Functions, you must register the webhook URL (e.g., `https://<region>-<project-id>.cloudfunctions.net/payosWebhook`) in your PayOS Dashboard.
> - **PayOS Credentials**: I will use the credentials you provided (`clientId`, `apiKey`, `checksumKey`) in the Cloud Functions code.

## Proposed Changes

### Backend: Firebase Cloud Functions

We will create a new `functions` directory at the project root to host the payment logic.

#### [index.js](file:///D:/Mobile/Mobile/functions/index.js) [NEW]
- `createPaymentLink`: A callable function that takes order details, generates a unique `orderCode` (based on timestamp), updates the Firestore order, and returns a PayOS checkout URL.
- `payosWebhook`: An HTTPS trigger that PayOS calls upon successful payment. It verifies the signature and updates the order status in Firestore.

#### [package.json](file:///D:/Mobile/Mobile/functions/package.json) [NEW]
- Add dependencies: `firebase-functions`, `firebase-admin`, `@payos/node`.

---

### Frontend: Android App

#### [Order.kt](file:///D:/Mobile/Mobile/app/src/main/java/com/example/matcha_vibe/model/Order.kt)
- Add `orderCode: Long = 0` to the `Order` data class. This numeric ID is required by PayOS.

#### [build.gradle.kts](file:///D:/Mobile/Mobile/app/build.gradle.kts)
- Add `com.google.firebase:firebase-functions` dependency.

#### [FirebaseHelper.kt](file:///D:/Mobile/Mobile/app/src/main/java/com/example/matcha_vibe/FirebaseHelper.kt)
- Ensure `placeOrder` uses the order's `timestamp` or a generated `Long` as `orderCode` before saving to Firestore.
- Update `placeOrder` to return the `orderId` to the caller.

#### [CartFragment.kt](file:///D:/Mobile/Mobile/app/src/main/java/com/example/matcha_vibe/fragment/CartFragment.kt)
- In `processCheckout`, if `paymentMethod` is `QR_CODE`, call the `createPaymentLink` Cloud Function after successfully placing the order in Firestore.
- Use an `Intent` to open the returned `checkoutUrl` in the device's browser or Chrome Custom Tabs.

---

## Verification Plan

### Automated Tests
- I will perform a static analysis of the Kotlin and Node.js code to ensure there are no syntax errors.

### Manual Verification
- **Functional Check**: Verify that `CartFragment` correctly initiates the Cloud Function call.
- **Link Generation**: Check if the Cloud Function correctly constructs the PayOS request payload and returns a valid URL format.
- **Webhook Logic**: Simulate a PayOS webhook payload locally to verify that it correctly updates the Firestore document status.
