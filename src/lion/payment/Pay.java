package lion.payment;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lion.dev.lang.MapJ;
import lion.framework.core.web.anno.Path;

/**
 * Servlet implementation class Pay
 */

public class Pay implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 提交支付信息
	 */
	@Path("/pay/commit")
	public void commit(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String orderId = request.getParameter("orderId");
		String channel = request.getParameter("channel");

		IPaymentOrder order = PaymentConfig.paymentHandler.getOrder(orderId);

		IPaymentPort port = PaymentConfig.getPort(PaymentConfig.DEFAULT_PAYMENT_PORT);

		String requestContent = port.buildRequestPage(order, channel);

		out(response, requestContent);
	}

	/**
	 * 通知商户支付结果
	 */
	@Path("/pay/notify")
	public void notify(HttpServletRequest request, HttpServletResponse response) throws Exception {

		MapJ param = MapJ.mapRequest(request.getParameterMap());

		IPaymentPort port = PaymentConfig.getPort(PaymentConfig.DEFAULT_PAYMENT_PORT);
		if (port == null) { return; }

		String result = port.notifyPaymentResult(param);

		out(response, result);
	}

	/**
	 * 退款操作
	 */
	@Path("/pay/refund")
	public void refund(HttpServletRequest request, HttpServletResponse response) throws Exception {

		MapJ param = MapJ.mapRequest(request.getParameterMap());
		IPaymentPort port = PaymentConfig.getPort(PaymentConfig.DEFAULT_PAYMENT_PORT);
		if (port == null) { return; }

		String result = port.refund(param);

		out(response, result);

	}

	@Path("/pay/refund/notify")
	public void refundNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {

		MapJ param = MapJ.mapRequest(request.getParameterMap());
		IPaymentPort port = PaymentConfig.getPort(PaymentConfig.DEFAULT_PAYMENT_PORT);
		if (port == null) { return; }

		String result = port.notifyRefund(param);

		out(response, result);

	}

	private void out(HttpServletResponse response, String content) throws IOException {

		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP
																					// 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setDateHeader("Expires", 0); // Proxies.

		response.getWriter().write(content);
	}
}
