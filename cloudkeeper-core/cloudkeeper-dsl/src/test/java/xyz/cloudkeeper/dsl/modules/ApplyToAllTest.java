package xyz.cloudkeeper.dsl.modules;

import cloudkeeper.types.ByteSequence;
import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.CompositeModule;
import xyz.cloudkeeper.dsl.CompositeModulePlugin;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;
import xyz.cloudkeeper.dsl.types.BAMFile;
import xyz.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import xyz.cloudkeeper.model.bare.type.BareDeclaredType;

import java.util.Collection;

public class ApplyToAllTest {
    @SimpleModulePlugin("Test module that pretends to split a BAM file, by chromosome, into an array of BAM files.")
    public abstract static class SplitByChromosome extends SimpleModule<SplitByChromosome> {
        public abstract InPort<BAMFile> inBAMFile();
        public abstract OutPort<Collection<BAMFile>> outBAMFiles();
    }

    @SimpleModulePlugin("Test module that pretends to compute statistics from a BAM file.")
    public abstract static class ChromosomeStatisticsModule extends SimpleModule<ChromosomeStatisticsModule> {
        public abstract InPort<BAMFile> inChromosomeBAMFile();
        public abstract OutPort<ByteSequence> outStatsFile();
    }

    @SimpleModulePlugin("Test module that pretends to combine multiple statistics files into one.")
    public abstract static class JoinStatistics extends SimpleModule<JoinStatistics> {
        public abstract InPort<Collection<ByteSequence>> inStatsFiles();
        public abstract OutPort<ByteSequence> outStatsFile();
    }

    @CompositeModulePlugin("Test module that pretends to gather statistics")
    public abstract static class GatherStatisticsModule extends CompositeModule<GatherStatisticsModule> {
        public abstract InPort<BAMFile> inBAMFile();
        public abstract OutPort<ByteSequence> outStatsFile();

        private final SplitByChromosome splitModule = child(SplitByChromosome.class).
            inBAMFile().from(inBAMFile());
        private final ChromosomeStatisticsModule statsModule = child(ChromosomeStatisticsModule.class).
            inChromosomeBAMFile().from(forEach(splitModule.outBAMFiles()));
        private final JoinStatistics joinModule = child(JoinStatistics.class).
            inStatsFiles().from(arrayOf(statsModule.outStatsFile()));

        { outStatsFile().from(joinModule.outStatsFile()); }
    }

    @Test
    public void instantiate() {
        GatherStatisticsModule gatherStatisticsModule = ModuleFactory.getDefault().create(GatherStatisticsModule.class);
        Assert.assertEquals(gatherStatisticsModule.getModules().size(), 3);
        Assert.assertEquals(
            ((BareDeclaredType) gatherStatisticsModule.inBAMFile().getType())
                .getDeclaration().getQualifiedName().toString(),
            BAMFile.class.getName()
        );
        Assert.assertEquals(
            ((BareDeclaredType)
                ((BareDeclaredType) gatherStatisticsModule.splitModule.outBAMFiles().getType())
                    .getTypeArguments().get(0)
            ).getDeclaration().getQualifiedName().toString(),
            BAMFile.class.getName()
        );
        Assert.assertSame(
            gatherStatisticsModule.getModules().get(0),
            gatherStatisticsModule.splitModule
        );

        BareCompositeModuleDeclaration declaration
            = (BareCompositeModuleDeclaration) ModuleFactory.getDefault().loadDeclaration(GatherStatisticsModule.class);
        Assert.assertEquals(declaration.getDeclaredAnnotations().size(), 0);
    }
}
