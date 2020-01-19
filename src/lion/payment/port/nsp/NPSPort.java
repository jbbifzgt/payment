package lion.payment.port.nsp;

import java.util.Date;

import lion.dev.lang.MapJ;
import lion.dev.text.Formater;
import lion.payment.IPaymentOrder;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;
import lion.payment.anno.PaymentPort;

import org.apache.commons.lang.StringUtils;

import com.eitop.platform.tools.encrypt.MD5Digest;
import com.eitop.platform.tools.encrypt.xStrEncrypt;

/**
 * @author lion
 * @time 2013 2013-4-17 下午05:45:06
 * @mail
 * @desc Network Payment System 深圳全动科技有限公司的支付系统
 */
@PaymentPort("nps")
public class NPSPort implements IPaymentPort {

	private static String PORT_NAME = "nps";
	private MapJ portConfig;

	@Override
	public String buildRequestPage(IPaymentOrder order, String paymentChannel) {

		init();

		String merId = portConfig.getString("merId");
		String key = portConfig.getString("key");

		// 订单信息 m_id|m_orderid|m_oamount|m_ocurrency|m_url|m_language
		String m_info = merId + "|" + order.getId() + "|" + order.getAmount() + "|" + 1 + "|" + portConfig.getString("notifyURL") + "|null";

		// 付款人信息 s_name|s_addr|s_postcode|s_tel|s_eml|r_name
		String s_info = "null|null|null|null|null|null";

		// 收货人信息 r_addr|r_postcode|r_tel|r_eml|m_ocomment|m_status|modate
		String r_info = "null|null|null|null|null|0|" + Formater.formatDate("yyyy-MM-dd HH:mm:ss", new Date());

		String orderInfo = xStrEncrypt.StrEncrypt(m_info + "|" + s_info + "|" + r_info, key);
		String sign = MD5Digest.encrypt(orderInfo + key);

		String result = buildPage(portConfig.getString("merId"), orderInfo, sign);

		return result;
	}

	@Override
	public String notifyPaymentResult(MapJ param) {

		init();

		String key = portConfig.getString("key");

		String sign = param.getString("Digest");
		String orderInfo = param.getString("OrderMessage");

		String digest = MD5Digest.encrypt(orderInfo + key);
		if (!StringUtils.equalsIgnoreCase(digest, sign)) { return "签名验证失败"; }

		orderInfo = xStrEncrypt.StrDecrypt(orderInfo, key);

		String[] array = orderInfo.split("\\|");
		if (array.length != 19) { return "支付信息不正确"; }

		String orderId = array[1];
		String Status = array[18];
		if ("2".equals(Status)) {

			if (!PaymentConfig.paymentHandler.isOrderDealed(orderId)) {
				PaymentConfig.paymentHandler.onSuccess(orderId, "");
			}

			return "支付成功";
		}

		return "支付失败";
	}

	private void init() {

		if (portConfig == null) {
			portConfig = PaymentConfig.getPortConfig(PORT_NAME);
		}
	}

	private String buildPage(String merId, String orderInfo, String sign) {

		StringBuffer buf = new StringBuffer();

		buf.append("<html>");
		buf.append("<head>");
		buf.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=gb2312\">");
		buf.append("</head>");
		buf.append("<body onload=\"document.frm001.submit();\">");
		buf.append("<form name=\"frm001\" method=\"post\" action=\"https://payment.nps.cn/ReceiveMerchantAction.do\">");
		buf.append("	<input type=\"hidden\" name=\"OrderMessage\" value=\"" + orderInfo + "\">");
		buf.append("	<input type=\"hidden\" name=\"digest\" value=\"" + sign + "\">");
		buf.append("	<input type=\"hidden\" name=\"M_ID\" value=\"" + merId + "\">");
		buf.append("	<input type=\"hidden\" name=\"s\" value=\"submit\">");
		buf.append("</form>");
		buf.append("</body>");
		buf.append("</html>");

		return buf.toString();
	}

	@Override
	public String refund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[nps]在线退款");
	}

	@Override
	public String notifyRefund(MapJ param) {

		throw new UnsupportedOperationException("不支持当前网关[nps]在线退款");
	}

}
