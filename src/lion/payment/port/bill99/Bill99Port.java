package lion.payment.port.bill99;

import java.util.Date;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.dev.web.Validator;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
import lion.payment.anno.PaymentPort;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author carryon
 * @time 2013 2013-3-16 下午11:20:59
 * @mail hl_0074@sina.com
 * @desc 快钱支付接口
 */
@PaymentPort("99bill")
public class Bill99Port implements IPaymentPort {

	private static final String PORT_NAME = "99bill";

	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		MapJ orderParam = new MapJ();

		orderParam.put("inputCharset", 1);
		orderParam.put("bgUrl", portConfig.getString("notifyURL", PaymentConfig.DEFAULT_NOTIFY_URL));
		orderParam.put("version", "v2.0");
		orderParam.put("language", "1");
		orderParam.put("signType", "1");
		orderParam.put("merchantAccId", portConfig.getString("merId", ""));// 商户编号
		orderParam.put("orderId", order.getId());
		orderParam.put("orderAmount", Validator.toInt(String.valueOf(order.getAmount() * 100), 0));
		orderParam.put("orderTime", Formater.formatDate("yyyyMMddHHmmss", new Date()));

		orderParam.put("productName", Lang.nvl(order.getOrderName(), ""));
		orderParam.put("productId", Lang.nvl(order.getProductId(), ""));

		orderParam.put("payType", 10);
		if (StringUtils.isNotBlank(paymentChannel)) {
			orderParam.put("bankId", paymentChannel);
		}

		String sign = signMap(orderParam);
		orderParam.put("signMsg", sign);

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		if (portConfig == null) {
			init();
		}
		String payResult = param.getString("payResult");
		String txnId = param.getString("dealId");
		String orderId = param.getString("orderId");

		if (!"10".equals(payResult)) { return ""; }

		MapJ responseParam = new MapJ();
		responseParam.put("merchantAcctId", param.getString("merchantAcctId"));
		responseParam.put("version", param.getString("version"));
		responseParam.put("language", param.getString("language"));
		responseParam.put("signType", param.getString("signType"));
		responseParam.put("payType", param.getString("payType"));
		responseParam.put("bankId", param.getString("bankId"));
		responseParam.put("orderId", orderId);
		responseParam.put("orderTime", param.getString("orderTime"));
		responseParam.put("orderAmount", param.getString("orderAmount"));
		responseParam.put("dealId", txnId);
		responseParam.put("bankDealId", param.getString("bankDealId"));
		responseParam.put("dealTime", param.getString("dealTime"));
		responseParam.put("payAmount", param.getString("payAmount"));
		responseParam.put("fee", param.getString("fee"));
		responseParam.put("ext1", param.getString("ext1"));
		responseParam.put("ext2", param.getString("ext2"));
		responseParam.put("payResult", payResult);
		responseParam.put("errCode", param.getString("errCode"));

		String signMsg = param.getString("signMsg");
		String sign = signMap(responseParam);

		if (!StringUtils.equals(signMsg, sign)) { return ""; }

		if (!PaymentConfig.paymentHandler.isOrderDealed(orderId)) {
			PaymentConfig.paymentHandler.onSuccess(orderId, txnId);
		}

		String result = "<result>1</result><redirecturl>" + portConfig.getString("returnURL", "") + "</redirecturl>";

		return result;
	}

	private String signMap(MapJ orderParam) {

		StringBuffer sbu = new StringBuffer();

		for (String key : orderParam.keySet()) {
			String value = orderParam.getString(key);
			if (StringUtils.isBlank(value)) {
				continue;
			}
			sbu.append("&" + key + "=" + value);
		}

		sbu.append("&key=" + portConfig.getString("key"));

		return DigestUtils.md5Hex(sbu.substring(1));
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

	private void init() {

		if (portConfig == null) {
			portConfig = PaymentConfig.getPortConfig(PORT_NAME);
		}
	}

	@Override
	public String refund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[快钱]在线退款");
	}

	@Override
	public String notifyRefund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[快钱]在线退款");
	}
}
