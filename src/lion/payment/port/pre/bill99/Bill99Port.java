package lion.payment.port.pre.bill99;

import java.util.Date;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.dev.web.Validator;
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
 * @time 2013 2013-3-16 下午11:20:59
 * @mail hl_0074@sina.com
 * @desc 快钱支付接口
 */
@PaymentPort("99bill")
public class Bill99Port implements IPaymentPort {

	private static final String PORT_NAME = "99bill";
	private static final String PORT_URL = "https://www.99bill.com/gateway/recvMerchantInfoAction.htm";

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		MapJ orderParam = new MapJ();

		orderParam.put("inputCharset", 1);

		String serverurl = config.getString("framework.payment.serverurl", "");
		orderParam.put("bgUrl", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify");
		orderParam.put("pageUrl", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/result");

		orderParam.put("version", "v2.0");
		orderParam.put("language", "1");
		orderParam.put("signType", "1");
		orderParam.put("merchantAccId", handler.getMerId(order.getPortName()));// 商户编号
		orderParam.put("orderId", order.getId());
		orderParam.put("orderAmount", Validator.toInt(String.valueOf(order.getAmount() * 100), 0));
		orderParam.put("orderTime", Formater.formatDate("yyyyMMddHHmmss", new Date()));

		orderParam.put("productName", Lang.nvl(order.getOrderName(), ""));

		orderParam.put("payType", 10);
		if (StringUtils.isNotBlank(paymentChannel)) {
			orderParam.put("bankId", paymentChannel);
		}

		String sign = signMap(orderParam, handler.getKey(PORT_NAME));
		orderParam.put("signMsg", sign);

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

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

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		IPaymentOrder order = handler.getOrder(orderId);
		if (order == null) { throw new WebException("Order Not Found"); }

		String sign = signMap(responseParam, handler.getKey(PORT_NAME));

		if (!StringUtils.equals(signMsg, sign)) { return ""; }

		if (!handler.isOrderPaied(orderId)) {
			handler.onOrderPaied(orderId, txnId);
		}

		String result = "<result>1</result>";

		return result;
	}

	private String signMap(MapJ orderParam, String merKey) {

		StringBuffer sbu = new StringBuffer();

		for (String key : orderParam.keySet()) {
			String value = orderParam.getString(key);
			if (StringUtils.isBlank(value)) {
				continue;
			}
			sbu.append("&" + key + "=" + value);
		}

		sbu.append("&key=" + merKey);

		return DigestUtils.md5Hex(StringUtils.stripStart(sbu.toString(), "&"));
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
	public String getOrderId(MapJ request) {

		return request.getString("orderId");
	}
}
