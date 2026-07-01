const functions = require("firebase-functions");
const admin = require("firebase-admin");
const PayOS = require("@payos/node");

admin.initializeApp();

// Khởi tạo PayOS với thông tin cấu hình từ bạn cung cấp
const payos = new PayOS(
  "f062487f-6f2c-49be-944d-de17e3a3bc12", // clientid
  "fe5103dc-2342-4258-bdf3-324aa07f5886", // API Key
  "7c0c74fd058f1b3ddab14050a06f973c64a544ee422dcb34f95d4531a397cae0" // Checksum Key
);

/**
 * 1. Hàm tạo link thanh toán PayOS (Callable Function)
 */
exports.createPaymentLink = functions.https.onCall(async (data, context) => {
  try {
    const { orderId, amount, description, returnUrl, cancelUrl } = data;

    if (!orderId || !amount) {
      throw new functions.https.HttpsError("invalid-argument", "Thiếu thông tin đơn hàng hoặc số tiền.");
    }

    // Tạo mã orderCode kiểu số (Long) từ thời gian hiện tại để không bị trùng lặp (PayOS yêu cầu orderCode là số)
    const orderCode = Number(String(Date.now()).slice(-6) + String(Math.floor(Math.random() * 1000)));

    // Chuẩn bị dữ liệu thanh toán PayOS
    const paymentBody = {
      orderCode: orderCode,
      amount: Math.round(amount),
      description: description || `Thanh toan Matcha Vibe`,
      returnUrl: returnUrl || "matchavibe://payment-success",
      cancelUrl: cancelUrl || "matchavibe://payment-cancel"
    };

    // Gọi API PayOS để tạo Link thanh toán
    const paymentLinkResponse = await payos.createPaymentLink(paymentBody);

    // Cập nhật mã orderCode vào tài liệu Đơn hàng trên Firestore để đối chiếu khi có Webhook
    await admin.firestore().collection("orders").document(orderId).update({
      orderCode: orderCode
    });

    return {
      success: true,
      checkoutUrl: paymentLinkResponse.checkoutUrl,
      orderCode: orderCode
    };

  } catch (error) {
    console.error("Lỗi tạo link thanh toán PayOS:", error);
    throw new functions.https.HttpsError("internal", error.message || "Không thể tạo link thanh toán PayOS.");
  }
});

/**
 * 2. Webhook xử lý thông báo thanh toán thành công từ PayOS (HTTPS Trigger)
 */
exports.payosWebhook = functions.https.onRequest(async (req, res) => {
  try {
    const webhookData = req.body;

    // 1. Kiểm tra và xác thực webhook từ PayOS (Tránh giả mạo)
    // Chú ý: PayOS gửi dữ liệu webhook chứa cấu trúc mã hóa để verify
    const verifiedData = payos.verifyPaymentWebhookData(webhookData);

    console.log("Dữ liệu Webhook hợp lệ nhận được từ PayOS:", verifiedData);

    const orderCode = verifiedData.orderCode;
    const isSuccess = verifiedData.desc === "success" || req.body.code === "00";

    if (isSuccess) {
      // 2. Tìm đơn hàng có chứa mã orderCode tương ứng trong Firestore
      const ordersSnapshot = await admin.firestore().collection("orders")
        .where("orderCode", "==", orderCode)
        .limit(1)
        .get();

      if (!ordersSnapshot.empty) {
        const orderDoc = ordersSnapshot.docs[0];

        // 3. Cập nhật trạng thái đơn hàng và trạng thái thanh toán thành công
        await orderDoc.ref.update({
          paymentStatus: "PAID",
          status: "PREPARING" // Chuyển sang trạng thái đang chuẩn bị đồ uống luôn
        });

        console.log(`Đã cập nhật đơn hàng ${orderDoc.id} sang PAID thành công.`);
      } else {
        console.warn(`Không tìm thấy đơn hàng nào có orderCode là: ${orderCode}`);
      }
    }

    // Luôn trả về phản hồi thành công (200 OK) cho PayOS biết hệ thống của bạn đã nhận được webhook
    return res.status(200).json({ success: true });

  } catch (error) {
    console.error("Lỗi xử lý Webhook PayOS:", error);
    // Vẫn trả về 200 hoặc 400 kèm log lỗi cụ thể để dễ debug
    return res.status(400).send(`Webhook Error: ${error.message}`);
  }
});
