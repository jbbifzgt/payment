package lion.payment;

public interface IPaymentHandler {

	IPaymentOrder getOrder(String id);

	void onOrderPaied(String orderId, String transId);

	boolean isOrderPaied(String orderId);

	boolean isOrderPayable(String orderId);

	String getMerId(String port);

	String getKey(String port);

	void setOrderPaymentPort(String orderId, String type);

	Object getMerEmail();
}
