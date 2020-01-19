package lion.payment;

public interface IPaymentHandler {

	IPaymentOrder getOrder(String id);

	void onSuccess(String orderId, String transId);

	boolean isOrderDealed(String orderId);

	String getRefundNo();

	void onRefundSuccess(String order, String batchNo);

	boolean isOrderRefund(String orderId);
}
