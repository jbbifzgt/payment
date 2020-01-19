package lion.payment.port.chinabank;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
import lion.payment.anno.PaymentPort;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author carryon
 * @time 2013 2013-3-21 下午08:13:23
 * @mail hl_0074@sina.com
 * @desc "网银在线" 支付接口
 */
@PaymentPort("chinabank")
public class ChinabankPort implements IPaymentPort {

	private static final String PORT_NAME = "chinabank";
	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		MapJ orderParam = new MapJ();

		orderParam.put("v_mid", portConfig.getString("merId", ""));
		orderParam.put("v_oid", order.getId());
		orderParam.put("v_amount", order.getAmount());
		orderParam.put("v_moneytype", "CNY");
		orderParam.put("v_url", Lang.nvl(portConfig.getString("notifyURL"), PaymentConfig.DEFAULT_NOTIFY_URL));

		String sign = singMap(orderParam);
		orderParam.put("v_md5info", sign);

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();

		// check status
		String pstatus = param.getString("v_pstatus");
		if (!"20".equals(pstatus)) { return "error"; }

		// check md5 sign
		String md5str = param.getString("v_md5str", "");
		String md5 = signResult(param);
		if (!md5str.equals(md5)) { return "error"; }

		// success
		String orderId = param.getString("v_oid");

		if (!PaymentConfig.paymentHandler.isOrderDealed(orderId)) {
			PaymentConfig.paymentHandler.onSuccess(orderId, "");
		}

		return "ok";
	}

	@Override
	public String refund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[网银在线]在线退款");
	}

	@Override
	public String notifyRefund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[网银在线]在线退款");
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

	private String singMap(MapJ orderParam) {

		StringBuffer sbu = new StringBuffer();
		sbu.append(orderParam.getString("v_amount"));
		sbu.append(orderParam.getString("v_moneytype"));
		sbu.append(orderParam.getString("v_oid"));
		sbu.append(orderParam.getString("v_mid"));
		sbu.append(orderParam.getString("v_url"));

		return DigestUtils.md5Hex(sbu.toString() + portConfig.getString("key"));
	}

	private String signResult(MapJ param) {

		StringBuffer sbu = new StringBuffer();
		sbu.append(param.getString("v_oid"));
		sbu.append(param.getString("v_pstatus"));
		sbu.append(param.getString("v_amount"));
		sbu.append(param.getString("v_moneytype"));

		return DigestUtils.md5Hex(sbu.toString() + portConfig.getString("key"));
	}

}
