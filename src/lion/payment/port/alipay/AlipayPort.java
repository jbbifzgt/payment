package lion.payment.port.alipay;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.framework.core.conf.Config;
import lion.framework.core.conf.ConfigManager;
import lion.framework.core.web.exception.WebException;
import lion.payment.HttpUtil;
import lion.payment.IPaymentHandler;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.anno.PaymentHandlers;
import lion.payment.anno.PaymentPort;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author carryon
 * @time 2013 2013-3-16 下午11:31:36
 * @mail hl_0074@sina.com
 * @desc 支付宝支付接口
 */
@PaymentPort("alipay")
public class AlipayPort implements IPaymentPort {

	public static final String PORT_NAME = "alipay";

	private static final String VERIFY_NOTIFY_URL = "https://mapi.alipay.com/gateway.do?service=notify_verify&partner=%s&notify_id=%s";
	private static final String PORT_URL = "https://mapi.alipay.com/gateway.do?_input_charset=UTF-8";

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		MapJ orderParam = new MapJ();

		orderParam.put("service", "create_direct_pay_by_user");
		orderParam.put("partner", handler.getMerId(order.getPortName()));
		orderParam.put("_input_charset", "UTF-8");
		orderParam.put("out_trade_no", order.getId());

		orderParam.put("subject", order.getOrderName());
		orderParam.put("total_fee", order.getAmount());
		orderParam.put("seller_email", handler.getMerEmail());

		orderParam.put("payment_type", "1");
		if (StringUtils.isNotBlank(paymentChannel)) {
			orderParam.put("paymethod", "bankPay");
			orderParam.put("defaultbank", Lang.nvl(paymentChannel, "CMB"));
		} else {
			orderParam.put("paymethod", "directPay");
		}

		String serverurl = config.getString("framework.payment.serverurl", "");
		orderParam.put("notify_url", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify");

		orderParam.put("sign_type", "MD5");
		String sign = signMap(orderParam, handler.getKey(PORT_NAME));

		orderParam.put("sign_type", "MD5");
		orderParam.put("sign", sign);

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		// check trade status
		String tradeStatus = param.getString("trade_status", "");
		if (!("TRADE_FINISHED".equals(tradeStatus) || "TRADE_SUCCESS".equals(tradeStatus))) { return "success"; }// 支付失败不处理
		String orderId = param.getString("out_trade_no", "");
		String transId = param.getString("trade_no", "");

		// verify request source
		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }
		IPaymentOrder order = handler.getOrder(orderId);
		if (order == null) { throw new WebException("Order Not Found"); }

		String notifyId = param.getString("notify_id");
		if (StringUtils.isNotBlank(notifyId)) {
			String result = new HttpUtil().get(String.format(VERIFY_NOTIFY_URL, handler.getMerId(order.getPortName()), notifyId));
			if (!"true".equalsIgnoreCase(result.trim())) { return "fail"; }
		}

		// verify sign
		String sign = param.getString("sign", "");
		String mysign = signMap(param, handler.getKey(PORT_NAME));
		if (!StringUtils.equals(sign, mysign)) { return "fail"; }

		// on trade success
		if (!handler.isOrderPaied(orderId)) {
			handler.onOrderPaied(orderId, transId);
		}
		return "success";
	}

	private String signMap(MapJ orderParam, String merKey) {

		MapJ param = new MapJ();

		for (String key : orderParam.keySet()) {
			if (StringUtils.isBlank(orderParam.getString(key)) || StringUtils.equals("sign_type", key) || StringUtils.equals("sign", key)) {
				continue;
			}
			param.put(key, orderParam.get(key));
		}

		List<String> keys = new ArrayList<>(param.keySet());
		Collections.sort(keys);

		StringBuffer buf = new StringBuffer();
		for (String key : keys) {
			buf.append("&" + key + "=" + param.getString(key));
		}

		try {
			String linkString = StringUtils.stripStart(buf.toString(), "&");
			return DigestUtils.md5Hex((linkString + merKey).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'></head><body>");

		buf.append("<form action='" + PORT_URL + "' method='POST'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");

		return buf.toString();
	}

	@Override
	public String getOrderId(MapJ request) {

		return request.getString("out_trade_no");
	}
}
