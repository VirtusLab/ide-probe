package org.virtuslab.ideprobe;

import org.junit.Test;
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ;
import org.virtuslab.ideprobe.ide.intellij.IntelliJProvider;
import org.virtuslab.ideprobe.protocol.Module;
import org.virtuslab.ideprobe.protocol.ProjectRef;
import scala.Function2;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;

import java.nio.file.Path;
import java.util.ArrayList;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.virtuslab.ideprobe.WaitLogic.emptyBackgroundTasks;
import static org.virtuslab.ideprobe.wait.WaitLogicFactory.*;
import static scala.collection.JavaConverters.collectionAsScalaIterable;
import static scala.collection.JavaConverters.seqAsJavaList;

public class ProbeDriverJavaTest {
    @Test
    public void openProject() {
        var workspaceProvider = new WorkspaceTemplate.FromResource("gradle-project");
        var fixture = new IntelliJFixture(
                workspaceProvider,
                IntelliJProvider.Default(),
                Config.Empty(),
                // API issue - need to use scalaconverters and scala's Function2 (#220)
                collectionAsScalaIterable(new ArrayList<Function2<IntelliJFixture, Path, BoxedUnit>>()).toSeq(),
                collectionAsScalaIterable(new ArrayList<Function2<IntelliJFixture, InstalledIntelliJ, BoxedUnit>>()).toSeq(),
                collectionAsScalaIterable(new ArrayList<Function2<IntelliJFixture, RunningIntelliJFixture, BoxedUnit>>()).toSeq(),
                IdeProbeFixture.defaultEC());

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
