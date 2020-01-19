package lion.payment.port.tenpay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.dev.web.Validator;
import lion.payment.HttpUtil;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
import lion.payment.anno.PaymentPort;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

/**
 * @author carryon
 * @time 2013 2013-3-21 下午10:45:37
 * @mail hl_0074@sina.com
 * @desc "财富通" 支付接口
 */
@PaymentPort("tenpay")
public class TenpayPort implements IPaymentPort {

	public static final String PORT_NAME = "tenpay";
	private MapJ portConfig;
	private Pattern retCode0 = Pattern.compile("<retcode>0+</retcode>");

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		MapJ orderMap = new MapJ();

		orderMap.put("partner", portConfig.getString("merId"));
		orderMap.put("out_trade_no", order.getId());
		orderMap.put("total_fee", Validator.toInt(String.valueOf(order.getAmount() * 100), 0));
		orderMap.put("return_url", PaymentConfig.SITE_URL + "pay/tenpay_return_url.jsp");
		orderMap.put("notify_url", Lang.nvl(portConfig.getString("notifyURL"), PaymentConfig.DEFAULT_NOTIFY_URL));
		orderMap.put("body", order.getOrderName());
		orderMap.put("bank_type", "DEFAULT");
		orderMap.put("fee_type", "1"); // 币种
		orderMap.put("subject", order.getOrderName());
		orderMap.put("spbill_create_ip", "");
		orderMap.put("sign_type", "MD5");
		orderMap.put("service_version", "1.0");
		orderMap.put("input_charset", "UTF-8");
		orderMap.put("sign_key_index", "1");

		orderMap.put("attach", "");
		orderMap.put("product_fee", "");
		orderMap.put("transport_fee", "0");
		orderMap.put("time_start", Formater.formatDate("yyyyMMddHHmmss", new Date()));
		orderMap.put("time_expire", "");
		orderMap.put("buyer_id", "");
		orderMap.put("goods_tag", "");
		orderMap.put("trade_mode", "1");// 交易模式（1.即时到帐模式，2.中介担保模式，3.后台选择（卖家进入支付中心列表选择））
		orderMap.put("transport_desc", "");
		orderMap.put("trans_type", "1"); // 交易类型
		orderMap.put("agentid", "");
		orderMap.put("agent_type", "");
		orderMap.put("seller_id", "");

		orderMap.put("sign", signMap(orderMap));

		String result = buildPage(orderMap);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();

		String notifyId = param.getString("notify_id");
		String sign = param.getString("sign");
		String checkSign = signMap(param);

		// 验证签名
		if (!StringUtils.endsWithIgnoreCase(sign, checkSign)) { return "认证签名失败"; }

		// 核实notifyid
		String url = "https://gw.tenpay.com/gateway/simpleverifynotifyid.xml?partner=" + portConfig.getString("merId") + "&notify_id=" + notifyId;
		String result = new HttpUtil().get(url);
		MapJ resMap = parseTenpayResponse(result);

		sign = resMap.getString("sign");
		checkSign = signMap(resMap);

		// 验证返回签名
		if (!StringUtils.equalsIgnoreCase(checkSign, sign)) { return "认证签名失败"; }

		// 验证 交易状态
		String retCode = resMap.getString("retcode");
		if (!"0".equals(retCode)) { return "fail"; }

		String transId = param.getString("transaction_id");
		String orderId = param.getString("out_trade_no");
		String tradeState = param.getString("trade_state");

		if ("0".equals(tradeState) && !PaymentConfig.paymentHandler.isOrderDealed(orderId)) {
			PaymentConfig.paymentHandler.onSuccess(orderId, transId);

			return "success";
		}

		return "fail";
	}

	private void init() {

		if (portConfig == null) {
			portConfig = PaymentConfig.getPortConfig(PORT_NAME);
		}

	}

	@SuppressWarnings("unchecked")
	private MapJ parseTenpayResponse(String xml) {

		MapJ result = new MapJ();
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			return result;
		}
		List<Node> nodes = doc.selectNodes("/root/*");

		for (Node node : nodes) {
			String text = node.getText();
			if (StringUtils.isBlank(text)) {
				continue;
			}
			result.put(node.getName(), text);
		}

		return result;
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=" + portConfig.getString("charset", "UTF-8") + "'></head><body>");

		buf.append("<form action='" + portConfig.getString("orderURL") + "' method='POST'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");

		return buf.toString();
	}

	private String signMap(MapJ paramMap) {

		paramMap.remove("sign");
		MapJ param = new MapJ();

		for (String key : paramMap.keySet()) {
			if (StringUtils.isBlank(paramMap.getString(key))) {
				continue;
			}
			param.put(key, paramMap.get(key));
		}

		List<String> keys = new ArrayList<String>(param.keySet());

		Collections.sort(keys);

		StringBuffer buf = new StringBuffer();
		for (String key : keys) {
			buf.append(key + "=" + param.getString(key) + "&");
		}

		MapJ portConfig = PaymentConfig.getPortConfig(PORT_NAME);
		String linkString = buf.toString() + "key=" + portConfig.getString("key");

		return DigestUtils.md5Hex(linkString);
	}

	@Override
	public String refund(MapJ param) {

		init();
		if (!portConfig.getBoolean("refund")) { return ""; }
		// TODO 部分参数不清楚，暂不提供退款功能
		throw new UnsupportedOperationException("不支持当前网关[tenpay]在线退款");

		// String[] orderIds = param.getStringArray("orderIds");
		//
		// for (String orderId : orderIds) {
		// if (!PaymentConfig.paymentHandler.isOrderRefund(orderId)) {
		// refund(orderId);
		// }
		// }
		//
		// return "退款操作已完成";
	}

	private void refund(String orderId) {

		IPaymentOrder order = PaymentConfig.paymentHandler.getOrder(orderId);
		if (order == null) { return; }

		String rno = PaymentConfig.paymentHandler.getRefundNo();
		if (rno.length() < 7) {
			rno = StringUtils.leftPad(rno, 7, "0");
		} else {
			rno = StringUtils.substring(rno, 0, 7);
		}
		String merId = portConfig.getString("merId");
		String refundId = "109" + portConfig.getString("merId") + Formater.formatDate("yyyyMMdd", new Date()) + rno;

		String content = "<?xml version=\"1.0\" encoding =\"GB2312\"?><root><op_code>1003</op_code><op_name>b2c_refund</op_name><op_user>" + merId
				+ "</op_user><op_passwd>{oppasswd}</op_passwd><op_time>" + Formater.formatDate("yyyyMMddhhmmssSSS", new Date()) + "</op_time><sp_id>merid</sp_id><trans_id>"
				+ orderId + "</trans_id><refund_id>" + refundId
				+ "</refund_id><client_ip></client_ip><rec_acc></rec_acc><rec_acc_truename></rec_acc_truename><cur_type>1</cur_type><pay_amt>" + order.getAmount() * 100
				+ "</pay_amt><desc>协商退款</desc></root>";

		String abs = Base64.encodeBase64String(content.getBytes());
		abs = DigestUtils.md5Hex(abs);
		abs = DigestUtils.md5Hex(abs + portConfig.getString("key"));

		MapJ orderParam = new MapJ();
		orderParam.put("content", Base64.encodeBase64(content.getBytes()));
		orderParam.put("abstract", abs);

		String result = new HttpUtil().post(portConfig.getString("refundURL"), orderParam);

		Matcher m = retCode0.matcher(result);

		if (m.find()) {
			PaymentConfig.paymentHandler.onRefundSuccess(order.getId(), rno);
		}
	}

	@Override
	public String notifyRefund(MapJ param) {

		throw new UnsupportedOperationException("不支持的方法[tenpay]");
	}

}
