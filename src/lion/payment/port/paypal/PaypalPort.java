package lion.payment.port.paypal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.payment.HttpUtil;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
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

	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		MapJ orderParam = new MapJ();
		orderParam.put("business", portConfig.getString("merId", ""));// 接收付款的账号
		orderParam.put("item_name", order.getOrderName());
		orderParam.put("invoice", order.getId());
		orderParam.put("amount", order.getAmount());
		orderParam.put("no_note", "");
		orderParam.put("custom", "");

		orderParam.put("return", portConfig.getString("returnURL", ""));// 返回地址
		orderParam.put("notify_url", Lang.nvl(portConfig.getString("notifyURL"), PaymentConfig.DEFAULT_NOTIFY_URL));
		orderParam.put("rm", "2");

		orderParam.put("currency_code", "USD");
		orderParam.put("cmd", "_xclick");
		orderParam.put("charset", "utf-8");

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();

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
		String res = new HttpUtil().get(portConfig.getString("orderURL", "") + "?" + sbu.toString());

		String orderId = param.getString("invoice", "");
		if ("verified".equalsIgnoreCase(res) && !PaymentConfig.paymentHandler.isOrderDealed(orderId)) {
			// 支付成功
			String txnId = param.getString("txn_id", "");
			PaymentConfig.paymentHandler.onSuccess(orderId, txnId);

			return "success";

		} else if ("invalid".equalsIgnoreCase(res)) {
			// 支付失败
			return "fail";
		}

		return "error";
	}

	private void init() {

		if (portConfig == null) {
			portConfig = PaymentConfig.getPortConfig(PORT_NAME);
		}
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=" + portConfig.getString("charset", "UTF-8") + "'></head><body>");

		buf.append("<form action='" + portConfig.getString("orderURL") + "' method='POST' target='_blank'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");
		return buf.toString();
	}

	@Override
	public String refund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[paypal]在线退款");
	}

	@Override
	public String notifyRefund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[paypal]在线退款");
	}
}
