package hmm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Main {

	
	public static final int MAX_FAMILY_SIZE = 53;
	public static final double TRAIN_PERCENT = 0.8;
	public static String folderPath = "C:\\Users\\Michael\\Desktop\\CS 185C\\Malware\\";

	public static String[] families = {"harebot", "cridex", "securityShield", "smarthdd", "zbot", "winwebsec", "zeroaccess"};
	
	public ArrayList<int[]> hyperParameters;
	
	public Main() {
		hyperParameters = new ArrayList<int[]>();
							//state, tuples, opcodes
		hyperParameters.add(new int[]{2,1,30});
		hyperParameters.add(new int[]{2,2,40});
		hyperParameters.add(new int[]{2,3,50});
		hyperParameters.add(new int[]{2,4,70});
		hyperParameters.add(new int[]{3,4,70});
		hyperParameters.add(new int[]{4,4,70});
		hyperParameters.add(new int[]{2,5,70});
		hyperParameters.add(new int[]{3,5,120});
		hyperParameters.add(new int[]{6,5,120});
		
		
		/*
		hyperParameters.add(new int[]{2,6,30});
		hyperParameters.add(new int[]{3,6,10});
		hyperParameters.add(new int[]{4,6,50});
		hyperParameters.add(new int[]{3,7,40});
		hyperParameters.add(new int[]{3,8,40});
		*/
	}
	
	public static void main(String[] args) {
		/*
		//int familyNum = 1;
		boolean train = true;
		Main main = new Main();
		for(int familyNum = 0; familyNum < families.length; familyNum++) {
			for(int[] hp : main.hyperParameters) {
				int numStates = hp[0];
				int tupleSize = hp[1];
				int numOpcodes = hp[2];
				String tupleName = String.format("7 family tuples\\opcode Frequencies %d-tuple.txt", tupleSize);
				String mapFile = folderPath + tupleName;
				
				Map<String, Integer> map = main.getMap(mapFile, tupleSize, numOpcodes);
				
				ArrayList<int[]> allSamples = main.getSamples(folderPath+"samples\\"+families[familyNum], map, tupleSize);
	
				int start = train ? 0 : (int) (MAX_FAMILY_SIZE * TRAIN_PERCENT);
				int stop = train ? (int) (MAX_FAMILY_SIZE * TRAIN_PERCENT) : MAX_FAMILY_SIZE;
				
				int[] samples = main.combineSamples(allSamples.subList(start, stop));
				
				HiddenMarkovModel hmm = new HiddenMarkovModel(numStates, map.keySet().size()+1, samples);
				
				//HashMap<String, ArrayList<int[]>> familySamples = main.getAllSampleMap(map);
				
				
				//HiddenMarkovModel hmm = new HiddenMarkovModel(NUM_STATES, NUM_OPCODES);
				hmm.train(1000, 10);
				
				String saveFile = String.format("%d%s-%d.txt",numStates, families[familyNum], tupleSize);
				
				File file = new File (folderPath + saveFile);
				SaveHMM.save(hmm, file);
			}
		}
		//SaveHMM.save(hmm, file);
		 */
		
		Main main = new Main();
		//main.trainAll(6, 5, 15, 1000, 50);
		for(String family : families) {
		//String family = "cridex";
			ArrayList<ArrayList<Double>> scores = new ArrayList<ArrayList<Double>>();
			for(int i = 0; i < families.length; i++) {
				for(int j = 0; j < 42; j++) {	//11 is MAX_FAMILY_SIZE * (1 - TRAIN_PERCENT)
					scores.add(new ArrayList<Double>());
					scores.get(scores.size()-1).add((double) i);
				}
			}
			for(int i = 0; i < main.hyperParameters.size(); i++) {
				int[] hp = main.hyperParameters.get(i);
				main.testAll(hp[0], hp[1], hp[2], family, scores, true);
			}
			
			File saveScores = new File(folderPath + family + " scores.txt");
			try {
				FileWriter fw = new FileWriter(saveScores);
				for(ArrayList<Double> sampleScores : scores) {
					fw.write(sampleScores.toString());
					fw.write("\n");
				}
				
				fw.close();
			} catch(IOException e) {e.printStackTrace();}
		}
		
		
	}
	
	public void testAll(int numStates, int tupleSize, int numOpcodes, String family, ArrayList<ArrayList<Double>> scores, boolean train) {
		String tupleName = String.format("7 family tuples\\opcode Frequencies %d-tuple.txt", tupleSize);
		String filePath = folderPath + tupleName;
		Map<String, Integer> map = getMap(filePath, tupleSize, numOpcodes);
		HashMap<String, ArrayList<int[]>> familySamples = getAllSampleMap(map, tupleSize);
	
		String loadFile = String.format("Trained Models\\%d%s-%d.txt",numStates, family, tupleSize);
		//String loadFile = String.format("%d%s-%d.txt",numStates, family, tupleSize);
		
		File file = new File (folderPath + loadFile);
		HiddenMarkovModel hmm = SaveHMM.load(file);
		
		for(int i = 0; i < families.length; i++) {
			int start = train ? 0 : (int) (MAX_FAMILY_SIZE * TRAIN_PERCENT);
			int stop = train ? (int) (MAX_FAMILY_SIZE * TRAIN_PERCENT) : MAX_FAMILY_SIZE;
			List<int[]> samples = familySamples.get(families[i]).subList(start, stop);
			System.out.printf("%s model vs %s samples\n", family, families[i]);
			testSamples(hmm, samples, scores, i);
		}
		
	}
	
	public void trainAll(int numStates, int tupleSize, int numOpcodes, int iterations, int resets) {
		String tupleName = String.format("7 family tuples\\opcode Frequencies %d-tuple.txt", tupleSize);
		String filePath = folderPath + tupleName;
		Map<String, Integer> map = getMap(filePath, tupleSize, numOpcodes);
		HashMap<String, ArrayList<int[]>> familySamples = getAllSampleMap(map, tupleSize);
		
		for(String family : families) {
			HiddenMarkovModel hmm = new HiddenMarkovModel(numStates, map.keySet().size()+1, splitSamples(familySamples.get(family)).get(0));
			hmm.train(iterations, resets);
			
			String saveFile = String.format("Trained Models\\%d%s-%d.txt",numStates, family, tupleSize);
			
			File file = new File (folderPath + saveFile);
			SaveHMM.save(hmm, file);
		}
		
	}
	
	public HashMap<String, ArrayList<int[]>> getAllSampleMap(Map<String, Integer> map, int tupleSize){

		HashMap<String, ArrayList<int[]>> allSamples = new HashMap<String, ArrayList<int[]>>();
		for(String string : families) 
			allSamples.put(string, getSamples(folderPath+"samples\\"+string, map, tupleSize));
		
		return allSamples;
		
	}
	
	public double testSamples(HiddenMarkovModel hmm, List<int[]> samples, ArrayList<ArrayList<Double>> scores, int familyCount) {
		double averageScore = 0;
		for(int i = 0; i < samples.size(); i++) {
			double score = hmm.test(samples.get(i));
			averageScore += score;
			scores.get(i+samples.size()*familyCount).add(score);
			System.out.println(score);
		}
		
		return averageScore/samples.size();
	}
	
	/**	
	 * @param total_samples:	max number of samples to be used across training and testing
	 * @return	first element contains all training samples concatenated together, the rest of the elements are the testing set
	 */
	public ArrayList<int[]> splitSamples(ArrayList<int[]> samples){
		ArrayList<int[]> combined = new ArrayList<int[]>();
		int numTraining = (int) (MAX_FAMILY_SIZE * TRAIN_PERCENT);
		
		combined.add(combineSamples(samples.subList(0, numTraining)));
		
		List<int[]> testSet = samples.subList(numTraining, MAX_FAMILY_SIZE);
		for(int[] sample : testSet)
			combined.add(sample);
		
		return combined;
	}
	
	public ArrayList<int[]> getSamples(String folderPath, Map<String, Integer> map, int numTuple){
		File folder = new File(folderPath);
		ArrayList<int[]> samples = new ArrayList<int[]>();
		for(File file : folder.listFiles()) 
			samples.add(readFile(map, file, numTuple));
		return samples;
	}
	
	public Map<String, Integer> getMap(String path, int numTuple, int cutoff){

		HashMap<String, Integer> map = new HashMap<String, Integer>();
		
		try {
			File file = new File(path);
			FileReader fr = new FileReader(file);
			BufferedReader bf = new BufferedReader(fr);
			
			for(int i = 0; i < cutoff; i++) {
				String line = bf.readLine();
				map.put(line.split(":")[0], i);
			}
			
			bf.close();
			fr.close();
		} catch(IOException e) {e.printStackTrace();}
		return map;
	}
	
	public int[] readFile(Map<String, Integer> map, File file, int numTuple) {
		LinkedList<Integer> sequence = new LinkedList<Integer>();
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader bf = new BufferedReader(fr);
			
			LinkedList<String> tuple = new LinkedList<String>();
			for(int i = 0; i < numTuple; i++) {
				String line = bf.readLine();
				tuple.add(line.replace("[^a-z]", ""));
			}
			
			String line;
			int maxLines = 1000;
			int lineNumber = 0;
			while((line = bf.readLine()) != null && lineNumber < maxLines) {
				line = line.replace("[^a-z]", ""); //remove non alpha
				
				//collect n lines together
				String str = stackToString(tuple);
				if(map.containsKey(str))
					sequence.add(map.get(str));
				else 
					sequence.add(numTuple);
				
				//update stack for collection
				tuple.removeFirst();
				tuple.add(line);
				lineNumber++;
			}
			bf.close();
			fr.close();
			
			return listToArray(sequence);	//convert linked list to int[]
			
		} catch(IOException e) {e.printStackTrace();}
		
		return null;
	}
	
	private String stackToString(LinkedList<String> stack) {
		StringBuilder str = new StringBuilder();
		
		for(String string : stack)
			str.append(string);

		return str.toString();
	}
	
	private int[] listToArray(List<Integer> list) {
		return list.stream().mapToInt(i -> i).toArray();
	}
	
	private int[] combineSamples(List<int[]> samples) {
		ArrayList<Integer> collect = new ArrayList<Integer>();
		
		for(int[] sequence : samples)
			for(int opcode : sequence)
				collect.add(opcode);
		return listToArray(collect);
	}

}
