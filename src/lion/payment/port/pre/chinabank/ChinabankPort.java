package lion.payment.port.pre.chinabank;

import lion.dev.lang.MapJ;
import lion.framework.core.conf.Config;
import lion.framework.core.conf.ConfigManager;
import lion.framework.core.web.exception.WebException;
import lion.payment.IPaymentHandler;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.anno.PaymentHandlers;
import lion.payment.anno.PaymentPort;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author carryon
 * @time 2013 2013-3-21 下午08:13:23
 * @mail hl_0074@sina.com
 * @desc "网银在线" 支付接口
 */

// FIXME 无测试账号进行测试
@PaymentPort("chinabank")
public class ChinabankPort implements IPaymentPort {

	private static final String PORT_NAME = "chinabank";
	private static final String PORT_URL = "https://pay3.chinabank.com.cn/PayGate";

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		MapJ orderParam = new MapJ();

		orderParam.put("v_mid", handler.getMerId(order.getPortName()));
		orderParam.put("v_oid", order.getId());
		orderParam.put("v_amount", order.getAmount());
		orderParam.put("v_moneytype", "CNY");
		if (StringUtils.isNotBlank(paymentChannel)) {
			orderParam.put("pmode_id", paymentChannel);
		}

		String serverurl = config.getString("framework.payment.serverurl", "");
		orderParam.put("v_url", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify");
		orderParam.put("remark2", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify");

		String sign = singMap(orderParam, handler.getKey(PORT_NAME));
		orderParam.put("v_md5info", sign);

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return "error"; }

		String orderId = param.getString("v_oid");
		IPaymentOrder order = handler.getOrder(orderId);
		if (order == null) { throw new WebException("Order Not Found"); }

		// check status
		String pstatus = param.getString("v_pstatus");
		if (!"20".equals(pstatus)) { return "ok"; }

		// check md5 sign
		String md5str = param.getString("v_md5str", "");
		String md5 = signResult(param, handler.getKey(PORT_NAME));
		if (!md5str.equals(md5)) { return "error"; }

		// success
		if (!handler.isOrderPaied(orderId)) {
			handler.onOrderPaied(orderId, "");
		}

		return "ok";
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();

		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body>");

		buf.append("<form action='" + PORT_URL + "' method='POST'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");

		return buf.toString();
	}

	private String singMap(MapJ orderParam, String merKey) {

		StringBuffer sbu = new StringBuffer();
		sbu.append(orderParam.getString("v_amount"));
		sbu.append(orderParam.getString("v_moneytype"));
		sbu.append(orderParam.getString("v_oid"));
		sbu.append(orderParam.getString("v_mid"));
		sbu.append(orderParam.getString("v_url"));

		return DigestUtils.md5Hex(sbu.toString() + merKey).toUpperCase();
	}

	private String signResult(MapJ param, String merKey) {

		StringBuffer sbu = new StringBuffer();
		sbu.append(param.getString("v_oid"));
		sbu.append(param.getString("v_pstatus"));
		sbu.append(param.getString("v_amount"));
		sbu.append(param.getString("v_moneytype"));

		return DigestUtils.md5Hex(sbu.toString() + merKey);
	}

	@Override
	public String getOrderId(MapJ param) {

		return param.getString("v_oid");
	}

}
