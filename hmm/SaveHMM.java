package hmm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;

public class SaveHMM {
	
	public static void main(String[] args) {
	}
	
	public static HiddenMarkovModel load(File file) {
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			double[] initial = stringToArray(br.readLine());
			int numStates = initial.length;
			double[][] transition = new double[numStates][];
			double[][] observation = new double[numStates][];
			for(int i = 0; i < numStates; i++)
				transition[i] = stringToArray(br.readLine());
			for(int i = 0; i < numStates; i++)
				observation[i] = stringToArray(br.readLine());
			
			br.close();
			fr.close();
			HiddenMarkovModel hmm = new HiddenMarkovModel(numStates, observation[0].length);
			hmm.load(initial, transition, observation);
			return hmm;
			
		} catch(IOException e) {e.printStackTrace();}
		return null;
	}
	
	public static void save(HiddenMarkovModel hmm, File file) {
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(arrayToString(hmm.getInitial()));
			fw.write("\n");
			fw.write(matrixToString(hmm.getTransition()));
			fw.write(matrixToString(hmm.getObservation()));
			
			fw.close();
		} catch(IOException e) {e.printStackTrace();}
	}
	
	public static double[] stringToArray(String string) {
		String[] split = string.split(", ");
		double[] array = new double[split.length];
		
		for(int i = 0; i < array.length; i++)
			array[i] = Double.valueOf(split[i]);
		
		return array;
	}
	
	
	public static String arrayToString(double[] array) {
		DecimalFormat df = new DecimalFormat("#.############");
		StringBuilder string = new StringBuilder();
		
		for(int i = 0; i < array.length-1; i++) {
			string.append(df.format(array[i]));
			string.append(", ");
		}

		string.append(df.format(array[array.length -1]));
		return string.toString();
	}
	
	public static String matrixToString(double[][] matrix) {
		StringBuilder string = new StringBuilder();
		
		for(double[] array : matrix) {
			string.append(arrayToString(array));
			string.append("\n");
		}
		
		return string.toString();
	}

}
