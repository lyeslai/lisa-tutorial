package it.unive.lisa.tutorial;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import org.junit.Test;

public class IntervalsWithOverflowDomainTest {

    @Test
    public void testIntervalsWithOverflow() throws ParsingException, AnalysisException {
        Program program = IMPFrontend.processFile("inputs/intervalsoverflows.imp");
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/intervalsWithOverflow"; // Ajusté pour cohérence
        conf.analysisGraphs = GraphType.HTML;
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment(new IntervalsWithOverflowDomain()),
                DefaultConfiguration.defaultTypeDomain()
        );
        LiSA lisa = new LiSA(conf);
        lisa.run(new Program[]{program});
    }
}