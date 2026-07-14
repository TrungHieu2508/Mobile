# Implementation Plan - PayOS & Firestore Unit Testing

As a Senior Android Developer, I will implement a suite of Local Unit Tests to verify the payment processing and order placement logic. This ensures that the integration between PayOS (for payment link generation) and Firebase (for order storage and status updates) works correctly under various scenarios.

## User Review Required

> [!IMPORTANT]
> - The tests use `mockkObject` to mock Singleton objects (`FirebaseHelper`, `PayOSHelper`). This is necessary due to the current architecture but is a "heavy" testing approach.
> - I am assuming the `Order` and `CartItem` models found in the codebase are the definitive versions for these tests.

## Proposed Changes

### [Test Suite]

#### [PayOSPaymentTest.kt](file:///D:/Mobile_2/Mobile_2/app/src/test/java/com/example/matcha_vibe/PayOSPaymentTest.kt)

- **Purpose**: To test the interaction between `CartFragment` logic (abstracted), `PayOSHelper`, and `FirebaseHelper`.
- **Test Scenarios**:
    1. **Happy Path**: Successful order placement followed by successful PayOS link creation. Verifies that `updateOrderCode` is called and the local payment status reflects success (simulating server-side update logic).
    2. **PayOS Error Case**: Successful order placement but PayOS fails to generate a link. Verifies that no further database updates (like `updateOrderCode`) occur and the error is captured.
    3. **Firestore Error Case**: Successful PayOS link generation but a subsequent Firestore update (e.g., `updateOrderCode`) fails. Verifies that the system handles the exception gracefully without crashing.

## Verification Plan

### Automated Tests
- Run the unit tests using the following command (PowerShell):
  ```powershell
  ./gradlew :app:testDebugUnitTest --tests "com.example.matcha_vibe.PayOSPaymentTest"
  ```
- All 3 test cases must pass.

### Manual Verification
- Review the test code to ensure it follows the **Given-When-Then** pattern and uses `coEvery`/`coVerify` correctly for mocking callbacks.
