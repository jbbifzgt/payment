package lion.payment.anno;

import java.lang.annotation.Annotation;

import lion.dev.lang.MapJ;
import lion.framework.core.anno.AnnoProcessor;
import lion.framework.core.anno.AnnotationProcessorManager;
import lion.framework.core.anno.IAnnotationProcessor;
import lion.framework.core.bean.BeanFactory;
import lion.payment.IPaymentPort;

@AnnoProcessor(PaymentPort.class)
public class PaymentPortAnnoProcessor implements IAnnotationProcessor {

	@Override
	public void processe(MapJ context, Annotation anno) throws Exception {

		Class<IPaymentPort> clazz = context.getE(AnnotationProcessorManager.ANNO_CONTEXT_TARGET);
		if (!IPaymentPort.class.isAssignableFrom(clazz)) { return; }
		PaymentPort port = (PaymentPort) anno;

		PaymentPorts.regist(port.value(), BeanFactory.create(clazz));
	}

}
