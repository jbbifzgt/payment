package lion.payment;


/**
 * @author carryon
 * @time 2013 2013-3-6 上午12:56:25
 * @mail hl_0074@sina.com
 * @desc 付款单
 */
public interface IPaymentOrder {

	String getId();

	double getAmount();

	String getOrderName();

	String getPortName();

}
