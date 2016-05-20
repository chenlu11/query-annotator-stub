package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import annotatorstub.utils.PBoHModelHelper;
import annotatorstub.utils.Utils;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Mention;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.data.ScoredTag;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.Pair;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

public class PBoHModelAnnotator implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = -1f;
	
	public static void main(String[] args) throws IOException {
		new PBoHModelAnnotator().solveDP("luxury apartments san francisco area");
	}

	public HashSet<ScoredAnnotation> PBoHModel(String query) throws IOException {
		lastTime = System.currentTimeMillis();
		HashSet<ScoredAnnotation> result = solveDP(query);
		lastTime = System.currentTimeMillis() - lastTime;
		return result;
	}

	/*
	 * solve dp: basecase, i == j, compute all the diagonal elements.
	 */
	public HashSet<ScoredAnnotation> solveDP(String query) throws IOException {
		String[] words = query.toLowerCase().replaceAll("[^A-Za-z0-9 ]", " ").split("\\s+");
		int l = words.length;
		double[][] store = new double[l][l];
		int[][] entity = new int[l][l];
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < l; j++) {
				entity[i][j] = -1;
			}
		}
		// entity[i][j] = -1 means this mention has not been calculated
		// entity[i][j] = 0 means this mention has no corresponding entity
		int[] previous = new int[l];
		
		DP(store, entity, previous, 0, l - 1, words);
				
		HashSet<ScoredAnnotation> result = new HashSet<>();
		WikipediaApiInterface api = WikipediaApiInterface.api();
		System.out.println();

		int word_end = l - 1;
		while (word_end >= 0) {
			int word_start = previous[word_end];
			int cur_entity = entity[word_start][word_end];
			if (cur_entity == 0) {
				word_end = word_start - 1;
				continue;
			}
			int char_start = query.indexOf(words[word_start]);
			int char_end = query.indexOf(words[word_end]) + words[word_end].length();						
			float score = (float) store[word_start][word_end];
			String cur_mention = constructSegmentation(words, word_start, word_end);
			System.out.println("find mention: " + cur_mention + "\twikipediaArticle:"
						+ api.getTitlebyId(cur_entity) + "(" + cur_entity
						+ ")\tscore: " + score);
			result.add(new ScoredAnnotation(char_start, char_end - char_start, cur_entity, score));

			word_end = word_start - 1;
		}
		return result;
	}
	
	public void DP(double[][] store, int[][] entity, int[] previous, int start, int end, String[] words) {
		if (entity[start][end] != -1)
			return;
		
		String mention = constructSegmentation(words, start, end);
		Pair<Integer, Double> pair = PBoHModelHelper.getMaxScoreAndEntity(mention, words);
		int max_entity = pair.first;
		double max_score = pair.second * (double)(end - start + 1);
		int prev = start;
		
		if (start == end) {
			System.out.println(mention + ", entity: " + max_entity + ", score: " + max_score + ", previous:" + start );	
			store[start][end] = max_score;
			entity[start][end] = max_entity;
			if (start == 0)
				previous[end] = prev;
			return;
		}
		
		for (int seg = start; seg < end; seg++) {
			DP(store, entity, previous, start, seg, words);
			DP(store, entity, previous, seg + 1, end, words);
			double cur_score = store[start][seg] + store[seg + 1][end];
			if (cur_score >= max_score) {
				max_score = cur_score;			
				prev = seg + 1;
			}				
		}		
		store[start][end] = max_score;
		entity[start][end] = max_entity;
		if (start == 0)
			previous[end] = prev;
		System.out.println(mention + ", entity: " + max_entity + ", score: " + max_score + ", previous:" + previous[end] );
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
		return "fast entity linker annotator";
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
