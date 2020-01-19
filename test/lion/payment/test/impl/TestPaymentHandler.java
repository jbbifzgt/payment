package lion.payment.test.impl;

import java.net.MalformedURLException;
import java.net.URL;

import lion.payment.IPaymentHandler;
import lion.payment.IPaymentOrder;
import lion.payment.anno.PaymentHandler;

@PaymentHandler
public class TestPaymentHandler implements IPaymentHandler {

	@Override
	public IPaymentOrder getOrder(String id) {

		Order order = new Order();
		order.setId(id);
		order.setAmount(30);
		order.setOrderDescribtion("");
		order.setProductId("333");
		order.setOrderName("KTV1小时欢唱");

		return order;
	}

	@Override
	public void onSuccess(String orderId, String transId) {

		System.out.println("订单号为：" + orderId + " 的订单支付成功");

	}

	public static void main(String[] args) throws MalformedURLException {

		URL url = new URL("http://www.baidu.com/index.php");

		System.out.println(url.getHost());

	}

	@Override
	public boolean isOrderDealed(String orderId) {

		return false;
	}

	@Override
	public String getRefundNo() {

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onRefundSuccess(String order, String batchNo) {

		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isOrderRefund(String orderId) {

		// TODO Auto-generated method stub
		return false;
	}

}
