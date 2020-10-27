package hmm;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

public class HiddenMarkovModel {
	
	private int numStates;
	private int numSymbols;
	private int[] sequence;
	private double[] initial;
	private double[][] alpha;		//recursive stores of probabilities for a function f-time(state)
	private double[][] beta;		//
	private double[][] gamma;		//
	private double[][][] digamma;	//recursive store of probabilities for function f-time(state_1, state_2)
	
	private double[][] transition;	//transition matrix
	private double[][] observation;	//observation matrix (double hash map used for String indexing
	
	/**
	 * @precondition obsSet.length must match number of unique strings in sequence
	 * @param numStates
	 * @param obsSet
	 * @param sequence
	 */
	public HiddenMarkovModel(int numStates, int numSymbols, int[] sequence) {
		this.numStates = numStates;
		this.numSymbols = numSymbols;
		transition = new double[numStates][numStates];
		observation = new double[numStates][numSymbols]; //new Matrix(numHiddenStates, numObserverationVariables);
		initial = new double[numStates];
							//time 				state
		alpha = new double[sequence.length][numStates];
		beta = new double[sequence.length][numStates];
		gamma = new double[sequence.length][numStates];
		digamma = new double[sequence.length][numStates][numStates];
		
		this.sequence = sequence;
		initializeValues();

	}
	
	public HiddenMarkovModel(int numStates, int numSymbols) {
		this.numStates = numStates;
		this.numSymbols = numSymbols;
		transition = new double[numStates][numStates];
		observation = new double[numStates][numSymbols]; //new Matrix(numHiddenStates, numObserverationVariables);
		initial = new double[numStates];
	}
	
	public void load(double[] initial, double[][] transition, double[][] observation) {
		this.initial = initial;
		this.transition = transition;
		this.observation = observation;
		this.numStates = initial.length;
		this.numSymbols = observation[0].length;
	}
	
	public double[] getInitial() {
		return initial;
	}
	public double[][] getTransition(){
		return transition;
	}
	public double[][] getObservation(){
		return observation;
	}
	
	public double test(int[] sequence) {
		double[][] alpha = new double[sequence.length][numStates];
		double score = logProb(forward(sequence, alpha));
		if(Double.isNaN(score))
			System.out.println(Arrays.toString(sequence));
		return score;
	}
	
	public double train(int maxIterations, int numResets) {
		double oldLogProb = trainHelper(maxIterations);
		double[][] transition = copyMatrix(this.transition);
		double[][] observation = copyMatrix(this.observation);
		
		for(int i = 0; i < numResets; i++) {
			initializeValues();
			double newLogProb = trainHelper(maxIterations);
			if(newLogProb > oldLogProb) {	//if probability increases
				oldLogProb = newLogProb;
				transition = copyMatrix(this.transition);
				observation = copyMatrix(this.observation);
			}
		}
		
		this.transition = transition;
		this.observation = observation;
		return oldLogProb;
	}
	
	private double[][] copyMatrix(double[][] matrix){
		double[][] clone = new double[matrix.length][matrix[0].length];
		for(int i = 0; i < matrix.length; i++)
			for(int j = 0; j <matrix[0].length; j++)
				clone[i][j] = matrix[i][j];
		return clone;
	}
	
	private double trainHelper(int maxIterations) {
		double oldLogProb = Double.NEGATIVE_INFINITY;
		double delta = Double.POSITIVE_INFINITY;
		for(int i = 0; i < maxIterations; i++) {
			double[] scalars = forward(sequence);
			
			backward(sequence,  scalars);
			gammaDigamma();
			reestimate();
			
			double logProb = logProb(scalars);
			delta = logProb - oldLogProb;
			//System.out.println(i + "\t"+ oldLogProb);
			if(delta < 0 || (i > 50 && Math.abs(delta) < 0.0001))  //logProb <= oldLogProb
				break;
			oldLogProb = logProb;
		}
		return oldLogProb;
	}
	
	/**
	 * printObservationMatrix prints the the transpose of the Observation Matrix
	 */
	public void printObservationMatrix() {
		DecimalFormat df = new DecimalFormat("#.####");
		int a = (int) 'a';
		System.out.print("l\t");
		for(int state = 0; state < numStates; state++)
			System.out.printf("%d\t", state+1);
		System.out.println();
		for(int symbol = 0; symbol < numSymbols; symbol++) {
			//System.out.print((char)((int) 'a' + observ)+"\t");
			System.out.print((char)(a + symbol) + "\t");
			for(int state = 0; state < numStates; state++)
				System.out.printf("%s\t", df.format(observation[state][symbol]));
			System.out.println();
		}
	}


	
	public double logProb(double[] scalars) {
		double logProb = 0;
		for(int time = 0; time < scalars.length; time++)
			logProb += Math.log(scalars[time]);	//scalars are sum of alphas across states so are reused here (logProb can also be calculated with double for loop)
		return -logProb/scalars.length;
	}
	
	public double[] forward(int[] sequence) {
		return forward(sequence, this.alpha);
	}
	
	/**
	 * compute alpha pass
	 * @return scalar values to be used in backward pass
	 */
	public double[] forward(int[] sequence, double[][] alpha) {
		//scaling constant to avoid underflow
		double[] scalars = new double[sequence.length];
		scalars[0] = 0;
		
		// compute initial alpha of each state
		for(int state = 0; state < numStates; state++) {
			alpha[0][state] = initial[state] * observation[state][sequence[0]];
			scalars[0] += alpha[0][state];
		}
		
		scalars[0] = 1.0/scalars[0];
		// scale each initial alpha
		for(int state = 0; state < numStates; state++)
			alpha[0][state] *= scalars[0];
		
		// compute each alpha at time > 0 for each state
		for(int time = 1; time < sequence.length; time++) {
			scalars[time] = 0;
			for(int state = 0; state < numStates; state++) {
				//System.out.println(sequence[time]);
				alpha[time][state] = forward_helper(state, time, alpha) * observation[state][sequence[time]];
				scalars[time] += alpha[time][state];
			}
			// scale each alpha at time > 0 for each state
			scalars[time] = 1.0/scalars[time];
			for(int state = 0; state < numStates; state++)
				alpha[time][state] *= scalars[time];
			}
		return scalars;
	}
	
	/**
	 * compute beta pass scaling each value with scalars from alpha pass
	 * @param scalars
	 */
	public void backward(int[] sequence, double[] scalars) {
		//scalars for last beta
		for(int state = 0; state < numStates; state++)
			beta[sequence.length-1][state] = scalars[sequence.length-1];
		
		for(int time  = sequence.length - 2; time >= 0; time--) 
			for(int state = 0; state < numStates; state++) {
				beta[time][state] = backward_helper(state, time);
				beta[time][state] *= scalars[time];
			}
	}
	
	private void initializeValues() {
		for(int state = 0; state < numStates; state++) {
			transition[state] = uniformDist(numStates); 
			observation[state] = uniformDist(numSymbols);
		}
		initial = uniformDist(numStates);
	}
	
	/**
	 * uniformDist creates a stochastic array of with a near uniform distribution
	 * @param num
	 * @return
	 */
	private static double[] uniformDist(int num) {
		Random rng = new Random();
		double[] randNums = new double[num];
		double total = 0;
		for(int i = 0; i < num; i++) {
			randNums[i] = rng.nextInt(10000) + 100000;
			total += randNums[i];
		}
		for(int i = 0; i < num; i++) {
			randNums[i] = (randNums[i]/ total);
		}
		
		
		return randNums;
	}
	
	
	
	/**
	 * sum across column $state of the observation matrix * previous alpha term
	 * @param state
	 * @param time
	 * @return
	 */
	private double forward_helper(int state, int time, double[][] alpha) {
		double sum = 0;
		alpha[time][state] = 0;
		for(int i = 0; i < numStates; i++)
			sum += alpha[time-1][i] * transition[i][state];
		return sum;
	}
	
	
	private double backward_helper(int state, int time){
		double sum = 0;
		beta[time][state] = 0;
		for(int i = 0; i < numStates; i++)
			// start at $state then transition to i and observe at next time * previous beta term (beta iterates backwards)
			sum += transition[state][i] * observation[i][sequence[time+1]] * beta[time+1][i];
		return sum;
	}
	private void gammaDigamma() {
		// alpha and beta already scaled so don't need to scale digamma now
		for(int time = 0; time < sequence.length-1; time++) {
			double denom = 0;
			for(int state_1 = 0; state_1 < numStates; state_1++) {/*
				gamma[time][state_1] = 0;
				for(int state_2 = 0; state_2 < numStates; state_2++) {
					digamma[time][state_1][state_2] = alpha[time][state_1] * transition[state_1][state_2] * observation[state_2][sequence[time+1]] * beta[time+1][state_2];
					gamma[time][state_1] += digamma[time][state_1][state_2];
				}*/
				for(int state_2 = 0; state_2 < numStates; state_2++)
					denom += alpha[time][state_1]*transition[state_1][state_2]*observation[state_2][sequence[time+1]]*beta[time+1][state_2];
			}
			for(int state_1 = 0; state_1 < numStates; state_1++) {
				gamma[time][state_1] = 0;
				for(int state_2 = 0; state_2 < numStates; state_2++) {
					digamma[time][state_1][state_2] = (alpha[time][state_1]*transition[state_1][state_2]*observation[state_2][sequence[time+1]]*beta[time+1][state_2])/denom;
					gamma[time][state_1] += digamma[time][state_1][state_2];
				}
			}
		}
		
		//special case for gamma of each state at last time
		double denom = 0;
		for(int state = 0; state < numStates; state++)
			denom += alpha[sequence.length-1][state];
		for(int state = 0; state < numStates; state++)
			gamma[sequence.length-1][state] = alpha[sequence.length-1][state] / denom;
	}
	
	private void reestimate() {
		//re-estimate initial
		for(int state = 0; state < numStates; state++)
			initial[state] = gamma[0][state];
		

		for(int i = 0; i < numStates; i++) {
			for(int j = 0; j < numStates; j++) {
				double numer = 0;
				double denom = 0;
				for(int time = 0; time < sequence.length-1; time++) {
					numer += digamma[time][i][j];		//"frequency" of i transitioning to j
					denom += gamma[time][i];			//"frequency" of i occurring
				}
				transition[i][j] = numer/denom;			//occurrence of i to j / occurrence of just i
			}
		}
		
		//re-estimate observation matrix;
		for(int state = 0; state < numStates; state++) {
			for(int symbol = 0; symbol < numSymbols; symbol++) {
				double numer = 0;
				double denom = 0;
				for(int time = 0; time < sequence.length; time++) {
					if(sequence[time] == symbol)
						numer += gamma[time][state];		//"frequency" of each symbol occurring in the sequence 
					denom += gamma[time][state];			//"frequency" of state occurring
				}
															//occurrence of symbol observed given $state / occurrence of just $state
				observation[state][symbol] = numer/denom;
			}
		}
	}

}

