package Classifier.SVM;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.functions.SMO;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.lazy.IBk;
import weka.core.FastVector;
import weka.core.Instances;

public class WekaTest {
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;
 
		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}
 
		return inputReader;
	}
	public static Evaluation classify(Classifier model,Instances trainingSet, Instances testingSet) 
			throws Exception {
		Evaluation evaluation = new Evaluation(trainingSet);
 
		model.buildClassifier(trainingSet);
		evaluation.evaluateModel(model, testingSet);
 
		return evaluation;
	}
	public static double calculateAccuracy(FastVector predictions) {
		double correct = 0;
 
		for (int i = 0; i < predictions.size(); i++) {
			NominalPrediction np = (NominalPrediction) predictions.elementAt(i);
			if (np.predicted() == np.actual()) {
				correct++;
			}
		}
 
		return 100 * correct / predictions.size();
	}
	public static Instances[][] crossValidationSplit(Instances data, int numberOfFolds) {
		Instances[][] split = new Instances[2][numberOfFolds];
 
		for (int i = 0; i < numberOfFolds; i++) {
			split[0][i] = data.trainCV(numberOfFolds, i);
			split[1][i] = data.testCV(numberOfFolds, i);
		}
 
		return split;
	}
	//MAIN 
	public static void main(String[] args) throws Exception {
		BufferedReader trainFile = readDataFile("wekatrain.arff");

		BufferedReader testfile = readDataFile("wekatest.arff");

		Instances train = new Instances(trainFile);
		Instances data = new Instances(testfile);

		data.setClassIndex(0);
		train.setClassIndex(0);
 
		// Do 10-split cross validation
		Instances[][] splitsfortrain = crossValidationSplit(train,10);
		Instances[][] split = crossValidationSplit(data, 10);

 
		// Separate split into training and testing arrays
		Instances[] trainingSplits = splitsfortrain[0];
		Instances[] testingSplits = split[1];
 
		// Use a set of classifiers
		Classifier[] models = { 
				new J48(), // a decision tree
				new RandomForest(),//RandomForest
				new IBk() //K-NN
		};
 
		// Run for each model
		for (int j = 0; j < models.length; j++) {
 
			// Collect every group of predictions for current model in a FastVector
			FastVector predictions = new FastVector();
 
			// For each training-testing split pair, train and test the classifier
			for (int i = 0; i < trainingSplits.length; i++) {
				Evaluation validation = classify(models[j], trainingSplits[i], testingSplits[i]);

				predictions.appendElements(validation.predictions());
 				validation.weightedFMeasure();
				// Uncomment to see the summary for each training-testing pair.
				System.out.println(models[j].toString());
				System.out.println("FMeasure: "+validation.weightedFMeasure()*100+"%");
				System.out.println("Precision: "+validation.weightedPrecision()*100+"%");
				System.out.println("Recall: "+validation.weightedRecall()*100+"%");
			}
 
			// Calculate overall accuracy of current classifier on all splits
			double accuracy = calculateAccuracy(predictions);
 
			// Print current classifier's name and accuracy in a complicated,
			// but nice-looking way.
			System.out.println("Accuracy of " + models[j].getClass().getSimpleName() + ": "
					+ String.format("%.2f%%", accuracy)
					+ "\n---------------------------------");
			System.out.println();
		}
	}
}
