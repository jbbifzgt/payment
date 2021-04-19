package lion.payment.anno;

import java.lang.annotation.Annotation;

import lion.dev.lang.MapJ;
import lion.framework.core.anno.AnnoProcessor;
import lion.framework.core.anno.AnnotationProcessorManager;
import lion.framework.core.anno.IAnnotationProcessor;
import lion.framework.core.bean.BeanFactory;
import lion.payment.IPaymentHandler;

@AnnoProcessor(PaymentHandler.class)
public class PaymentHandlerAnnoProcessor implements IAnnotationProcessor {

	@Override
	public void processe(MapJ context, Annotation anno) throws Exception {

		Class<?> clazz = (Class<?>) context.get(AnnotationProcessorManager.ANNO_CONTEXT_CLASS);
		if (!IPaymentHandler.class.isAssignableFrom(clazz)) { return; }

		PaymentHandler paymentHandler = (PaymentHandler) anno;

		PaymentHandlers.regist(paymentHandler.value(), (IPaymentHandler) BeanFactory.create(clazz));

	}
}
