package lion.payment;

import java.util.Date;

/**
 * @author carryon
 * @time 2013 2013-3-6 上午12:56:25
 * @mail hl_0074@sina.com
 * @desc 付款单
 */
public interface IPaymentOrder {

	String getId();

	double getAmount();

	String getProductId();

	String getOrderName();

	String getOrderCategory();

	String getOrderDescribtion();

	String getTransId();

	Date getTransDate();

}
