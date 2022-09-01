package org.virtuslab.ideprobe;

import org.junit.Test;
import org.virtuslab.ideprobe.protocol.Module;
import org.virtuslab.ideprobe.protocol.ProjectRef;
import scala.concurrent.duration.Duration;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.virtuslab.ideprobe.WaitLogic.emptyBackgroundTasks;
import static org.virtuslab.ideprobe.wait.WaitLogicFactory.*;
import static scala.collection.JavaConverters.seqAsJavaList;

public class ProbeDriverJavaTest {
    @Test
    public void openProject() {
        var config = Config.fromString("probe.workspace.path = \"classpath:/gradle-project\"");
        var fixture = IntelliJFixture.fromConfig(config, "probe", IdeProbeFixture.defaultEC());

        fixture.run().apply(intelliJ -> {
            var expectedProjectName = "foo";
            var projectRef = intelliJ.probe().openProject(intelliJ.workspace(), WaitLogic.Default()); // API issue - need to pass default WaitLogic (#220)
            assertEquals(new ProjectRef.ByName(expectedProjectName), projectRef);

            intelliJ.probe().await(
                    emptyBackgroundTasks(
                            DefaultCheckFrequency(),
                            DefaultEnsurePeriod(),
                            DefaultEnsureFrequency(),
                            Duration.create(10, MINUTES))
            );

            // API issue: need to use JavaConverters
            var actualModules = seqAsJavaList(intelliJ.probe().projectModel(projectRef).modules())
                    .stream()
                    .map(Module::name)
                    .collect(toList());
            var expectedModules = asList("foo.main", "foo.test", "foo.uiTest", "foo");
            assertEquals(expectedModules, actualModules);

            return null; //API issue - need to return null (#220)
        });
    }
}
