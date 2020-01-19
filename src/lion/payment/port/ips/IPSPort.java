package lion.payment.port.ips;

import java.util.Date;

import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
import lion.payment.anno.PaymentPort;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author carryon
 * @time 2013 2013-3-17 下午07:58:54
 * @mail hl_0074@sina.com
 * @desc 环迅支付接口
 */
@PaymentPort("ips")
public class IPSPort implements IPaymentPort {

	private static final String PORT_NAME = "ips";

	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		MapJ orderParam = new MapJ();
		orderParam.put("Mer_code", portConfig.getString("merId", ""));
		orderParam.put("Billno", order.getId());
		orderParam.put("Amount", order.getAmount());
		orderParam.put("Date", Formater.formatDate("yyyyMMdd", new Date()));
		orderParam.put("Currency_Type", "RMB");
		orderParam.put("Gateway_Type", "01");
		orderParam.put("OrderEncodeType", 5);
		orderParam.put("RetEncodeType", 17);
		orderParam.put("Rettype", 1);
		orderParam.put("ServerUrl", portConfig.getString("notifyURL", PaymentConfig.DEFAULT_NOTIFY_URL));

		String sign = signMap(orderParam);
		orderParam.put("SignMD5", sign);

		String result = buildPage(orderParam);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();
		String succ = param.getString("succ");
		if (!"Y".equals(succ)) { return "支付未成功"; }

		String billno = param.getString("billno");
		String txn_id = param.getString("ipsbillno");

		String md5 = signResponse(param);

		String signature = param.getString("signature");
		if (!md5.equalsIgnoreCase(signature)) { return ""; }

		if (!PaymentConfig.paymentHandler.isOrderDealed(billno)) {
			PaymentConfig.paymentHandler.onSuccess(billno, txn_id);
		}

		return "订单：" + billno + " 已支付成功";
	}

	private String signResponse(MapJ param) {

		StringBuffer buf = new StringBuffer();
		buf.append(param.getString("billno"));
		buf.append(param.getString("Currency_type"));
		buf.append(param.getString("amount"));
		buf.append(param.getString("date"));
		buf.append(param.getString("succ"));
		buf.append(param.getString("ipsbillno"));
		buf.append(param.getString("retencodetype"));

		buf.append(portConfig.getString("key"));

		return DigestUtils.md5Hex(buf.toString());
	}

	private void init() {

		if (portConfig == null) {
			portConfig = PaymentConfig.getPortConfig(PORT_NAME);
		}
	}

	private String signMap(MapJ orderParam) {

		StringBuffer sbu = new StringBuffer();

		sbu.append(orderParam.getString("Billno"));
		sbu.append(orderParam.getString("Currency_Type"));
		sbu.append(orderParam.getString("Amount"));
		sbu.append(orderParam.getString("Date"));
		sbu.append(orderParam.getString("OrderEncodeType"));
		sbu.append(portConfig.getString("key"));

		return DigestUtils.md5Hex(sbu.toString());
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

		throw new UnsupportedOperationException("不支持当前网关[ips]在线退款");
	}

	@Override
	public String notifyRefund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[ips]在线退款");
	}

}
