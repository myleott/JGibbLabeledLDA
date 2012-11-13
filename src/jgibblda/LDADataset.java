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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class LDADataset {
	//---------------------------------------------------------------
	// Instance Variables
	//---------------------------------------------------------------
	
	public Dictionary localDict;			// local dictionary	
	public Document [] docs; 		// a list of documents	
	public int M; 			 		// number of documents
	public int V;			 		// number of words
	
	// map from local coordinates (id) to global ones 
	// null if the global dictionary is not set
	public Map<Integer, Integer> lid2gid; 
	
	//link to a global dictionary (optional), null for train data, not null for test data
	public Dictionary globalDict;	 		
	
	//--------------------------------------------------------------
	// Constructor
	//--------------------------------------------------------------
	public LDADataset(){
		localDict = new Dictionary();
		M = 0;
		V = 0;
		docs = null;
	
		globalDict = null;
		lid2gid = null;
	}
	
	public LDADataset(int M){
		localDict = new Dictionary();
		this.M = M;
		this.V = 0;
		docs = new Document[M];	
		
		globalDict = null;
		lid2gid = null;
	}
	
	public LDADataset(int M, Dictionary globalDict){
		localDict = new Dictionary();	
		this.M = M;
		this.V = 0;
		docs = new Document[M];	
		
		this.globalDict = globalDict;
		lid2gid = new HashMap<Integer, Integer>();
	}
	
	//-------------------------------------------------------------
	//Public Instance Methods
	//-------------------------------------------------------------
	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param doc document to be set
	 * @param idx index in the document array
	 */	
	public void setDoc(Document doc, int idx){
		if (0 <= idx && idx < M){
			docs[idx] = doc;
		}
	}
	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param str string contains doc
	 * @param idx index in the document array
	 */
	public void setDoc(String str, int idx){
		if (0 <= idx && idx < M){
			String [] words = str.split("[ \\t\\n]");
			
			Vector<Integer> ids = new Vector<Integer>();
			
			for (String word : words){
				int _id = localDict.word2id.size();
				
				if (localDict.contains(word))		
					_id = localDict.getID(word);
								
				if (globalDict != null){
					//get the global id					
					Integer id = globalDict.getID(word);
					//System.out.println(id);
					
					if (id != null){
						localDict.addWord(word);
						
						lid2gid.put(_id, id);
						ids.add(_id);
					}
					else { //not in global dictionary
						//do nothing currently
					}
				}
				else {
					localDict.addWord(word);
					ids.add(_id);
				}
			}
			
			Document doc = new Document(ids, str);
			docs[idx] = doc;
			V = localDict.word2id.size();			
		}
	}
	//---------------------------------------------------------------
	// I/O methods
	//---------------------------------------------------------------
	
	/**
	 *  read a dataset from a stream, create new dictionary
	 *  @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(String filename){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			
			LDADataset data = readDataSet(reader);
			
			reader.close();
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * read a dataset from a file with a preknown vocabulary
	 * @param filename file from which we read dataset
	 * @param dict the dictionary
	 * @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(String filename, Dictionary dict){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			LDADataset data = readDataSet(reader, dict);
			
			reader.close();
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 *  read a dataset from a stream, create new dictionary
	 *  @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(BufferedReader reader){
		try {
			//read number of document
			String line;
			line = reader.readLine();
			int M = Integer.parseInt(line);
			
			LDADataset data = new LDADataset(M);
			for (int i = 0; i < M; ++i){
				line = reader.readLine();
				
				data.setDoc(line, i);
			}
			
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * read a dataset from a stream with respect to a specified dictionary
	 * @param reader stream from which we read dataset
	 * @param dict the dictionary
	 * @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(BufferedReader reader, Dictionary dict){
		try {
			//read number of document
			String line;
			line = reader.readLine();
			int M = Integer.parseInt(line);
			System.out.println("NewM:" + M);
			
			LDADataset data = new LDADataset(M, dict);
			for (int i = 0; i < M; ++i){
				line = reader.readLine();
				
				data.setDoc(line, i);
			}
			
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * read a dataset from a string, create new dictionary
	 * @param str String from which we get the dataset, documents are seperated by newline character 
	 * @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(String [] strs){
		LDADataset data = new LDADataset(strs.length);
		
		for (int i = 0 ; i < strs.length; ++i){
			data.setDoc(strs[i], i);
		}
		return data;
	}
	
	/**
	 * read a dataset from a string with respect to a specified dictionary
	 * @param str String from which we get the dataset, documents are seperated by newline character	
	 * @param dict the dictionary
	 * @return dataset if success and null otherwise
	 */
	public static LDADataset readDataSet(String [] strs, Dictionary dict){
		//System.out.println("readDataset...");
		LDADataset data = new LDADataset(strs.length, dict);
		
		for (int i = 0 ; i < strs.length; ++i){
			//System.out.println("set doc " + i);
			data.setDoc(strs[i], i);
		}
		return data;
	}
}
