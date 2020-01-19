package lion.payment.port.yeepay;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.payment.HttpUtil;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
import lion.payment.anno.PaymentPort;

import org.apache.commons.lang.StringUtils;

/**
 * @author carryon
 * @time 2013 2013-3-6 上午01:07:35
 * @mail hl_0074@sina.com
 * @desc 易宝支付接口
 */
@PaymentPort("yeepay")
public class YeepayPort implements IPaymentPort {

	private static final String PORT_NAME = "yeepay";

	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		MapJ param = new MapJ();

		param.put("p0_Cmd", "Buy");
		param.put("p1_MerId", portConfig.getString("merId"));
		param.put("p2_Order", order.getId());
		param.put("p3_Amt", order.getAmount());
		param.put("p4_Cur", "CNY");
		param.put("p5_Pid", Lang.nvl(order.getProductId(), ""));
		param.put("p6_Pcat", Lang.nvl(order.getOrderCategory(), ""));
		param.put("p7_Pdesc", Lang.nvl(order.getOrderDescribtion(), ""));
		param.put("p8_Url", Lang.nvl(portConfig.getString("notifyURL"), PaymentConfig.DEFAULT_NOTIFY_URL));
		param.put("p9_SAF", "0");
		param.put("pa_MP", "");
		param.put("pd_FrpId", Lang.nvl(paymentChannel, ""));
		param.put("pr_NeedResponse", "1");
		param.put("hmac", signOrder(order, paymentChannel));

		String result = buildPage(param);

		return result;
	}

	private void init() {

		if (portConfig == null) {
			portConfig = PaymentConfig.getPortConfig(PORT_NAME);
		}
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();

		String hmac = param.getString("hmac", "");
		String r1_Code = param.getString("r1_Code", "");
		String r9_BType = param.getString("r9_BType", "");
		String r6_Order = param.getString("r6_Order", "");
		String r2_TrxId = param.getString("r2_TrxId", "");

		StringBuffer sbu = new StringBuffer();
		sbu.append(portConfig.getString("merId", ""));
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

		String digest = YeepayDigestUtil.hmacSign(sbu.toString(), portConfig.getString("key"));
		if (!StringUtils.equals(hmac, digest)) { return ""; }

		if ("2".equals(r9_BType) && !PaymentConfig.paymentHandler.isOrderDealed(r6_Order)) {

			PaymentConfig.paymentHandler.onSuccess(r6_Order, r2_TrxId);

			return "SUCCESS";
		}

		return "";
	}

	private String buildPage(MapJ orderParam) {

		StringBuffer buf = new StringBuffer();
		buf.append("<html><head><meta http-equiv='Content-Type' content='text/html; charset=" + portConfig.getString("charset") + "'></head><body>");

		buf.append("<form action='" + portConfig.getString("orderURL") + "' method='POST' target='_blank'>");
		for (String key : orderParam.keySet()) {
			buf.append("<input type=\"hidden\" name=\"" + key + "\"   value=\"" + orderParam.getString(key) + "\">");
		}
		buf.append("</form>");

		buf.append("<script>document.forms[0].submit();</script></body></html>");
		return buf.toString();
	}

	public String signOrder(IPaymentOrder order, String paymentChannel) {

		StringBuilder sbu = new StringBuilder();
		sbu.append("Buy");
		sbu.append(portConfig.getString("merId"));
		sbu.append(order.getId());
		sbu.append(order.getAmount());
		sbu.append("CNY");
		sbu.append(Lang.nvl(order.getOrderName(), ""));
		sbu.append(Lang.nvl(order.getOrderCategory(), ""));
		sbu.append(Lang.nvl(order.getOrderDescribtion(), ""));
		sbu.append(Lang.nvl(portConfig.getString("notifyURL"), PaymentConfig.DEFAULT_NOTIFY_URL));// payment
																									// response
																									// url
		sbu.append("0");// 是否需要填写送货地址
		sbu.append("");// 自定义数据
		sbu.append(Lang.nvl(paymentChannel, ""));// 支付通道编码
		sbu.append("1");// 应答机制

		return YeepayDigestUtil.hmacSign(sbu.toString(), portConfig.getString("key"));
	}

	@Override
	public String refund(MapJ param) {

		init();

		if (!portConfig.getBoolean("refund")) { return ""; }

		String[] orderIds = param.getStringArray("orderIds");
		if (orderIds.length == 0) { return ""; }

		for (String orderId : orderIds) {
			if (!PaymentConfig.paymentHandler.isOrderRefund(orderId)) {
				refund(orderId);
			}
		}

		return "success";
	}

	private void refund(String orderId) {

		MapJ orderMap = new MapJ();

		IPaymentOrder order = PaymentConfig.paymentHandler.getOrder(orderId);
		if (order == null) { return; }

		orderMap.put("p0_Cmd", "RefundOrd");
		orderMap.put("p1_MerId", portConfig.getString("merId"));
		orderMap.put("pb_TrxId", order.getTransId());
		orderMap.put("p3_Amt", order.getAmount());
		orderMap.put("p4_Cur", "CNY");
		orderMap.put("p5_Desc", "");

		String data = "RefundOrd" + portConfig.getString("merId") + order.getTransId() + order.getAmount() + "CNY";
		orderMap.put("hmac", YeepayDigestUtil.hmacSign(data, portConfig.getString("key")));

		String result = new HttpUtil().post(portConfig.getString("orderURL"), orderMap);

		if (result.indexOf("r1_Code=1") > 0) {

			Pattern p = Pattern.compile("r2_TrxId=(\\w+)");
			Matcher m = p.matcher(result);
			if (m.find()) {
				PaymentConfig.paymentHandler.onRefundSuccess(orderId, m.group(1));
			}
		}
	}

	@Override
	public String notifyRefund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[yeepay]在线退款");
	}
}
