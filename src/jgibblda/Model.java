/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package jgibblda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

public class Model {	

    //---------------------------------------------------------------
    //	Class Variables
    //---------------------------------------------------------------

    public static String tassignSuffix = ".tassign.gz";	 // suffix for topic assignment file
    public static String thetaSuffix   = ".theta.gz";    // suffix for theta (topic - document distribution) file
    public static String phiSuffix     = ".phi.gz";      // suffix for phi file (topic - word distribution) file
    public static String othersSuffix  = ".others.gz"; 	 // suffix for containing other parameters
    public static String twordsSuffix  = ".twords.gz";	 // suffix for file containing words-per-topics
    public static String wordMapSuffix  = ".wordmap.gz"; // suffix for file containing word to id map

    //---------------------------------------------------------------
    //	Model Parameters and Variables
    //---------------------------------------------------------------


    public String dir = "./";
    public String dfile = "trndocs.dat";
    public boolean unlabeled = false;
    public String modelName = "model";
    public LDADataset data; // link to a dataset

    public int M = 0;          // dataset size (i.e., number of docs)
    public int V = 0;          // vocabulary size
    public int K = 100;        // number of topics
    public double alpha;       // LDA hyperparameters
    public double beta = 0.01; // LDA hyperparameters
    public int niters = 1000;  // number of Gibbs sampling iteration
    public int nburnin = 500;  // number of Gibbs sampling burn-in iterations
    public int samplingLag = 5;// Gibbs sampling sample lag
    public int numSamples = 1; // number of samples taken
    public int liter = 0;      // the iteration at which the model was saved	
    public int twords = 20;    // print out top words per each topic

    // Estimated/Inferenced parameters
    public double[][] theta = null; // theta: document - topic distributions, size M x K
    public double[][] phi = null;   // phi: topic-word distributions, size K x V

    // Temp variables while sampling
    public TIntArrayList[] z = null; // topic assignments for words, size M x doc.size()
    protected int[][] nw = null;       // nw[i][j]: number of instances of word/term i assigned to topic j, size V x K
    protected int[][] nd = null;       // nd[i][j]: number of words in document i assigned to topic j, size M x K
    protected int[] nwsum = null;      // nwsum[j]: total number of words assigned to topic j, size K
    protected int[] ndsum = null;      // ndsum[i]: total number of words in document i, size M

    protected ArrayList<TIntObjectHashMap<int[]>> nw_inf = null;       // nw[m][i][j]: number of instances of word/term i assigned to topic j in doc m, size M x V x K
    protected int[][] nwsum_inf = null;      // nwsum[m][j]: total number of words assigned to topic j in doc m, size M x K

    // temp variables for sampling
    protected double[] p = null; 

    //---------------------------------------------------------------
    //	Constructors
    //---------------------------------------------------------------	

    public Model(LDACmdOption option) throws FileNotFoundException, IOException
    {
        this(option, null);
    }

    public Model(LDACmdOption option, Model trnModel) throws FileNotFoundException, IOException
    {
        modelName = option.modelName;
        K = option.K;

        alpha = option.alpha;
        if (alpha < 0.0)
            alpha = 50.0 / K;

        if (option.beta >= 0)
            beta = option.beta;

        niters = option.niters;
        nburnin = option.nburnin;
        samplingLag = option.samplingLag;

        dir = option.dir;
        if (dir.endsWith(File.separator))
            dir = dir.substring(0, dir.length() - 1);

        dfile = option.dfile;
        unlabeled = option.unlabeled;
        twords = option.twords;

        // initialize dataset
        data = new LDADataset();

        // process trnModel (if given)
        if (trnModel != null) {
            data.setDictionary(trnModel.data.localDict);
            K = trnModel.K;

            // use hyperparameters from model (if not overridden in options)
            if (option.alpha < 0.0)
                alpha = trnModel.alpha;
            if (option.beta < 0.0)
                beta = trnModel.beta;
        }

        // read in data
        data.readDataSet(dir + File.separator + dfile, unlabeled);
    }

    //---------------------------------------------------------------
    //	Init Methods
    //---------------------------------------------------------------

    /**
     * Init parameters for estimation or inference
     */
    public boolean init(boolean random)
    {
        if (random) {
            M = data.M;
            V = data.V;
            z = new TIntArrayList[M];
        } else {
            if (!loadModel()) {
                System.out.println("Fail to load word-topic assignment file of the model!"); 
                return false;
            }

            // debug output
            System.out.println("Model loaded:");
            System.out.println("\talpha:" + alpha);
            System.out.println("\tbeta:" + beta);
            System.out.println("\tK:" + K);
            System.out.println("\tM:" + M);
            System.out.println("\tV:" + V);
        }

        p = new double[K];

        initSS();

        for (int m = 0; m < data.M; m++){
            if (random) {
                z[m] = new TIntArrayList();
            }

            // initilize for z
            int N = data.docs.get(m).length;
            for (int n = 0; n < N; n++){
                int w = data.docs.get(m).words[n];
                int topic;

                // random init a topic or load existing topic from z[m]
                if (random) {
                    topic = (int)Math.floor(Math.random() * K);
                    z[m].add(topic);
                } else {
                    topic = z[m].get(n);
                }

                nw[w][topic]++; // number of instances of word assigned to topic j
                nd[m][topic]++; // number of words in document i assigned to topic j
                nwsum[topic]++; // total number of words assigned to topic j
            }

            ndsum[m] = N; // total number of words in document i
        }

        theta = new double[M][K];		
        phi = new double[K][V];

        return true;
    }

    public boolean initInf()
    {
        nw_inf = new ArrayList<TIntObjectHashMap<int[]>>();

        nwsum_inf = new int[M][K];
        for (int m = 0; m < M; m++) {
            for (int k = 0; k < K; k++) {
                nwsum_inf[m][k] = 0;
            }
        }

        for (int m = 0; m < data.M; m++){
            nw_inf.add(m, new TIntObjectHashMap<int[]>());

            // initilize for z
            int N = data.docs.get(m).length;
            for (int n = 0; n < N; n++){
                int w = data.docs.get(m).words[n];
                int topic = z[m].get(n);

                if (!nw_inf.get(m).containsKey(w)) {
                    int[] nw_inf_m_w = new int[K];
                    for (int k = 0; k < K; k++) {
                        nw_inf_m_w[k] = 0;
                    }
                    nw_inf.get(m).put(w, nw_inf_m_w);
                }

                nw_inf.get(m).get(w)[topic]++; // number of instances of word assigned to topic j in doc m
                //nw_inf[m][w][topic]++; // number of instances of word assigned to topic j in doc m
                nwsum_inf[m][topic]++; // total number of words assigned to topic j in doc m
            }
        }

        return true;
    }

    /**
     * Init sufficient stats
     */
    protected void initSS()
    {
        nw = new int[V][K];
        for (int w = 0; w < V; w++){
            for (int k = 0; k < K; k++){
                nw[w][k] = 0;
            }
        }

        nd = new int[M][K];
        for (int m = 0; m < M; m++){
            for (int k = 0; k < K; k++){
                nd[m][k] = 0;
            }
        }

        nwsum = new int[K];
        for (int k = 0; k < K; k++){
            nwsum[k] = 0;
        }

        ndsum = new int[M];
        for (int m = 0; m < M; m++){
            ndsum[m] = 0;
        }
    }

    //---------------------------------------------------------------
    //	Update Methods
    //---------------------------------------------------------------

    public void updateParams()
    {
        updateTheta();
        updatePhi();
        numSamples++;
    }
    public void updateParams(Model trnModel)
    {
        updateTheta();
        updatePhi(trnModel);
        numSamples++;
    }

    public void updateTheta()
    {
        double Kalpha = K * alpha;
        for (int m = 0; m < M; m++) {
            for (int k = 0; k < K; k++) {
                if (numSamples > 1) theta[m][k] *= numSamples - 1; // convert from mean to sum
                theta[m][k] += (nd[m][k] + alpha) / (ndsum[m] + Kalpha);
                if (numSamples > 1) theta[m][k] /= numSamples; // convert from sum to mean
            }
        }
    }

    public void updatePhi()
    {
        double Vbeta = V * beta;
        for (int k = 0; k < K; k++) {
            for (int w = 0; w < V; w++) {
                if (numSamples > 1) phi[k][w] *= numSamples - 1; // convert from mean to sum
                phi[k][w] += (nw[w][k] + beta) / (nwsum[k] + Vbeta);
                if (numSamples > 1) phi[k][w] /= numSamples; // convert from sum to mean
            }
        }
    }

    // for inference
    public void updatePhi(Model trnModel)
    {
        double Vbeta = trnModel.V * beta;
        for (int k = 0; k < K; k++) {
            for (int _w = 0; _w < V; _w++) {
                if (data.lid2gid.containsKey(_w)) {
                    int id = data.lid2gid.get(_w);

                    if (numSamples > 1) phi[k][_w] *= numSamples - 1; // convert from mean to sum
                    phi[k][_w] += (trnModel.nw[id][k] + nw[_w][k] + beta) / (trnModel.nwsum[k] + nwsum[k] + Vbeta);
                    if (numSamples > 1) phi[k][_w] /= numSamples; // convert from sum to mean
                } // else ignore words that don't appear in training
            } //end foreach word
        } // end foreach topic
    }

    //---------------------------------------------------------------
    //	I/O Methods
    //---------------------------------------------------------------

    /**
     * Save model
     */
    public boolean saveModel()
    {
        return saveModel("");
    }
    public boolean saveModel(String modelPrefix)
    {
        if (!saveModelTAssign(dir + File.separator + modelPrefix + modelName + tassignSuffix)) {
            return false;
        }

        if (!saveModelOthers(dir + File.separator + modelPrefix + modelName + othersSuffix)) {
            return false;
        }

        if (!saveModelTheta(dir + File.separator + modelPrefix + modelName + thetaSuffix)) {
            return false;
        }

        //if (!saveModelPhi(dir + File.separator + modelPrefix + modelName + phiSuffix)) {
        //    return false;
        //}

        if (twords > 0) {
            if (!saveModelTwords(dir + File.separator + modelPrefix + modelName + twordsSuffix)) {
                return false;
            }
        }

        if (!data.localDict.writeWordMap(dir + File.separator + modelPrefix + modelName + wordMapSuffix)) {
            return false;
        }

        return true;
    }

    /**
     * Save word-topic assignments for this model
     */
    public boolean saveModelTAssign(String filename) {
        int i, j;

        try{
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(
                            new FileOutputStream(filename)), "UTF-8"));

            //write docs with topic assignments for words
            for (i = 0; i < data.M; i++) {
                for (j = 0; j < data.docs.get(i).length; ++j) {
                    writer.write(data.docs.get(i).words[j] + ":" + z[i].get(j) + " ");
                }
                writer.write("\n");
            }

            writer.close();
        }
        catch (Exception e) {
            System.out.println("Error while saving model tassign: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Save theta (topic distribution) for this model
     */
    public boolean saveModelTheta(String filename) {
        try{
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(
                            new FileOutputStream(filename)), "UTF-8"));

            for (int i = 0; i < M; i++) {
                for (int j = 0; j < K; j++) {
                    if (theta[i][j] > 0) {
                        writer.write(j + ":" + theta[i][j] + " ");
                    }
                }
                writer.write("\n");
            }
            writer.close();
        }
        catch (Exception e){
            System.out.println("Error while saving topic distribution file for this model: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Save word-topic distribution
     */
    public boolean saveModelPhi(String filename)
    {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(
                            new FileOutputStream(filename)), "UTF-8"));

            for (int i = 0; i < K; i++) {
                for (int j = 0; j < V; j++) {
                    if (phi[i][j] > 0) {
                        writer.write(j + ":" + phi[i][j] + " ");
                    }
                }
                writer.write("\n");
            }
            writer.close();
        }
        catch (Exception e) {
            System.out.println("Error while saving word-topic distribution:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Save other information of this model
     */
    public boolean saveModelOthers(String filename){
        try{
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(
                            new FileOutputStream(filename)), "UTF-8"));

            writer.write("alpha=" + alpha + "\n");
            writer.write("beta=" + beta + "\n");
            writer.write("ntopics=" + K + "\n");
            writer.write("ndocs=" + M + "\n");
            writer.write("nwords=" + V + "\n");
            writer.write("liters=" + liter + "\n");

            writer.close();
        }
        catch(Exception e){
            System.out.println("Error while saving model others:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Save model the most likely words for each topic
     */
    public boolean saveModelTwords(String filename){
        try{
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(
                            new FileOutputStream(filename)), "UTF-8"));

            if (twords > V){
                twords = V;
            }

            for (int k = 0; k < K; k++){
                ArrayList<Pair> wordsProbsList = new ArrayList<Pair>(); 
                for (int w = 0; w < V; w++){
                    Pair p = new Pair(w, phi[k][w], false);

                    wordsProbsList.add(p);
                }//end foreach word

                //print topic				
                writer.write("Topic " + k + ":\n");
                Collections.sort(wordsProbsList);

                for (int i = 0; i < twords; i++){
                    if (data.localDict.contains((Integer)wordsProbsList.get(i).first)){
                        String word = data.localDict.getWord((Integer)wordsProbsList.get(i).first);

                        writer.write("\t" + word + "\t" + wordsProbsList.get(i).second + "\n");
                    }
                }
            } //end foreach topic			

            writer.close();
        }
        catch(Exception e){
            System.out.println("Error while saving model twords: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Load saved model
     */
    public boolean loadModel(){
        if (!readOthersFile(dir + File.separator + modelName + othersSuffix))
            return false;

        if (!readTAssignFile(dir + File.separator + modelName + tassignSuffix))
            return false;

        // read dictionary
        Dictionary dict = new Dictionary();
        if (!dict.readWordMap(dir + File.separator + modelName + wordMapSuffix))
            return false;

        data.localDict = dict;

        return true;
    }

    /**
     * Load "others" file to get parameters
     */
    protected boolean readOthersFile(String otherFile){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(
                            new FileInputStream(otherFile)), "UTF-8"));
            String line;
            while((line = reader.readLine()) != null){
                StringTokenizer tknr = new StringTokenizer(line,"= \t\r\n");

                int count = tknr.countTokens();
                if (count != 2)
                    continue;

                String optstr = tknr.nextToken();
                String optval = tknr.nextToken();

                if (optstr.equalsIgnoreCase("alpha")){
                    alpha = Double.parseDouble(optval);					
                }
                else if (optstr.equalsIgnoreCase("beta")){
                    beta = Double.parseDouble(optval);
                }
                else if (optstr.equalsIgnoreCase("ntopics")){
                    K = Integer.parseInt(optval);
                }
                else if (optstr.equalsIgnoreCase("liter")){
                    liter = Integer.parseInt(optval);
                }
                else if (optstr.equalsIgnoreCase("nwords")){
                    V = Integer.parseInt(optval);
                }
                else if (optstr.equalsIgnoreCase("ndocs")){
                    M = Integer.parseInt(optval);
                }
                else {
                    // any more?
                }
            }

            reader.close();
        }
        catch (Exception e){
            System.out.println("Error while reading other file:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Load word-topic assignments for this model
     */
    protected boolean readTAssignFile(String tassignFile)
    {
        try {
            int i,j;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(
                            new FileInputStream(tassignFile)), "UTF-8"));

            String line;
            z = new TIntArrayList[M];			
            data = new LDADataset();
            data.setM(M);
            data.V = V;			
            for (i = 0; i < M; i++){
                line = reader.readLine();
                StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");

                int length = tknr.countTokens();

                TIntArrayList words = new TIntArrayList();
                TIntArrayList topics = new TIntArrayList();
                for (j = 0; j < length; j++){
                    String token = tknr.nextToken();

                    StringTokenizer tknr2 = new StringTokenizer(token, ":");
                    if (tknr2.countTokens() != 2){
                        System.out.println("Invalid word-topic assignment line\n");
                        return false;
                    }

                    words.add(Integer.parseInt(tknr2.nextToken()));
                    topics.add(Integer.parseInt(tknr2.nextToken()));
                }//end for each topic assignment

                //allocate and add new document to the corpus
                Document doc = new Document(words);
                data.setDoc(doc, i);

                //assign values for z
                z[i] = new TIntArrayList();
                for (j = 0; j < topics.size(); j++){
                    z[i].add(topics.get(j));
                }

            }//end for each doc

            reader.close();
        }
        catch (Exception e){
            System.out.println("Error while loading model: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
