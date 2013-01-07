package jgibblda;

import org.kohsuke.args4j.*;

public class LDACmdOption {

    @Option(name="-est", usage="Specify whether we want to estimate model from scratch")
        public boolean est = false;

    @Option(name="-estc", usage="Specify whether we want to continue the last estimation")
        public boolean estc = false;

    @Option(name="-inf", usage="Specify whether we want to do inference")
        public boolean inf = true;

    @Option(name="-infseparately", usage="Do inference for each document separately")
        public boolean infSeparately = false;

    @Option(name="-unlabeled", usage="Ignore document labels")
        public boolean unlabeled = false;

    @Option(name="-dir", usage="Specify directory")
        public String dir = "";

    @Option(name="-dfile", usage="Specify data file (*.gz)")
        public String dfile = "";

    @Option(name="-model", usage="Specify the model name")
        public String modelName = "";

    @Option(name="-alpha", usage="Specify alpha")
        public double alpha = -1;

    @Option(name="-beta", usage="Specify beta")
        public double beta = -1;

    @Option(name="-ntopics", usage="Specify the number of topics")
        public int K = 100;

    @Option(name="-niters", usage="Specify the number of iterations")
        public int niters = 1000;

    @Option(name="-nburnin", usage="Specify the number of burn-in iterations")
        public int nburnin = 500;

    @Option(name="-samplinglag", usage="Specify the sampling lag")
        public int samplingLag = 5;

    @Option(name="-twords", usage="Specify the number of most likely words to be printed for each topic")
        public int twords = 100;
}
