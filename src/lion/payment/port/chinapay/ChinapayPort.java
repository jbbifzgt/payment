package lion.payment.port.chinapay;

import java.util.Date;

import lion.dev.io.FileUtil;
import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
import lion.payment.anno.PaymentPort;

import org.apache.commons.lang.StringUtils;

import chinapay.PrivateKey;
import chinapay.SecureLink;

/**
 * @author carryon
 * @time 2013 2013-3-21 下午08:14:16
 * @mail hl_0074@sina.com
 * @desc "银联在线" 支付接口
 */
@PaymentPort("chinapay")
public class ChinapayPort implements IPaymentPort {

	private static final String PORT_NAME = "chinapay";

	private SecureLink se;
	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();
		MapJ param = new MapJ();
		param.put("MerId", portConfig.getString("merId"));
		param.put("OrdId", order.getId());
		param.put("TransAmt", StringUtils.leftPad((order.getAmount() * 100) + "", 12, "0"));
		param.put("CuryId", "156");// 人民币
		param.put("TransDate", Formater.formatDate("yyyyMMdd", new Date()));
		param.put("TransType", "0001");// 消费交易
		param.put("Version", "20070129");
		param.put("BgRetUrl", portConfig.getString("notifyURL", PaymentConfig.DEFAULT_NOTIFY_URL));

		String checkValue = se.signOrder(param.getString("MerId"), param.getString("OrdId"), param.getString("TransAmt"), param.getString("CuryId"), param.getString("TransDate"),
				param.getString("TransType"));

		param.put("ChkValue", checkValue);

		String result = buildPage(param, portConfig.getString("orderURL"));

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();

		String transDate = param.getString("transdate");
		String merId = param.getString("merid");
		String ordId = param.getString("orderno");
		String transType = param.getString("transtype");
		String transAmt = param.getString("amount");
		String curyId = param.getString("currencycode");
		String orderStatus = param.getString("status");
		String chkValue = param.getString("checkvalue");

		boolean flag = se.verifyTransResponse(merId, ordId, transAmt, curyId, transDate, transType, orderStatus, chkValue);
		if (flag && !PaymentConfig.paymentHandler.isOrderDealed(ordId)) {
			PaymentConfig.paymentHandler.onSuccess(ordId, "");
		}

		return null;
	}

	private String buildPage(MapJ orderParam, String actionURL) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=" + portConfig.getString("charset", "UTF-8") + "'></head><body>");

		buf.append("<form action='" + actionURL + "' method='POST'>");
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
		if (se == null) {
			PrivateKey privateKey = new PrivateKey();

			privateKey.buildKey(portConfig.getString("merId"), 0, FileUtil.toUnixPath(ChinapayPort.class.getClassLoader().getResource("").getPath() + portConfig.getString("key")));
			se = new SecureLink(privateKey);
		}
	}

	@Override
	public String refund(MapJ param) {

		if (!portConfig.getBoolean("refund")) { return ""; }

		init();

		String orderId = param.getString("orderIds");
		if (StringUtils.isBlank(orderId)) { return ""; }
		IPaymentOrder order = PaymentConfig.paymentHandler.getOrder(orderId);
		if (order == null) { return ""; }

		MapJ orderMap = new MapJ();
		orderMap.put("MerID", portConfig.getString("merId"));
		orderMap.put("TransType", "0002");// 首次退款
		orderMap.put("OrderId", order.getId());
		orderMap.put("RefundAmount", StringUtils.leftPad(order.getAmount() * 100 + "", 12));
		orderMap.put("TransDate", Formater.formatDate("yyyyMMdd", order.getTransDate()));
		orderMap.put("Version", "20070129");
		orderMap.put("ReturnURL", portConfig.getString("siteurl") + "pay/refund/notify");
		orderMap.put("Priv1", PaymentConfig.paymentHandler.getRefundNo());

		String chkValue = se.Sign(orderMap.getString("merId") + orderMap.getString("TransDate") + orderMap.getString("TransType") + order.getId()
				+ orderMap.getString("RefundAmount") + orderMap.getString("Priv1"));

		orderMap.put("ChkValue", chkValue);

		String result = buildPage(orderMap, portConfig.getString("refundURL"));

		return result;
	}

	@Override
	public String notifyRefund(MapJ param) {

		init();

		String responseCode = param.getString("ResponseCode");
		if (!"0".equals(responseCode)) { return "退款失败" + param.getString("Message"); }

		String orderId = param.getString("OrderId");
		String checkValue = param.getString("CheckValue");
		String data = param.getString("MerID") + param.getString("TransDate") + param.getString("TransType") + orderId + param.getString("RefundAmount") + param.getString("Priv1");
		if (!se.verifyAuthToken(data, checkValue)) { return "验证失败"; }

		if (!PaymentConfig.paymentHandler.isOrderRefund(orderId)) {
			PaymentConfig.paymentHandler.onRefundSuccess(orderId, param.getString("Priv1"));
		}

		return "success";
	}
}
