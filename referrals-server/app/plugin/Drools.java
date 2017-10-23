package plugin;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.drools.compiler.kie.builder.impl.KieServicesImpl;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import play.Environment;
import play.inject.ApplicationLifecycle;

/**
 * @author srivassumit
 *
 */
@Singleton
public class Drools {
	public final KieSession kieSession;

	@Inject
	public Drools(ApplicationLifecycle lifecycle, Environment environment) {
		KieServices kieServices = new KieServicesImpl();
		KieContainer kc = kieServices.getKieClasspathContainer(environment.classLoader());
		kieSession = kc.newKieSession("ksession-rules");

		// uncomment these to enable debugging
		// kieSession.addEventListener(new DebugAgendaEventListener());
		// kieSession.addEventListener(new DebugRuleRuntimeEventListener());

		lifecycle.addStopHook(() -> {
			kieSession.destroy();
			return CompletableFuture.completedFuture(null);
		});
		// lifecycle.addStopHook(() -> {
		// kieSession.destroy();
		// return F.Promise.pure(null);//deprecated 
		// use CompletableFuture.completedFuture(null) instead
		// });
	}
}
