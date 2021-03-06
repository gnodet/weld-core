package org.jboss.weld.environment.osgi.tests.karaf;

import org.jboss.weld.environment.osgi.spi.CDIContainer;
import org.jboss.weld.environment.osgi.spi.CDIContainerFactory;
import org.jboss.weld.environment.osgi.tests.util.Environment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.*;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import java.util.Collection;

import static org.jboss.weld.environment.osgi.tests.karaf.WeldOSGIContainerSupport.getDefaultKarafOptions;
import static org.jboss.weld.environment.osgi.tests.util.Environment.toMavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ContainerTest {

    @Configuration
    public static Option[] configure() {
        Option[] options = combine(

                getDefaultKarafOptions(),

                Environment.toCDIKarafEnvironment(
                        toMavenBundle("org.jboss.weld.osgi.tests", "weld-osgi-bundle1"),
                        toMavenBundle("org.jboss.weld.osgi.tests", "weld-osgi-bundle2"),
                        toMavenBundle("org.jboss.weld.osgi.tests", "weld-osgi-bundle3")
                )

        );

        return options;
    }

    @Inject
    private BundleContext context;

    @Test
    public void launchTest() throws InterruptedException, BundleException, InvalidSyntaxException {

        Environment.waitForEnvironment(context);

        Bundle bundle1 = null, bundle2 = null, bundle3 = null;
        for (Bundle b : context.getBundles()) {
            Assert.assertEquals("Bundle" + b.getSymbolicName() + " is not ACTIVE", Bundle.ACTIVE, b.getState());
            if (b.getSymbolicName().equals("org.jboss.weld.osgi.tests.weld-osgi-bundle1")) {
                bundle1 = b;
            } else if (b.getSymbolicName().equals("org.jboss.weld.osgi.tests.weld-osgi-bundle2")) {
                bundle2 = b;
            } else if (b.getSymbolicName().equals("org.jboss.weld.osgi.tests.weld-osgi-bundle3")) {
                bundle3 = b;
            }
        }
        Assert.assertNotNull("The bundle1 was not retrieved", bundle1);
        Assert.assertNotNull("The bundle2 was not retrieved", bundle2);
        Assert.assertNotNull("The bundle3 was not retrieved", bundle3);

        ServiceReference[] factoryReferences = context.getServiceReferences(CDIContainerFactory.class.getName(), null);
        Assert.assertNotNull("The container factory service reference array was null", factoryReferences);
        Assert.assertTrue("There was no container factory service available", factoryReferences.length > 0);
        CDIContainerFactory factory = (CDIContainerFactory) context.getService(factoryReferences[0]);
        Assert.assertNotNull("The container factory service was null", factory);
        Collection<CDIContainer> containers = factory.containers();
        Assert.assertEquals("The container collection had the wrong number of containers", 2, containers.size());

        CDIContainer container1 = factory.container(bundle1);
        CDIContainer container2 = factory.container(bundle2);
        CDIContainer container3 = factory.container(bundle3);
        Assert.assertNotNull("The container for bundle1 was null", container1);
        Assert.assertNotNull("The container for bundle2 was null", container2);
        Assert.assertNull("The container for bundle3 was not null", container3);
        Assert.assertTrue("The container for bundle1 was not started", container1.isStarted());
        Assert.assertTrue("The container for bundle2 was not started", container2.isStarted());

        Collection<ServiceRegistration> registrations1 = container1.getRegistrations();
        Collection<ServiceRegistration> registrations2 = container2.getRegistrations();
        Assert.assertEquals("The registration collection 1 had the wrong number of registrations", (3 + 9 + 3), registrations1.size());
        Assert.assertEquals("The registration collection 2 had the wrong number of registrations", (3 + 2), registrations2.size());

        Collection<String> beanClasses1 = container1.getBeanClasses();
        Collection<String> beanClasses2 = container2.getBeanClasses();
        Assert.assertNotNull("The bean class collection 1 was null", beanClasses1);
        Assert.assertNotNull("The bean class collection 2 was null", beanClasses2);

        BeanManager beanManager1 = container1.getBeanManager();
        BeanManager beanManager2 = container2.getBeanManager();
        ServiceReference[] beanManagerServices = context.getServiceReferences(BeanManager.class.getName(), null);
        Assert.assertNotNull("The event bean manager reference array was null", beanManagerServices);
        Assert.assertEquals("The number of bean manager services was wrong", 2, beanManagerServices.length);
        Assert.assertNotNull("The bean manager 1 was null", beanManager1);
        Assert.assertNotNull("The bean manager 2 was null", beanManager2);

        Event event1 = container1.getEvent();
        Event event2 = container2.getEvent();
        ServiceReference[] eventServices = context.getServiceReferences(Event.class.getName(), null);
        Assert.assertNotNull("The event service reference array was null", eventServices);
        Assert.assertEquals("The number of event services was wrong", 2, eventServices.length);
        Assert.assertNotNull("The event 1 was null", event1);
        Assert.assertNotNull("The event 2 was null", event2);

        Instance instance1 = container1.getInstance();
        Instance instance2 = container2.getInstance();
        ServiceReference[] instanceServices = context.getServiceReferences(Instance.class.getName(), null);
        Assert.assertNotNull("The instance service reference array was null", instanceServices);
        Assert.assertEquals("The number of instance services was wrong", 2, instanceServices.length);
        Assert.assertNotNull("The instance 1 was null", instance1);
        Assert.assertNotNull("The instance 2 was null", instance2);

        Assert.assertTrue("The container was not been shutdown", container1.shutdown());
        Assert.assertFalse("The container was still started", container1.isStarted());
        Assert.assertEquals("The container collection had the wrong number of containers", 2, containers.size());
    }
}
