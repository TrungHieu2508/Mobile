# Task List

- [/] Research and Planning
    - [x] Research PayOS integration for Node.js and Android
    - [x] Explore current project structure and checkout flow
    - [/] Create implementation plan
- [ ] Backend: Firebase Cloud Functions Implementation
    - [ ] Initialize `functions` directory and dependencies
    - [ ] Implement `createPaymentLink` function
    - [ ] Implement `payosWebhook` function
- [ ] Frontend: Android App Integration
    - [ ] Add Firebase Functions dependencies to `build.gradle.kts`
    - [ ] Update `Order` model to include `orderCode`
    - [ ] Update `FirebaseHelper.placeOrder` to return order ID or handle success correctly
    - [ ] Update `CartFragment.kt` to call Cloud Function and open payment link
    - [ ] Update `AndroidManifest.xml` for Deep Link handling (optional but recommended)
- [ ] Verification
    - [ ] Verify Cloud Functions logic
    - [ ] Verify Android integration flow
