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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class Inferencer {	
	// Train model
	public Model trnModel;
	public Dictionary globalDict;
	private LDACmdOption option;
	
	private Model newModel;
	public int niters = 100;
	
	//-----------------------------------------------------
	// Init method
	//-----------------------------------------------------
	public boolean init(LDACmdOption option){
		this.option = option;
		trnModel = new Model();
		
		if (!trnModel.initEstimatedModel(option))
			return false;		
		
		globalDict = trnModel.data.localDict;
		computeTrnTheta();
		computeTrnPhi();
		
		return true;
	}
	
	//inference new model ~ getting data from a specified dataset
	public Model inference( LDADataset newData){
		System.out.println("init new model");
		Model newModel = new Model();		
		
		newModel.initNewModel(option, newData, trnModel);		
		this.newModel = newModel;		
		
		System.out.println("Sampling " + niters + " iteration for inference!");		
		for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
			//System.out.println("Iteration " + newModel.liter + " ...");
			
			// for all newz_i
			for (int m = 0; m < newModel.M; ++m){
				for (int n = 0; n < newModel.data.docs[m].length; n++){
					// (newz_i = newz[m][n]
					// sample from p(z_i|z_-1,w)
					int topic = infSampling(m, n);
					newModel.z[m].set(n, topic);
				}
			}//end foreach new doc
			
		}// end iterations
		
		System.out.println("Gibbs sampling for inference completed!");
		
		computeNewTheta();
		computeNewPhi();
		newModel.liter--;
		return this.newModel;
	}
	
	public Model inference(String [] strs){
		//System.out.println("inference");
		Model newModel = new Model();
		
		//System.out.println("read dataset");
		LDADataset dataset = LDADataset.readDataSet(strs, globalDict);
		
		return inference(dataset);
	}
	
	//inference new model ~ getting dataset from file specified in option
	public Model inference(){	
		//System.out.println("inference");
		
		newModel = new Model();
		if (!newModel.initNewModel(option, trnModel)) return null;
		
		System.out.println("Sampling " + niters + " iteration for inference!");
		
		for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
			//System.out.println("Iteration " + newModel.liter + " ...");
			
			// for all newz_i
			for (int m = 0; m < newModel.M; ++m){
				for (int n = 0; n < newModel.data.docs[m].length; n++){
					// (newz_i = newz[m][n]
					// sample from p(z_i|z_-1,w)
					int topic = infSampling(m, n);
					newModel.z[m].set(n, topic);
				}
			}//end foreach new doc
			
		}// end iterations
		
		System.out.println("Gibbs sampling for inference completed!");		
		System.out.println("Saving the inference outputs!");
		
		computeNewTheta();
		computeNewPhi();
		newModel.liter--;
		newModel.saveModel(newModel.dfile + "." + newModel.modelName);		
		
		return newModel;
	}
	
	/**
	 * do sampling for inference
	 * m: document number
	 * n: word number?
	 */
	protected int infSampling(int m, int n){
		// remove z_i from the count variables
		int topic = newModel.z[m].get(n);
		int _w = newModel.data.docs[m].words[n];
		int w = newModel.data.lid2gid.get(_w);
		newModel.nw[_w][topic] -= 1;
		newModel.nd[m][topic] -= 1;
		newModel.nwsum[topic] -= 1;
		newModel.ndsum[m] -= 1;
		
		double Vbeta = trnModel.V * newModel.beta;
		double Kalpha = trnModel.K * newModel.alpha;
		
		// do multinomial sampling via cummulative method		
		for (int k = 0; k < newModel.K; k++){			
			newModel.p[k] = (trnModel.nw[w][k] + newModel.nw[_w][k] + newModel.beta)/(trnModel.nwsum[k] +  newModel.nwsum[k] + Vbeta) *
					(newModel.nd[m][k] + newModel.alpha)/(newModel.ndsum[m] + Kalpha);
		}
		
		// cummulate multinomial parameters
		for (int k = 1; k < newModel.K; k++){
			newModel.p[k] += newModel.p[k - 1];
		}
		
		// scaled sample because of unnormalized p[]
		double u = Math.random() * newModel.p[newModel.K - 1];
		
		for (topic = 0; topic < newModel.K; topic++){
			if (newModel.p[topic] > u)
				break;
		}
		
		// add newly estimated z_i to count variables
		newModel.nw[_w][topic] += 1;
		newModel.nd[m][topic] += 1;
		newModel.nwsum[topic] += 1;
		newModel.ndsum[m] += 1;
		
		return topic;
	}
	
	protected void computeNewTheta(){
		for (int m = 0; m < newModel.M; m++){
			for (int k = 0; k < newModel.K; k++){
				newModel.theta[m][k] = (newModel.nd[m][k] + newModel.alpha) / (newModel.ndsum[m] + newModel.K * newModel.alpha);
			}//end foreach topic
		}//end foreach new document
	}
	
	protected void computeNewPhi(){
		for (int k = 0; k < newModel.K; k++){
			for (int _w = 0; _w < newModel.V; _w++){
				Integer id = newModel.data.lid2gid.get(_w);
				
				if (id != null){
					newModel.phi[k][_w] = (trnModel.nw[id][k] + newModel.nw[_w][k] + newModel.beta) / (newModel.nwsum[k] + newModel.nwsum[k] + trnModel.V * newModel.beta);
				}
			}//end foreach word
		}// end foreach topic
	}
	
	protected void computeTrnTheta(){
		for (int m = 0; m < trnModel.M; m++){
			for (int k = 0; k < trnModel.K; k++){
				trnModel.theta[m][k] = (trnModel.nd[m][k] + trnModel.alpha) / (trnModel.ndsum[m] + trnModel.K * trnModel.alpha);
			}
		}
	}
	
	protected void computeTrnPhi(){
		for (int k = 0; k < trnModel.K; k++){
			for (int w = 0; w < trnModel.V; w++){
				trnModel.phi[k][w] = (trnModel.nw[w][k] + trnModel.beta) / (trnModel.nwsum[k] + trnModel.V * trnModel.beta);
			}
		}
	}
}
