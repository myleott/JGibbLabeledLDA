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

import java.util.Vector;

public class Document {

	//----------------------------------------------------
	//Instance Variables
	//----------------------------------------------------
	public int [] words;
	public String rawStr;
	public int length;
	
	//----------------------------------------------------
	//Constructors
	//----------------------------------------------------
	public Document(){
		words = null;
		rawStr = "";
		length = 0;
	}
	
	public Document(int length){
		this.length = length;
		rawStr = "";
		words = new int[length];
	}
	
	public Document(int length, int [] words){
		this.length = length;
		rawStr = "";
		
		this.words = new int[length];
		for (int i =0 ; i < length; ++i){
			this.words[i] = words[i];
		}
	}
	
	public Document(int length, int [] words, String rawStr){
		this.length = length;
		this.rawStr = rawStr;
		
		this.words = new int[length];
		for (int i =0 ; i < length; ++i){
			this.words[i] = words[i];
		}
	}
	
	public Document(Vector<Integer> doc){
		this.length = doc.size();
		rawStr = "";
		this.words = new int[length];
		for (int i = 0; i < length; i++){
			this.words[i] = doc.get(i);
		}
	}
	
	public Document(Vector<Integer> doc, String rawStr){
		this.length = doc.size();
		this.rawStr = rawStr;
		this.words = new int[length];
		for (int i = 0; i < length; ++i){
			this.words[i] = doc.get(i);
		}
	}
}
