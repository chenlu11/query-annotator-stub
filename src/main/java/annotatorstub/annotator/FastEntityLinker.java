package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import annotatorstub.utils.EmbeddingHelper;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.Pair;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

public class FastEntityLinker {

	public static void main(String[] args) {
		new FastEntityLinker().solveDP("lyme disease in georgia keep");
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
		// double[] minScore = new double[l + 1];
		// int[] previous = new int[l + 1];
		// int[] linkedEntity = new int[l + 1];
		// for (int i = 0; i < l + 1; i++) {
		// for (int j = 0; j < i; j++) {
		// String mention = constructSegmentation(p, j, i);
		// Pair<Integer, Double> ret = EmbeddingHelper.getHighestScore(mention,
		// p);
		// double score = phiFunction(minScore[j], ret.second);
		// if(minScore[i] == 0) {
		// minScore[i] = score;
		// }
		// if (score < minScore[i]) {
		// minScore[i] = score;
		// previous[i] = j;
		// linkedEntity[i] = ret.first;
		// }
		// }
		// }
		// for (int i = 0; i < l; i++) {
		// System.out.print(minScore[i] + " ");
		// }
		// System.out.println();
		// for (int i = 0; i < l; i++) {
		// System.out.print(previous[i] + " ");
		// }
		// System.out.println();
		// for (int i = 0; i < l; i++) {
		// System.out.print(linkedEntity[i] + " ");
		// }
		// System.out.println();

	}

	/*
	 * solve dp: basecase, i == j, compute all the diagonal elements.
	 */
	public void solveDP(String query) {
		String[] p = query.replaceAll("[^A-Za-z0-9 ]", " ").split("\\s+");
		int l = p.length;
		double[][] store = new double[l][l];
		int[][] entity = new int[l][l];
		int[] previous = new int[l];
		dp(store, entity, previous, 0, l - 1, p);

		int i = l - 1;
		while (i >= 0) {
			int prev = previous[i];
			try {
				System.out.println(constructSegmentation(p, prev, i) + ", " + entity[prev][i] + ", "
						+ WikipediaApiInterface.api().getTitlebyId(entity[prev][i]) + ", " + store[prev][i]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i = prev - 1;
		}
		for(int j = 0; j < previous.length; j++) {
			System.out.print(previous[j] + " ");
		}

	}

	public void dp(double[][] store, int[][] entity, int[] previous, int start, int end, String[] words) {
		Pair<Integer, Double> pair = EmbeddingHelper.getHighestScore(constructSegmentation(words, start, end), words);
		
		double minScore = pair.second;
		int minEntity = pair.first;
		if(minEntity == 0) {
			previous[end] = end;
		}else {
			previous[end] = start;
		}
		for (int i = 0; start + i + 1 <= end; i++) {
			if (store[start][start + i] == 0) {
				dp(store, entity, previous, start, start + i, words);
			}
			if (store[start + i + 1][end] == 0) {
				dp(store, entity, previous, start + i + 1, end, words);
			}
			
			if (store[start][start + i] != EmbeddingHelper.notLinkedScore
					&& store[start + i + 1][end] != EmbeddingHelper.notLinkedScore) {
				if (minScore > store[start][start + i] + store[start + i + 1][end]) {
					minScore = store[start][start + i] + store[start + i + 1][end];
					minEntity = 0;
					if(entity[start + i + 1][end] != 0 && entity[start][start + i] != 0) {
						previous[end] = start + i + 1;
					}
				}
			} else {
				if (minScore > Math.min(store[start][start + i], store[start + i + 1][end])) {
					minScore = Math.min(store[start][start + i], store[start + i + 1][end]);
					minEntity = 0;
					previous[end] = end;
				}
			}
		}
		store[start][end] = minScore;
		entity[start][end] = minEntity;
		System.out.println("Update i = " + start + " j = " + end + " val = " + minScore + "  mention: "
				+ constructSegmentation(words, start, end) + " entity: " + minEntity + " end index: " + end +" previous index: " +previous[end]);

	}

	private String constructSegmentation(String[] queryTerms, int start, int end) {
		String ret = "";
		for (int i = start; i <= end; i++) {
			ret += queryTerms[i].trim() + " ";
		}
		return ret.trim();
	}


}
