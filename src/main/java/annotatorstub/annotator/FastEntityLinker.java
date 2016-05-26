package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import annotatorstub.utils.BingCorrectionHelper;
import annotatorstub.utils.EmbeddingHelper;
import annotatorstub.utils.PluralToSingularHelper;
import annotatorstub.utils.Utils;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Mention;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.data.ScoredTag;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

public class FastEntityLinker implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = -1f;

	public static void main(String[] args) throws IOException {
		new FastEntityLinker().solveDP("kathy alfred;atytorney at law");
	}

	public HashSet<ScoredAnnotation> fastEntityLinkerModel(String query) throws IOException {
		lastTime = System.currentTimeMillis();
		HashSet<ScoredAnnotation> result = solveDP(query);
		lastTime = System.currentTimeMillis() - lastTime;
		return result;
	}
	
	private static final Comparator<ScoredAnnotation> comp = new Comparator<ScoredAnnotation> () {
		@Override
		public int compare(ScoredAnnotation a, ScoredAnnotation b) {
			return (int)(b.getScore() - a.getScore());
		}
	};

	public HashSet<ScoredAnnotation> solveDP(String query) throws IOException {
//		 String[] words = query.toLowerCase().replaceAll("[^A-Za-z0-9 ]", " ").split("\\s+");
		Pair<String, HashMap<String, String>> ret = BingCorrectionHelper.correction(query);
		ret = PluralToSingularHelper.getSingular(ret.getFirst(), ret.getSecond());
		String newQuery = ret.getFirst();
		System.out.println("newQuery is : " + newQuery);
		String[] words = ret.getFirst().split("\\W+");
		HashMap<String, String> map = ret.getSecond();
		int l = words.length;
		double[][] store = new double[l][l];
		int[][] entity = new int[l][l];
		int[] previous = new int[l];
		dp(store, entity, previous, 0, l - 1, words, newQuery);

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
			int char_start;
			int char_end;
			if (map.containsKey(words[word_start])) {
				char_start = query.indexOf(map.get(words[word_start]));
			} else {
				char_start = query.indexOf(words[word_start]);
			}
			if (map.containsKey(words[word_end])) {
				char_end = query.indexOf(map.get(words[word_end])) + map.get(words[word_end]).length();
			} else {
				char_end = query.indexOf(words[word_end]) + words[word_end].length();
			}
			float score = (float) store[word_start][word_end];
			String cur_mention = constructSegmentation(words, word_start, word_end);
			System.out.println("find mention: " + cur_mention + "\twikipediaArticle:" + api.getTitlebyId(cur_entity)
					+ "(" + cur_entity + ")\tscore: " + score);
			result.add(new ScoredAnnotation(char_start, char_end - char_start, cur_entity, score));

			word_end = word_start - 1;
		}
		// only take the first 3 top scored annotations
		if(result.size() > 3) {
			List<ScoredAnnotation> filter = new ArrayList<>();
			filter.addAll(result);
			filter.sort(comp);
			result.clear();
			for(int i = 0; i < 5; i ++) {
				result.add(filter.get(i));
			}
		}
		return result;
	}

	public void dp(double[][] store, int[][] entity, int[] previous, int start, int end,String[] words, String query) {
		Pair<Integer, Double> pair = EmbeddingHelper.getHighestScore(constructSegmentation(words, start, end), query);

		double minScore = pair.getSecond();
		int minEntity = pair.getFirst();
		if (minEntity == 0) {
			previous[end] = end;
		} else {
			previous[end] = start;
		}
		for (int i = 0; start + i + 1 <= end; i++) {
			if (store[start][start + i] == 0) {
				dp(store, entity, previous, start, start + i, words, query);
			}
			if (store[start + i + 1][end] == 0) {
				dp(store, entity, previous, start + i + 1, end, words, query);
			}

			if (store[start][start + i] != EmbeddingHelper.notLinkedScore
					&& store[start + i + 1][end] != EmbeddingHelper.notLinkedScore) {
				if (minScore > store[start][start + i] + store[start + i + 1][end]) {
					minScore = store[start][start + i] + store[start + i + 1][end];
					minEntity = 0;
					if (entity[start + i + 1][end] != 0 && entity[start][start + i] != 0) {
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
				+ constructSegmentation(words, start, end) + " entity: " + minEntity + " end index: " + end
				+ " previous index: " + previous[end]);

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
			result = fastEntityLinkerModel(text);
			System.out.println();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // just call Baseline
		return result;
	}

}
