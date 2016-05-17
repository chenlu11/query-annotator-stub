package annotatorstub.annotator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import annotatorstub.utils.EmbeddingHelper;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.Pair;

public class FastEntityLinker {

	public static void main(String[] args) {
		new FastEntityLinker().fastEntityLinkerModel("buying land and arizona");
	}

	/**
	 * Out new model
	 * 
	 * @param query
	 * @return
	 */
	public void fastEntityLinkerModel(String query) {
		String[] p = query.replaceAll("[^A-Za-z0-9 ]", " ").split("\\s+");
		int l = p.length;
		double[] maxScore = new double[l];
		int[] previous = new int[l];
		int[] linkedEntity = new int[l];
		for (int i = 0; i < l; i++) {
			for (int j = 0; j <= i; j++) {
				String mention = constructSegmentation(p, j, i);
				Pair<Integer, Double> ret = EmbeddingHelper.getHighestScore(mention, p);
				double score = phiFunction(maxScore[j], ret.second);
				if (score > maxScore[i]) {
					maxScore[i] = score;
					previous[i] = j;
					linkedEntity[i] = ret.first;
				}
			}
		}
		for(int i = 0; i < l; i ++) {
			System.out.print(maxScore[i] + " ");
		}
		System.out.println();
		for(int i = 0; i < l; i ++) {
			System.out.print(previous[i] + " ");
		}
		System.out.println();
		for(int i = 0; i < l; i ++) {
			System.out.print(linkedEntity[i] + " ");
		}
		System.out.println();
		
	}
	
	private void generateResult(double[] maxScore, int[] previous, int[] entity, String[] terms) {
		List<String> mentions = new ArrayList<String> ();
		List<Integer> entities = new ArrayList<Integer> ();
		List<Double> score = new ArrayList<Double> ();
		int index = entity.length - 1;
		while(index != 0) {
			int start = previous[index];
		}
	}
	
	private String constructSegmentation(String[] queryTerms, int start, int end) {
		String ret = "";
		for(int i = start; i <= end; i ++) {
			ret += queryTerms[i].trim();
		}
		return ret;
	}
	private double phiFunction(double max, double highestScore) {
		return max + highestScore;
	}

}
