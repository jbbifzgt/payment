package lion.payment.anno;

import java.lang.annotation.Annotation;

import lion.dev.lang.MapJ;
import lion.framework.core.anno.AnnoProcessor;
import lion.framework.core.anno.AnnotationProcessorManager;
import lion.framework.core.anno.IAnnotationProcessor;
import lion.payment.IPaymentHandler;
import lion.payment.PaymentConfig;

@AnnoProcessor(PaymentHandler.class)
public class PaymentHandlerAnnoProcessor implements IAnnotationProcessor {

	@Override
	public void processe(MapJ context, Annotation anno) throws Exception {

		Class<IPaymentHandler> handler = context.getE(AnnotationProcessorManager.ANNO_CONTEXT_CLASS);
		try {
			PaymentConfig.paymentHandler = handler.newInstance();
		} catch (Exception e) {
		}
	}
}
