package lion.payment.port.pre.chinapay;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lion.dev.io.FileUtil;
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

import chinapay.PrivateKey;
import chinapay.SecureLink;

/**
 * @author carryon
 * @time 2013 2013-3-21 下午08:14:16
 * @mail hl_0074@sina.com
 * @desc "银联在线" 支付接口
 */

// FIXME 无测试账号
@PaymentPort("chinapay")
public class ChinapayPort implements IPaymentPort {

	private static final String PORT_NAME = "chinapay";
	private static final String PORT_URL = "https://payment.chinapay.com/pay/TransGet";

	private Map<String, SecureLink> secureLinks = new HashMap<>();;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		String merId = handler.getMerId(order.getPortName());
		MapJ param = new MapJ();
		param.put("MerId", merId);
		param.put("OrdId", order.getId());
		param.put("TransAmt", StringUtils.leftPad((order.getAmount() * 100) + "", 12, "0"));
		param.put("CuryId", "156");// 人民币
		param.put("TransDate", Formater.formatDate("yyyyMMdd", new Date()));
		param.put("TransType", "0001");// 消费交易
		param.put("Version", "20070129");

		String serverurl = config.getString("framework.payment.serverurl", "");
		param.put("BgRetUrl", StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify");

		String checkValue = getSE(merId, handler.getKey(PORT_NAME)).signOrder(param.getString("MerId"), param.getString("OrdId"), param.getString("TransAmt"),
				param.getString("CuryId"), param.getString("TransDate"), param.getString("TransType"));

		param.put("ChkValue", checkValue);

		String result = buildPage(param);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		String transDate = param.getString("transdate");
		String merId = param.getString("merid");
		String orderId = param.getString("orderno");
		String transType = param.getString("transtype");
		String transAmt = param.getString("amount");
		String curyId = param.getString("currencycode");
		String orderStatus = param.getString("status");
		String chkValue = param.getString("checkvalue");

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }
		IPaymentOrder order = handler.getOrder(orderId);
		if (order == null) { throw new WebException("Order Not Found"); }

		boolean flag = getSE(merId, handler.getKey(PORT_NAME)).verifyTransResponse(merId, orderId, transAmt, curyId, transDate, transType, orderStatus, chkValue);
		if (flag && !handler.isOrderPaied(orderId)) {
			handler.onOrderPaied(orderId, "");
		}

		return null;
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'></head><body>");

		buf.append("<form action='" + PORT_URL + "' method='POST'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");

		return buf.toString();
	}

	private SecureLink getSE(String merId, String merKey) {

		SecureLink se = secureLinks.get(merId + merKey);
		if (se == null) {
			PrivateKey privateKey = new PrivateKey();
			privateKey.buildKey(merId, 0, FileUtil.toUnixPath(ChinapayPort.class.getClassLoader().getResource("MerPrk.key").getPath() + merKey));
			se = new SecureLink(privateKey);
			secureLinks.put(merId + merKey, se);
		}
		return se;
	}

	@Override
	public String getOrderId(MapJ param) {

		return param.getString("orderno");
	}
}
