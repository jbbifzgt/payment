package lion.payment;

import lion.framework.core.conf.Config;
import lion.framework.core.conf.ConfigManager;
import lion.framework.core.web.anno.Namespace;
import lion.framework.core.web.anno.Path;
import lion.framework.core.web.anno.Result;
import lion.framework.core.web.anno.View;
import lion.framework.core.web.request.Request;
import lion.framework.core.web.views.ResultType;
import lion.payment.anno.PaymentHandlers;
import lion.payment.anno.PaymentPorts;

import org.apache.commons.lang.StringUtils;

/**
 * 
 */
@Namespace("/payment/pay")
public class Pay {

	/**
	 * 提交支付信息
	 */
	@Path("commit")
	@Result(@View(type = ResultType.HTML))
	public String commit(Request request) throws Exception {

		String orderId = request.getString("orderId");
		String channel = request.getString("channel");
		String type = request.getString("type");// 支付接口类型

		if (StringUtils.isBlank(channel)) {
			channel = null;
		}

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		if (!handler.isOrderPayable(orderId)) {
			request.addModel("content", "当前订单状态无法进行支付");
			return "success";
		}

		IPaymentPort port = PaymentPorts.get(type);
		if (port == null) { return null; }

		handler.setOrderPaymentPort(orderId, type);

		IPaymentOrder order = handler.getOrder(orderId);

		String requestContent = port.buildRequestPage(order, channel);

		request.addModel("content", requestContent);

		return "success";
	}

	/**
	 * 通知商户支付结果
	 */
	@Path("notify")
	@Result(@View(type = ResultType.HTML))
	public String notify(Request request) throws Exception {

		String orderId = "";
		for (IPaymentPort port : PaymentPorts.getPorts()) {
			orderId = port.getOrderId(request);
			if (StringUtils.isNotBlank(orderId)) {
				break;
			}
		}
		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		IPaymentOrder order = handler.getOrder(orderId);
		IPaymentPort port = PaymentPorts.get(order.getPortName());
		if (port == null) { return "payment port not found"; }

		for (String key : request.keySet()) {
			request.put(key, new String(request.getString(key).getBytes("ISO-8859-1"), "UTF-8"));
		}

		request.addModel("content", port.notifyPaymentResult(request));

		return "success";
	}
}
