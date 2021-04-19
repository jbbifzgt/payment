package lion.payment.port.tenpay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lion.dev.lang.MapJ;
import lion.dev.web.Validator;
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

	private static final String PORT_NAME = "tenpay";
	private static final String PORT_URL = "https://gw.tenpay.com/gateway/pay.htm";
	private static final String NOTIFY_ID_URL = "https://gw.tenpay.com/gateway/simpleverifynotifyid.xml?partner=%s&notify_id=%s&sign=%s";

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		MapJ orderMap = new MapJ();
		orderMap.put("partner", handler.getMerId(order.getPortName()));
		orderMap.put("out_trade_no", order.getId());
		orderMap.put("total_fee", Validator.toInt(String.valueOf(order.getAmount() * 100), 0));
		orderMap.put("body", order.getOrderName());

		if (StringUtils.isNotBlank(paymentChannel)) {
			orderMap.put("bank_type", "DEFAULT");
		}

		orderMap.put("fee_type", "1"); // 币种
		orderMap.put("subject", order.getOrderName());

		orderMap.put("spbill_create_ip", "127.0.0.1");// 客户端IP，不能为空
		orderMap.put("sign_type", "MD5");
		orderMap.put("service_version", "1.0");
		orderMap.put("input_charset", "UTF-8");
		orderMap.put("sign_key_index", "1");

		orderMap.put("trade_mode", "1");// 交易模式（1.即时到帐模式，2.中介担保模式，3.后台选择（卖家进入支付中心列表选择））
		orderMap.put("trans_type", "1"); // 交易类型

		String serverurl = config.getString("framework.payment.serverurl", "");
		orderMap.put("notify_url", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify");

		orderMap.put("sign", signMap(orderMap, handler.getKey(PORT_NAME)));

		String result = buildPage(orderMap);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		String notifyId = param.getString("notify_id");
		String sign = param.getString("sign");
		String transId = param.getString("transaction_id");
		String orderId = param.getString("out_trade_no");
		String tradeState = param.getString("trade_state");

		if (!StringUtils.equals("0", tradeState)) { return "success"; } // 支付失败不处理

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		IPaymentOrder order = handler.getOrder(orderId);
		if (order == null) { throw new WebException("Order Not Found"); }

		String merKey = handler.getKey(PORT_NAME);
		String merId = handler.getMerId(PORT_NAME);

		String checkSign = signMap(param, merKey);
		// 验证签名
		if (!StringUtils.equalsIgnoreCase(sign, checkSign)) { return "认证签名失败"; }

		// 核实notifyid
		MapJ map = new MapJ();
		map.put("notify_id", notifyId);
		map.put("partner", merId);

		sign = signMap(map, merKey);
		String url = String.format(NOTIFY_ID_URL, merId, notifyId, sign);
		String result = new HttpUtil().get(url);
		MapJ resMap = parseTenpayResponse(result);
		sign = resMap.getString("sign");
		checkSign = signMap(resMap, merKey);

		// 验证返回签名
		if (!StringUtils.equalsIgnoreCase(checkSign, sign)) { return "认证签名失败"; }

		// 验证 交易状态
		String retCode = resMap.getString("retcode");
		if ("0".equals(retCode) && !handler.isOrderPaied(orderId)) {
			handler.onOrderPaied(orderId, transId);
		}
		return "success";
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
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body>");

		buf.append("<form action='" + PORT_URL + "' method='POST'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");

		return buf.toString();
	}

	private String signMap(MapJ paramMap, String merKey) {

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

		String linkString = buf.toString() + "key=" + merKey;

		return DigestUtils.md5Hex(linkString);
	}

	@Override
	public String getOrderId(MapJ param) {

		return param.getString("out_trade_no");
	}

}
