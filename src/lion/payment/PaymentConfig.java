package lion.payment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lion.dev.lang.Init;
import lion.dev.lang.Lang;
import lion.dev.lang.MapJ;
import lion.framework.core.anno.Initializer;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * @author carryon
 * @time 2013 2013-3-6 下午06:00:46
 * @mail hl_0074@sina.com
 * @desc 支付类
 */
@Initializer
public class PaymentConfig implements Init {

	public static IPaymentHandler paymentHandler;

	public static String DEFAULT_PAYMENT_PORT = "alipay";
	public static String DEFAULT_NOTIFY_URL = "";
	public static String SITE_URL = "";

	private static final MapJ paymentConfig = new MapJ();

	private static Map<String, IPaymentPort> paymentPorts = new HashMap<String, IPaymentPort>();

	@Override
	@SuppressWarnings("unchecked")
	public void init() {

		SAXReader reader = new SAXReader();
		Document doc = null;
		try {
			doc = reader.read(PaymentConfig.class.getResourceAsStream("/payment.xml"));
		} catch (Exception e) {
			return;
		}

		List<Element> ports = doc.selectNodes("/payment/port");

		for (Element node : ports) {
			MapJ paymentPort = new MapJ();
			String name = node.attributeValue("name");
			if ("true".equalsIgnoreCase(node.attributeValue("default"))) {
				DEFAULT_PAYMENT_PORT = name;
			}

			Iterator<Element> subIter = node.elementIterator();
			while (subIter.hasNext()) {
				Element sub = subIter.next();
				paymentPort.put(sub.getName(), sub.getTextTrim());
			}

			paymentConfig.put(name, paymentPort);
		}

		Element common = (Element) doc.selectSingleNode("/payment/siteurl");
		if (common != null) {

			String siteurl = common.getTextTrim();
			if (!siteurl.endsWith("/")) {
				siteurl += "/";
			}
			SITE_URL = siteurl;
		}

		common = (Element) doc.selectSingleNode("/payment/notifyURL");
		if (common != null) {

			String notifyURL = common.getTextTrim();
			if (notifyURL.startsWith("/")) {
				notifyURL = SITE_URL + notifyURL.substring(1);
			}

			DEFAULT_NOTIFY_URL = notifyURL;
		}
	}

	public static IPaymentPort getPort(String portName) {

		return paymentPorts.get(Lang.nvl(portName, DEFAULT_PAYMENT_PORT));
	}

	public static void registPort(String name, IPaymentPort port) {

		paymentPorts.put(name, port);
	}

	public static MapJ getPortConfig(String portName) {

		return paymentConfig.getE(Lang.nvl(portName, DEFAULT_PAYMENT_PORT));
	}

	@Override
	public int order() {

		return 0;
	}

}
