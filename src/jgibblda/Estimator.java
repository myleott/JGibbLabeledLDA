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

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

public class Estimator {

    // output model
    protected Model trnModel;
    LDACmdOption option;

    public Estimator(LDACmdOption option){
        this.option = option;
        trnModel = new Model(option);

        if (option.est){
            trnModel.init(true);
            trnModel.data.localDict.writeWordMap(option.dir + File.separator + option.wordMapFileName);
        }
        else if (option.estc){
            trnModel.init(false);
        }
    }

    public void estimate(){
        System.out.println("Sampling " + trnModel.niters + " iteration!");

        int lastIter = trnModel.liter;
        for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters + lastIter; trnModel.liter++){
            System.out.println("Iteration " + trnModel.liter + " ...");

            // for all z_i
            for (int m = 0; m < trnModel.M; m++){				
                for (int n = 0; n < trnModel.data.docs.get(m).length; n++){
                    // z_i = z[m][n]
                    // sample from p(z_i|z_-i, w)
                    int topic = sampling(m, n);
                    trnModel.z[m].set(n, topic);
                }// end for each word
            }// end for each document

            if (option.savestep > 0){
                if (trnModel.liter % option.savestep == 0){
                    System.out.println("Saving the model at iteration " + trnModel.liter + " ...");
                    trnModel.updateTheta();
                    trnModel.updatePhi();
                    trnModel.saveModel("model-" + Conversion.ZeroPad(trnModel.liter, 5));
                }
            }
        }// end iterations		

        System.out.println("Gibbs sampling completed!");
        System.out.println("Saving the final model!");
        trnModel.updateTheta();
        trnModel.updatePhi();
        trnModel.liter--;
        trnModel.saveModel("model-final");
    }

    /**
     * Do sampling
     * @param m document number
     * @param n word number
     * @return topic id
     */
    public int sampling(int m, int n){
        // remove z_i from the count variable
        int topic = trnModel.z[m].get(n);
        int w = trnModel.data.docs.get(m).words[n];
        double[] p = trnModel.p;

        trnModel.nw[w][topic] -= 1;
        trnModel.nd[m][topic] -= 1;
        trnModel.nwsum[topic] -= 1;
        trnModel.ndsum[m] -= 1;

        double Vbeta = trnModel.V * trnModel.beta;
        double Kalpha = trnModel.K * trnModel.alpha;

        // get labels for this document
        ArrayList<Integer> labels = trnModel.data.docs.get(m).labels;

        // determine number of possible topics for this document
        int K_m = (labels == null) ? trnModel.K : labels.size();

        //do multinominal sampling via cumulative method
        if (labels == null) {
            for (int k = 0; k < K_m; k++){
                p[k] = (trnModel.nw[w][k] + trnModel.beta)/(trnModel.nwsum[k] + Vbeta) *
                       (trnModel.nd[m][k] + trnModel.alpha);// /(trnModel.ndsum[m] + Kalpha); // indep of k
            }
        } else {
            int i = 0;
            for (int k : labels) {
                p[i++] = (trnModel.nw[w][k] + trnModel.beta)/(trnModel.nwsum[k] + Vbeta) *
                         (trnModel.nd[m][k] + trnModel.alpha);// /(trnModel.ndsum[m] + Kalpha); // indep of k
            }
        }

        // cumulate multinomial parameters
        for (int k = 1; k < K_m; k++) {
            p[k] += p[k - 1];
        }

        // scaled sample because of unnormalized p[]
        double u = Math.random() * p[K_m - 1];

        for (topic = 0; topic < K_m; topic++){
            if (p[topic] > u) //sample topic w.r.t distribution p
                break;
        }

        // map [0, K_m - 1] topic to [0, K - 1] topic according to labels
        if (labels != null) {
            topic = labels.get(topic);
        }

        // add newly estimated z_i to count variables
        trnModel.nw[w][topic] += 1;
        trnModel.nd[m][topic] += 1;
        trnModel.nwsum[topic] += 1;
        trnModel.ndsum[m] += 1;

        return topic;
    }
}
