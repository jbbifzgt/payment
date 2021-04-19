package lion.payment.port.ips;

import java.util.Date;

import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.framework.core.conf.Config;
import lion.framework.core.conf.ConfigManager;
import lion.framework.core.web.exception.WebException;
import lion.payment.IPaymentHandler;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.anno.PaymentHandlers;
import lion.payment.anno.PaymentPort;

import org.apache.commons.lang.StringUtils;

/**
 * @author carryon
 * @time 2013 2013-3-17 下午07:58:54
 * @mail hl_0074@sina.com
 * @desc 环迅支付接口
 */
@PaymentPort("ips")
public class IPSPort implements IPaymentPort {

	private static final String PORT_NAME = "ips";
	private static final String PORT_URL = "https://pay.ips.com/ipayment.aspx";

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		MapJ orderParam = new MapJ();
		orderParam.put("Mer_code", handler.getMerId(PORT_NAME));
		orderParam.put("Billno", order.getId());
		orderParam.put("Amount", Formater.formateNumber("#0.00", order.getAmount()));
		orderParam.put("Date", Formater.formatDate("yyyyMMdd", new Date()));
		orderParam.put("Currency_Type", "RMB");
		orderParam.put("Gateway_Type", "01");
		orderParam.put("Lang", "GB");
		orderParam.put("OrderEncodeType", 5);
		orderParam.put("RetEncodeType", 17);
		orderParam.put("Rettype", 0);

		String serverurl = config.getString("framework.payment.serverurl", "");
		orderParam.put("ServerUrl", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify");

		String sign = signMap(orderParam, handler.getKey(PORT_NAME));
		orderParam.put("SignMD5", sign);

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		String succ = param.getString("succ");
		if (!"Y".equals(succ)) { return "success"; } // 支付失败不处理

		String orderId = param.getString("billno");
		String txn_id = param.getString("ipsbillno");

		IPaymentOrder order = handler.getOrder(orderId);
		if (order == null) { throw new WebException("Order Not Found"); }

		String md5 = signResponse(param, handler.getKey(PORT_NAME));

		String signature = param.getString("signature");
		if (!md5.equalsIgnoreCase(signature)) { return "fail"; }

		if (!handler.isOrderPaied(orderId)) {
			handler.onOrderPaied(orderId, txn_id);
		}
		return "success";
	}

	private String signResponse(MapJ param, String merKey) {

		StringBuffer buf = new StringBuffer();
		buf.append("billno" + param.getString("billno"));
		buf.append("currencytype" + param.getString("Currency_type"));
		buf.append("amount" + param.getString("amount"));
		buf.append("date" + param.getString("date"));
		buf.append("succ" + param.getString("succ"));
		buf.append("ipsbillno" + param.getString("ipsbillno"));
		buf.append("retencodetype" + param.getString("retencodetype"));
		buf.append(merKey);
		cryptix.jce.provider.MD5 md5 = new cryptix.jce.provider.MD5();
		return md5.toMD5(buf.toString());
	}

	private String signMap(MapJ orderParam, String merKey) {

		StringBuffer sbu = new StringBuffer();

		sbu.append("billno" + orderParam.getString("Billno"));
		sbu.append("currencytype" + orderParam.getString("Currency_Type"));
		sbu.append("amount" + orderParam.getString("Amount"));
		sbu.append("date" + orderParam.getString("Date"));
		sbu.append("orderencodetype" + orderParam.getString("OrderEncodeType"));
		sbu.append(merKey);

		cryptix.jce.provider.MD5 md5 = new cryptix.jce.provider.MD5();
		return md5.toMD5(sbu.toString()).toLowerCase();
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

		return param.getString("billno");
	}
}
