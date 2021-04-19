package lion.payment.port.pre.yeepay;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
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
 * @time 2013 2013-3-6 上午01:07:35
 * @mail hl_0074@sina.com
 * @desc 易宝支付接口
 */
// 易宝接口维护
@PaymentPort("yeepay")
public class YeepayPort implements IPaymentPort {

	private static final String PORT_NAME = "yeepay";
	private static final String PORT_URL = "https://www.yeepay.com/app-merchant-proxy/node";

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		MapJ param = new MapJ();

		String merId = handler.getMerId(order.getPortName());
		String merKey = handler.getKey(PORT_NAME);

		param.put("p0_Cmd", "Buy");
		param.put("p1_MerId", merId);
		param.put("p2_Order", order.getId());
		param.put("p3_Amt", order.getAmount());
		param.put("p4_Cur", "CNY");
		param.put("p5_Pid", order.getOrderName());
		param.put("p6_Pcat", "");
		param.put("p7_Pdesc", "");

		String serverurl = config.getString("framework.payment.serverurl", "");
		String notifyUrl = StringUtils.stripEnd(serverurl, "/") + "/payment/pay/notify";
		param.put("p8_Url", notifyUrl);

		param.put("p9_SAF", "0");
		param.put("pa_MP", "");
		param.put("pd_FrpId", Lang.nvl(paymentChannel, ""));
		param.put("pr_NeedResponse", "1");

		param.put("hmac", signOrder(order, paymentChannel, notifyUrl, merId, merKey));

		String result = buildPage(param);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		Config config = ConfigManager.getConfig("framework");
		IPaymentHandler handler = PaymentHandlers.get(config.getString("framework.payment.handler"));
		if (handler == null) { return null; }

		String hmac = param.getString("hmac", "");
		String r1_Code = param.getString("r1_Code", "");
		String r9_BType = param.getString("r9_BType", "");
		String r6_Order = param.getString("r6_Order", "");
		String r2_TrxId = param.getString("r2_TrxId", "");

		IPaymentOrder order = handler.getOrder(r6_Order);
		if (order == null) { throw new WebException("Order Not Found"); }

		String merId = handler.getMerId(order.getPortName());
		String merKey = handler.getKey(PORT_NAME);

		StringBuffer sbu = new StringBuffer();
		sbu.append(merId);
		sbu.append(param.getString("r0_Cmd", ""));
		sbu.append(r1_Code);
		sbu.append(r2_TrxId);
		sbu.append(param.getString("r3_Amt", ""));
		sbu.append(param.getString("r4_Cur", ""));
		sbu.append(param.getString("r5_Pid", ""));
		sbu.append(r6_Order);
		sbu.append(param.getString("r7_Uid", ""));
		sbu.append(param.getString("r8_MP", ""));
		sbu.append(r9_BType);

		if (!"1".equals(r1_Code)) { return ""; }

		String digest = YeepayDigestUtil.hmacSign(sbu.toString(), merKey);
		if (!StringUtils.equals(hmac, digest)) { return ""; }

		if ("2".equals(r9_BType) && !handler.isOrderPaied(r6_Order)) {

			handler.onOrderPaied(r6_Order, r2_TrxId);

			return "SUCCESS";
		}

		return "";
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=GBK'></head><body>");

		buf.append("<form action='" + PORT_URL + "' method='POST'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");
		return buf.toString();
	}

	public String signOrder(IPaymentOrder order, String paymentChannel, String notifyURL, String merId, String merKey) {

		StringBuilder sbu = new StringBuilder();
		sbu.append("Buy");
		sbu.append(merId);
		sbu.append(order.getId());
		sbu.append(order.getAmount());
		sbu.append("CNY");
		sbu.append(Lang.nvl(order.getOrderName(), ""));
		sbu.append(notifyURL);//
		sbu.append("0");// 是否需要填写送货地址
		sbu.append("");// 自定义数据
		sbu.append(Lang.nvl(paymentChannel, ""));// 支付通道编码
		sbu.append("1");// 应答机制

		return YeepayDigestUtil.hmacSign(sbu.toString(), merKey);
	}

	@Override
	public String getOrderId(MapJ param) {

		return param.getString("r6_Order", "");
	}
}
