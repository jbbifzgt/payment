package lion.payment.port.pre.paypal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import lion.dev.lang.MapJ;
import lion.framework.core.conf.Config;
import lion.framework.core.conf.ConfigManager;
import lion.payment.HttpUtil;
import lion.payment.IPaymentHandler;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.anno.PaymentHandlers;
import lion.payment.anno.PaymentPort;

/**
 * @author carryon
 * @time 2013 2013-3-16 下午11:32:15
 * @mail hl_0074@sina.com
 * @desc 宝贝支付接口
 */
@PaymentPort("paypal")
public class PaypalPort implements IPaymentPort {

	private static final String PORT_NAME = "paypal";
	private static final String PORT_URL = "https://www.paypal.com/cgi-bin/webscr";

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		MapJ orderParam = new MapJ();
		orderParam.put("business", handler.getMerId(order.getPortName()));// 接收付款的账号
		orderParam.put("item_name", order.getOrderName());
		orderParam.put("invoice", order.getId());
		orderParam.put("amount", order.getAmount());
		orderParam.put("no_note", "");
		orderParam.put("custom", "");

		String serverurl = config.getString("framework.payment.serverurl", "");
		if (!serverurl.endsWith("/")) {
			serverurl += "/";
		}
		orderParam.put("notify_url", serverurl + "payment/pay/notify");
		orderParam.put("rm", "2");

		orderParam.put("currency_code", "USD");
		orderParam.put("cmd", "_xclick");
		orderParam.put("charset", "utf-8");

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		// 获取参数
		StringBuffer sbu = new StringBuffer();
		sbu.append("cmd=_notify-validate");
		for (String key : param.keySet()) {
			try {
				sbu.append("&" + key + "=" + URLEncoder.encode(param.getString(key), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}

		// 验证请求
		String res = new HttpUtil().get(PORT_URL + "?" + sbu.toString());

		String orderId = param.getString("invoice", "");

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		if ("verified".equalsIgnoreCase(res) && !handler.isOrderPaied(orderId)) {
			// 支付成功
			String txnId = param.getString("txn_id", "");
			handler.onOrderPaied(orderId, txnId);

			return "success";

		} else if ("invalid".equalsIgnoreCase(res)) {
			// 支付失败
			return "fail";
		}

		return "error";
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'></head><body>");

		buf.append("<form action='" + PORT_URL + "' method='POST' target='_blank'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");
		return buf.toString();
	}

	@Override
	public String getOrderId(MapJ param) {

		return param.getString("invoice");
	}
}
