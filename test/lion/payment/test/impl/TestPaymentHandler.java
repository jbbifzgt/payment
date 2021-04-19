package lion.payment.test.impl;

import java.util.HashMap;
import java.util.Map;

import lion.payment.IPaymentHandler;
import lion.payment.anno.PaymentHandler;

@PaymentHandler("test")
public class TestPaymentHandler implements IPaymentHandler {

	private String merId = "879905060103109788";
	private String merKey = "99billKeyForTest";
	// paypal
	// private String merId = "10012226645";
	// private String merKey =
	// "w79txMr0ql14p709ADuQ7a9Hk3Dc6G2225021w79ws3G9oPSy20U92QimbLy";

	// yeepay
	// private String merId = "10012226645";
	// private String merKey =
	// "w79txMr0ql14p709ADuQ7a9Hk3Dc6G2225021w79ws3G9oPSy20U92QimbLy";

	// ips
	// private String merId = "000015";
	// private String merKey =
	// "GDgLwwdK270Qj1w4xho8lyTpRQZV9Jm5x4NwWOTThUa4fMhEBK9jOXFrKRT6xhlJuU2FEa89ov0ryyjfJuuPkcGzO5CeVx5ZIrkkt1aBlZV36ySvHOMcNv8rncRiy3DQ";

	// icpay
	// private String merId = "222378";
	// private String merKey =
	// "00518847228994856151214381286034373160268923638865209509623755128452179689329064232083487454640280528679651027955842303507571503";

	// private String merId = "2088901893133434";
	// private String merKey = "m45wchdx03vc5kecfu3h6rko3hx4lckw";
	// private String merEmail = "admin@fangchancaifu.com";

	// private String merId = "1900000113";
	// private String merKey = "e82573dc7e6136ba414f2e2affbe39fa";
	private String merEmail = "admin@fangchancaifu.com";

	private Map<String, Order> orders = new HashMap<>();

	@Override
	public Order getOrder(String id) {

		Order order = orders.get(id);

		if (order == null) {
			order = new Order();
			order.setId(id);
			order.setAmount(0.1);
			order.setOrderDescribtion("");
			order.setProductId("3333");
			order.setOrderName("测试支付");
			order.setStatus(1);
			orders.put(id, order);
		}

		return order;
	}

	@Override
	public void onOrderPaied(String orderId, String transId) {

		System.out.println("订单号为：" + orderId + " 的订单支付成功");

		Order order = this.getOrder(orderId);
		order.setStatus(2);
	}

	@Override
	public boolean isOrderPaied(String orderId) {

		Order order = this.getOrder(orderId);
		return order.getStatus() == 2;
	}

	@Override
	public String getMerId(String port) {

		return merId;
	}

	@Override
	public String getKey(String port) {

		return merKey;
	}

	@Override
	public boolean isOrderPayable(String orderId) {

		Order order = this.getOrder(orderId);
		return order.getStatus() == 1;
	}

	@Override
	public void setOrderPaymentPort(String orderId, String port) {

		Order order = this.getOrder(orderId);
		order.setPort(port);
	}

	@Override
	public Object getMerEmail() {

		return merEmail;
	}

}
