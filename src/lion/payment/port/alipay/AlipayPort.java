package lion.payment.port.alipay;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.payment.HttpUtil;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
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

	private static final String VERIFY_NOTIFY_URL = "https://mapi.alipay.com/gateway.do?service=notify_verify&notify_id=";

	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		MapJ orderParam = new MapJ();

		orderParam.put("service", "create_direct_pay_by_user");
		orderParam.put("partner", portConfig.getString("merId", ""));
		orderParam.put("_input_charset", portConfig.getString("charset", "UTF-8"));
		orderParam.put("seller_id", portConfig.getString("aliId", ""));
		orderParam.put("out_trade_no", order.getId());

		orderParam.put("subject", order.getOrderName());
		orderParam.put("body", Lang.nvl(order.getOrderDescribtion(), ""));
		orderParam.put("total_fee", order.getAmount());

		orderParam.put("payment_type", Lang.nvl(order.getOrderCategory(), "1"));
		orderParam.put("defaultbank", paymentChannel);
		orderParam.put("paymethod", "bankPay");

		orderParam.put("notify_url", portConfig.getString("notifyURL", PaymentConfig.DEFAULT_NOTIFY_URL));
		orderParam.put("return_url", portConfig.getString("returnURL", ""));

		String sign = signMap(orderParam);
		orderParam.put("sign_type", "MD5");
		orderParam.put("sign", sign);

		String result = buildPage(orderParam);

		return result;

	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();
		// check success flag
		String trade_flag = param.getString("is_success", "");
		if (!"T".equals(trade_flag)) { return "fail"; }

		// check trade status
		String tradeStatus = param.getString("trade_status", "");
		if (!"TRADE_FINISHED".equals(tradeStatus) && !"TRADE_SUCCESS".equals(tradeStatus)) { return "fail"; }

		// verify request source
		String notifyId = param.getString("notify_id");
		if (StringUtils.isNotBlank(notifyId)) {
			String result = new HttpUtil().get(VERIFY_NOTIFY_URL + notifyId);
			if (!"true".equalsIgnoreCase(result)) { return "fail"; }
		}

		// verify sign
		String sign = param.getString("sign", "");
		param.remove("sign");
		param.remove("sign_type");

		String sign1 = signMap(param);
		if (!sign.equals(sign1)) { return "fail"; }

		// on trade success
		String orderId = param.getString("out_trade_no", "");
		String transId = param.getString("trade_no", "");

		if (!PaymentConfig.paymentHandler.isOrderDealed(orderId)) {
			PaymentConfig.paymentHandler.onSuccess(orderId, transId);
		}
		return "success";
	}

	@Override
	public String refund(MapJ param) {

		init();

		if (!portConfig.getBoolean("refund")) { return ""; }
		MapJ orderMap = new MapJ();
		orderMap.put("service", "refund_fastpay_by_platform_pwd");
		orderMap.put("partner", portConfig.getString("merId"));
		orderMap.put("_input_charset", portConfig.getString("charset", "UTF-8"));
		orderMap.put("notify_url", portConfig.getString("siteurl") + "pay/refund/notify");
		orderMap.put("seller_user_id", portConfig.getString("merId"));
		orderMap.put("refund_date", Formater.formatDate("yyyy-MM-dd HH:mm:ss", new Date()));
		orderMap.put("batch_no", PaymentConfig.paymentHandler.getRefundNo());

		String[] orderIds = param.getStringArray("orderIds");
		if (orderIds.length == 0) { return "没有退款订单"; }

		orderMap.put("batch_num", orderIds.length);

		StringBuffer buf = new StringBuffer();
		for (String orderId : orderIds) {
			IPaymentOrder order = PaymentConfig.paymentHandler.getOrder(orderId);
			if (order == null || StringUtils.isBlank(order.getTransId())) {
				continue;
			}
			buf.append("#" + order.getTransId() + "^" + order.getAmount() + "^退款");
		}
		if (buf.length() == 0) { return "没有退款订单"; }

		orderMap.put("detail_data", buf.substring(1));

		orderMap.put("sign", signMap(orderMap));
		orderMap.put("sign_type", "MD5");

		String result = buildPage(orderMap);

		return result;
	}

	@Override
	public String notifyRefund(MapJ param) {

		init();

		String batchNo = param.getString("batch_no");
		int successNum = param.getInt("success_num");
		String[] resultDetails = param.getStringArray("result_details", "#");
		String notifyId = param.getString("notify_id");

		if (successNum <= 0 || resultDetails.length == 0) { return "success"; }

		if (StringUtils.isNotBlank(notifyId)) {
			String result = new HttpUtil().get(VERIFY_NOTIFY_URL + notifyId);
			if (!"true".equalsIgnoreCase(result)) { return "fail"; }
		}

		for (String order : resultDetails) {
			if (order.indexOf("SUCCESS") < 0) {
				continue;
			}

			PaymentConfig.paymentHandler.onRefundSuccess(order, batchNo);
		}

		return "success";
	}

	private String signMap(MapJ orderParam) {

		MapJ param = new MapJ();

		for (String key : orderParam.keySet()) {
			if (StringUtils.isBlank(orderParam.getString(key))) {
				continue;
			}
			param.put(key, orderParam.get(key));
		}

		List<String> keys = new ArrayList<String>(param.keySet());

		Collections.sort(keys);

		StringBuffer buf = new StringBuffer();
		for (String key : keys) {
			buf.append("&" + key + "=" + param.getString(key));
		}

		String linkString = buf.substring(1);

		try {
			return DigestUtils.md5Hex((linkString + portConfig.getString("key")).getBytes(portConfig.getString("charset", "UTF-8")));
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();

		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=" + portConfig.getString("charset", "UTF-8") + "'></head><body>");

		buf.append("<form action='" + portConfig.getString("orderURL") + "?_input_charset=" + portConfig.getString("charset", "UTF-8") + "' method='POST' target='_blank'>");
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

}
