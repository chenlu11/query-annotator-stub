package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import annotatorstub.utils.BingCorrectionHelper;
import annotatorstub.utils.PBoHModelHelper;
import annotatorstub.utils.PluralToSingularHelper;
import annotatorstub.utils.Utils;
import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Mention;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.data.ScoredTag;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

public class PBoHModelAnnotator implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = -1f;
	
	public static void main(String[] args) throws IOException {
		new PBoHModelAnnotator().solveDP("president 1974");
//		new PBoHModelAnnotator().solveDP("pine mountain state park lodge");
		WATRelatednessComputer.flush();
	}

	public HashSet<ScoredAnnotation> PBoHModel(String query) throws IOException {
		lastTime = System.currentTimeMillis();
		HashSet<ScoredAnnotation> result = solveDP(query);
		lastTime = System.currentTimeMillis() - lastTime;
		return result;
	}

	public HashSet<ScoredAnnotation> solveDP(String query) throws IOException {
		String[] words = query.toLowerCase().replaceAll("[^A-Za-z0-9 ]", " ").split("\\s+");		
		int l = words.length;
		double[][] store1 = new double[l][l];
		double[][] store2 = new double[l][l];
		int[][] entity = new int[l][l];
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < l; j++) {
				entity[i][j] = -1;
			}
		}
		// entity[i][j] = -1 means this mention has not been calculated
		// entity[i][j] = 0 means this mention has no corresponding entity
		int[][] previous = new int[l][l];
		ArrayList<ArrayList<Set<Integer>>> entities_set = new ArrayList<ArrayList<Set<Integer>>>();
		for (int i = 0; i < l; i++) {
			entities_set.add(new ArrayList<Set<Integer>>());
			for (int j = 0; j < l; j++)
				entities_set.get(i).add(new HashSet<Integer>());
		}		
		
		DP(store1, store2, entity, previous, entities_set, 0, l - 1, words);

		HashSet<ScoredAnnotation> result = new HashSet<>();
		WikipediaApiInterface api = WikipediaApiInterface.api();
		for (int i : entities_set.get(0).get(l - 1)) {
			System.out.print(i + " ");
		}
		System.out.println();		
		PrintResult(query, words, store1, entity, previous, api, 0, l - 1, result);
		api.flush();
		return result;
	}
	
	private void PrintResult(String query, String[] words, double[][] store1, int[][] entity, int[][] previous, WikipediaApiInterface api, int i, int j, HashSet<ScoredAnnotation> result) throws IOException {
		int k = previous[i][j];
		if (k == i) {
			int cur_entity = entity[i][j];
			if (cur_entity == 0)
				return;
			int char_start = query.toLowerCase().indexOf(words[i]);
			int char_end = query.toLowerCase().indexOf(words[j]) + words[j].length() - 1;
			if (char_end < char_start)
				return;
			float score = (float) store1[i][j];
			String cur_mention = constructSegmentation(words, i, j);
			System.out.println("find mention: " + cur_mention + "\twikipediaArticle:"
						+ api.getTitlebyId(cur_entity) + " (" + cur_entity
						+ ")\tscore: " + score + "\tchar: " + char_start + "-" + char_end);
			result.add(new ScoredAnnotation(char_start, char_end - char_start + 1, cur_entity, score));			
		}
		else {
			PrintResult(query, words, store1, entity, previous, api, i, k - 1, result);
			PrintResult(query, words, store1, entity, previous, api, k, j, result);
		}
	}

	public void DP(double[][] store1, double[][] store2, int[][] entity, int[][] previous, ArrayList<ArrayList<Set<Integer>>> entities_set, int start, int end, String[] words) {
		if (entity[start][end] != -1)
			return;

		String mention = constructSegmentation(words, start, end);
		Pair<Integer, Double> pair = PBoHModelHelper.getMaxScoreAndEntity(mention, words);
		int max_entity = pair.getFirst();
		double max_score = pair.getSecond();
		if (max_entity != 0)
			entities_set.get(start).get(end).add(max_entity);
		
		if (start == end) {
			System.out.println(mention + ", entity: " + max_entity + ", score: " + max_score + ", previous:" + start);	
			store1[start][end] = max_score;
			entity[start][end] = max_entity;
			previous[start][end] = start;
			return;
		}

		double score1 = max_score, score2 = 0;
		int prev = start;
		for (int seg = end - 1; seg >= start; seg--) {
			DP(store1, store2, entity, previous, entities_set, start, seg, words);
			DP(store1, store2, entity, previous, entities_set, seg + 1, end, words);
			double cur_score1, cur_score2, cur_score;
			if(store1[start][seg] != Double.NEGATIVE_INFINITY && store1[seg + 1][end] != Double.NEGATIVE_INFINITY) {
				cur_score1 = store1[start][seg] + store1[seg + 1][end];
				Set<Integer> entities_left = new HashSet<>(entities_set.get(start).get(seg));
				Set<Integer> entities_right = new HashSet<>(entities_set.get(seg + 1).get(end));
				int n1 = entities_left.size(), n2 = entities_right.size();
				cur_score2 = (store2[start][seg] * (n1 - 1) + store2[seg + 1][end] * (n2 - 1) 
						+ PBoHModelHelper.getCombinedSumOfLogRelatedness(entities_left, entities_right)) / (n1 + n2 - 1);
			}
			else if (store1[start][seg] != Double.NEGATIVE_INFINITY) {
				cur_score1 = store1[start][seg];
				cur_score2 = store2[start][seg];
			}
			else if (store1[seg + 1][end] != Double.NEGATIVE_INFINITY) {
				cur_score1 = store1[seg + 1][end];
				cur_score2 = store2[seg + 1][end];
			}
			else
				continue;
			cur_score = cur_score1 + cur_score2;
			if (max_score == Double.NEGATIVE_INFINITY || cur_score > max_score) { 
				max_score = cur_score;
				score1 = cur_score1;
				score2 = cur_score2;
				prev = seg + 1;
				entities_set.get(start).get(end).clear();
				entities_set.get(start).get(end).addAll(entities_set.get(start).get(seg));
				entities_set.get(start).get(end).addAll(entities_set.get(seg + 1).get(end));
			}				
		}		
		store1[start][end] = score1;
		store2[start][end] = score2;
		entity[start][end] = max_entity;
		previous[start][end] = prev;
		System.out.println(mention + ", entity: " + max_entity + ", score: " + max_score 
				+ ", score1: " + score1 + ", score2: " + score2 + ", previous:" + prev );
	}
	
	private String constructSegmentation(String[] queryTerms, int start, int end) {
		String ret = "";
		for (int i = start; i <= end; i++) {
			ret += queryTerms[i] + " ";
		}
		return ret.trim();
	}
	
	
	@Override
	public HashSet<Annotation> solveA2W(String text) throws AnnotationException {
		return ProblemReduction.Sa2WToA2W(solveSa2W(text), threshold);
	}

	@Override
	public HashSet<Tag> solveC2W(String text) throws AnnotationException {
		return ProblemReduction.A2WToC2W(solveA2W(text));
	}

	@Override
	public String getName() {
		return "PBoH Model annotator";
	}

	@Override
	public long getLastAnnotationTime() {
		return lastTime;
	}

	@Override
	public HashSet<Annotation> solveD2W(String text, HashSet<Mention> mentions) throws AnnotationException {
		return ProblemReduction.Sa2WToD2W(solveSa2W(text), mentions, threshold);
	}

	@Override
	public HashSet<ScoredTag> solveSc2W(String text) throws AnnotationException {
		return ProblemReduction.Sa2WToSc2W(solveSa2W(text));
	}

	@Override
	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
		HashSet<ScoredAnnotation> result = new HashSet<>();
		try {
			Utils.iter += 1;
			System.out.println("starting the query #" + Utils.iter);
			result = PBoHModel(text);
			System.out.println();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // just call Baseline
		return result;
	}


}
