package clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class Entropy {
	public static String folderPath = "C:\\Users\\Michael\\Desktop\\CS 185C\\Malware\\";
	public static String[] families = {"harebot", "cridex", "securityShield", "smarthdd", "zbot", "winwebsec", "zeroaccess"};
	
	
	public static void main(String[] args) throws IOException {
		int[] tupleCounts = new int[5];
		
		for(int i = 0; i < tupleCounts.length; i++) {
			String tupleName = String.format("7 family tuples\\opcode Frequencies %d-tuple.txt", i+1);
			String filePath = folderPath + tupleName;
			tupleCounts[i] = (int) Files.lines(Paths.get(filePath)).count();
		}
		
		//String family = "cridex";
		for(String family : families) {
			File folder = new File(folderPath + "samples\\" + family);
			
			File save = new File(folderPath + family + " cluster.txt");
			FileWriter fw = new FileWriter(save);
			
			for(File file : folder.listFiles()) {
				fw.write(processFile(file, tupleCounts));
				fw.write("\n");
			}
			fw.close();
		}
	}
	
	public static String processFile(File file, int[] tupleCounts) {
		StringBuilder string = new StringBuilder();
		
		string.append(file.length() + ",");	//file size in bytes
		string.append(distinctOpcodes(readFile(file, 1)) + ",");	//number of different opcodes
		
		for(int i = 0; i < tupleCounts.length; i++){	//entropy modified by only looking at know opcode tuples rather than all possible tuples
			double entropy = calculateEntropy(file, tupleCounts[i], i+1);
			string.append(entropy);
			if(i != tupleCounts.length-1)
				string.append(",");
		}
		
		return string.toString();
	}
	
	public static int distinctOpcodes(ArrayList<String> arrayList) {
		HashSet<String> opcodes = new HashSet<String>();
		for(String i : arrayList)
			opcodes.add(i);
		
		return opcodes.size();
	}
	
	public static double calculateEntropy(File file, int distinctOpcodes, int numTuple) {
		ArrayList<String> sequence = readFile(file, numTuple);
		HashMap<String, int[]> frequencies = new HashMap<String, int[]>(); //int[] is of size one / a pseudo-pointer for java
		
		for(String str : sequence) {
			if(frequencies.containsKey(str))
				frequencies.get(str)[0]++;
			else
				frequencies.put(str, new int[] {1});
		}
		
		double entropy = 0;
		for(int[] count : frequencies.values()) {
			double p = 1.0 * count[0] / sequence.size();
			entropy -= p * log(p, Math.pow(distinctOpcodes, numTuple));
		}
		
		return entropy * numTuple;
	}
	
	public static ArrayList<String> readFile(File file, int numTuple) {
		ArrayList<String> sequence = new ArrayList<String>();
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader bf = new BufferedReader(fr);
			
			LinkedList<String> tuple = new LinkedList<String>();
			for(int i = 0; i < numTuple; i++) {
				String line = bf.readLine();
				tuple.add(line.replace("[^a-z]", ""));
			}
			
			String line;
			while((line = bf.readLine()) != null) {
				line = line.replace("[^a-z]", ""); //remove non alpha
				
				//collect n lines together
				String str = stackToString(tuple);
				sequence.add(str);
				
				//update stack for collection
				tuple.removeFirst();
				tuple.add(line);
			}
			bf.close();
			fr.close();
			
			return sequence;	//convert linked list to int[]
			
		} catch(IOException e) {e.printStackTrace();}
		
		return null;
	}
	
	private static double log(double a, double b) {
		return Math.log(a) / Math.log(b);
	}
	
	private static String stackToString(LinkedList<String> stack) {
		StringBuilder str = new StringBuilder();
		
		for(String string : stack)
			str.append(string);

		return str.toString();
	}
}
