package org.jboss.weld.environment.osgi.tests.karaf;

import org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;

import java.io.File;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.replaceConfigurationFile;
import static org.ops4j.pax.exam.CoreOptions.maven;

public class WeldOSGIContainerSupport {

    public static Option[] getDefaultKarafOptions() {
        Option[] options =
                // Set the karaf environment with some customer configuration
                new Option[]{
                        karafDistributionConfiguration()
                                .frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("tar.gz").version("2.3.0"))
                                .karafVersion("2.3.0")
                                .name("Apache Karaf")
                                .useDeployFolder(false).unpackDirectory(new File("target/paxexam/unpack/")),

                        KarafDistributionOption.keepRuntimeFolder(),
                        logLevel(LogLevelOption.LogLevel.INFO),

                        // override the config.properties (to fix pax-exam bug)
                        replaceConfigurationFile("etc/config.properties", new File("src/test/resources/org/jboss/weld/environment/osgi/tests/karaf/config.properties"))
                        //replaceConfigurationFile("etc/custom.properties", new File("src/test/resources/org/jboss/weld/environment/osgi/tests/karaf/custom.properties"))
                };

        return options;

    }

}
