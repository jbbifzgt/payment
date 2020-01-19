package lion.payment.anno;

import java.lang.annotation.Annotation;

import lion.dev.lang.MapJ;
import lion.framework.core.anno.AnnoProcessor;
import lion.framework.core.anno.AnnotationProcessorManager;
import lion.framework.core.anno.IAnnotationProcessor;
import lion.payment.IPaymentPort;
import lion.payment.PaymentConfig;

@AnnoProcessor(PaymentPort.class)
public class PaymentPortAnnoProcessor implements IAnnotationProcessor {

	@Override
	public void processe(MapJ context, Annotation anno) throws Exception {

		PaymentPort port = (PaymentPort) anno;

		Class<IPaymentPort> klass = context.getE(AnnotationProcessorManager.ANNO_CONTEXT_TARGET);

		try {
			PaymentConfig.registPort(port.value(), klass.newInstance());
		} catch (Exception e) {
		}
	}

}
