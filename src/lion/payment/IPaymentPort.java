package lion.payment;

import lion.dev.lang.MapJ;

/**
 * @author carryon
 * @time 2013 2013-3-6 上午12:59:42
 * @mail hl_0074@sina.com
 * @desc 支付接口公司
 */
public interface IPaymentPort {

	String buildRequestPage(IPaymentOrder order, String channel);

	String notifyPaymentResult(MapJ param);

	String getOrderId(MapJ param);
}
