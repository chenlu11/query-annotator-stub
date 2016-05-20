package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.nodes.Entities;

import annotatorstub.utils.BingSearchHelper;
import annotatorstub.utils.CrawlerHelper;
import annotatorstub.utils.EmbeddingHelper;
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

public class newAnnotator implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = -1f;

	String[] words;
	ArrayList<String> mentions = null;
	ArrayList<Integer> mentions_start_index = null;
	ArrayList<Integer> mentions_size = null;
	ArrayList<int[]> mention_entity_pair = null;
	ArrayList<ArrayList<Double>> mention_entity_score = null;

	public HashSet<ScoredAnnotation> contextModel(String query) throws Exception {
		// split query to string array
		split_query(query);

		// construct mentions, mentions_size, & mention_entity_pair
		construct_m_e_pair();

		// calculate the cosine similarity between each mention-entity pair
		// the larger the better
		mention_entity_score = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < mentions.size(); i++) {
			String cur_mention = mentions.get(i);
			int cur_mention_size = mentions_size.get(i);
			int[] cur_entities = mention_entity_pair.get(i);
			ArrayList<Double> cur_scores = new ArrayList<Double>();
			for (int j = 0; j < cur_entities.length; j++) {
				// compute the distance of current <mention, entity> pair
				double cur_score = compute_query_entity_Similarity(query, cur_mention, cur_entities[j], cur_mention_size);
				cur_scores.add(cur_score);
				System.out.println(cur_mention + ", " + cur_entities[j] + ", "
						+ WikipediaApiInterface.api().getTitlebyId(cur_entities[j]) + ", " + cur_score);
			}
			mention_entity_score.add(cur_scores);
		}

		// for each mention, only keep the entity (id) with largest similarity
		int num_of_mentions = mentions.size();
		int[] selected_entity = new int[num_of_mentions];
		Pair[] mention_score_pairs = new Pair[num_of_mentions];
		for (int i = 0; i < num_of_mentions; i++) {
			ArrayList<Double> cur_scores = mention_entity_score.get(i);
			double minScore = Collections.max(cur_scores);
			int minIndex = cur_scores.indexOf(minScore);
			selected_entity[i] = mention_entity_pair.get(i)[minIndex];
			mention_score_pairs[i] = new Pair(i, minScore);
		}
		
		Arrays.sort(mention_score_pairs);
		System.out.println();
		for (int i = 0; i < num_of_mentions; i++) {
			int cur_mention_index = mention_score_pairs[i].index;
			String cur_mention = mentions.get(cur_mention_index);
			int wikipediaArticle = selected_entity[cur_mention_index];
			System.out.println("largest similarity match for mention: " + cur_mention + ", " + wikipediaArticle + ", "
					+ WikipediaApiInterface.api().getTitlebyId(wikipediaArticle) + ", " + mention_score_pairs[i].value);
		}
		System.out.println();
		
		HashSet<ScoredAnnotation> result = new HashSet<>();
		Set<String> existed_words = new HashSet<String>();
		for (int i = 0; i < num_of_mentions; i++) {
			int cur_mention_index = mention_score_pairs[i].index;
			String cur_mention = mentions.get(cur_mention_index);
			Boolean flag = true;
			for (String cur_word : cur_mention.split(" ")) {
				if (existed_words.contains(cur_word)) {
					flag = false;
					break;
				}
			}
			if (flag) {
				// meaning no other overlapping mentions have been selected
				for (String cur_word : cur_mention.split(" ")) {
					existed_words.add(cur_word);
				}
				int word_start = mentions_start_index.get(cur_mention_index);
				int word_length = mentions_size.get(cur_mention_index);
				int word_end = word_start + word_length - 1;
				int char_start = query.indexOf(words[word_start]);
				int char_end = query.indexOf(words[word_end]) + words[word_end].length();
				int wikipediaArticle = selected_entity[cur_mention_index];
				float score = (float) mention_score_pairs[i].value;
				result.add(new ScoredAnnotation(char_start, char_end - char_start, wikipediaArticle, score));
				System.out.println("find mention: " + cur_mention + "\twikipediaArticle:"
						+ WikipediaApiInterface.api().getTitlebyId(wikipediaArticle) + "(" + wikipediaArticle
						+ ")\tscore: " + score);
			}
		}
		return result;
	}

	public class Pair implements Comparable<Pair> {
		public final int index;
		public final double value;

		public Pair(int index, double minScore) {
			this.index = index;
			this.value = minScore;
		}

		@Override
		public int compareTo(Pair other) {
			// multiplied to -1 if you need descending sort order
			return -1 * Double.valueOf(this.value).compareTo(other.value);
		}
	}

	public void split_query(String query) {
		words = query.toLowerCase().replaceAll("[^A-Za-z0-9 ]", " ").split("\\s+");
		for (int i = 0; i < words.length; i++) {
			words[i] = words[i].trim();
		}
	}

	public void construct_m_e_pair() {
		mentions = new ArrayList<String>();
		mentions_start_index = new ArrayList<Integer>();
		mentions_size = new ArrayList<Integer>();
		mention_entity_pair = new ArrayList<int[]>();

		for (int i = 0; i < words.length; i++) {
			String cur_mention = null;
			for (int j = i; j < words.length; j++) {
				if (j == i) {
					cur_mention = words[j];
				} else {
					cur_mention = cur_mention + " " + words[j];
				}
				int[] cur_entities = WATRelatednessComputer.getLinks(cur_mention);
				if (cur_entities.length > 0) {
					mentions.add(cur_mention);
					mentions_start_index.add(i);
					mentions_size.add(j - i + 1);
					if (cur_entities.length <= 10) 
						mention_entity_pair.add(cur_entities);
					else {
						int[] cur_entities_10 = new int[10];
						for (int k = 0; k < 10; k++) {
							cur_entities_10[k] = cur_entities[k];
						}
						mention_entity_pair.add(cur_entities_10);
					}
				}
			}
		}
	}

	public double compute_query_entity_Similarity(String query, String mention, int entity_id, int mention_size) throws Exception {
//		String entity_title = WikipediaApiInterface.api().getTitlebyId(entity_id);
//		String query_context = BingSearchHelper.getBingSearchResult(query + " Wikipedia");
//		String entity_description = CrawlerHelper.getWikiPageDescription(entity_id);
//		double cos_similarity = EmbeddingHelper.getSimilarityValue(query_context, entity_description);
		double commonness = WATRelatednessComputer.getCommonness(mention, entity_id);
		double mention_probability = WATRelatednessComputer.getLp(mention);
//		return commonness + cos_similarity;
//		return commonness * mention_size;
//		return mention_probability;
		return commonness + mention_probability;
	}

	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
		HashSet<ScoredAnnotation> result = new HashSet<>();
		try {
			Utils.iter += 1;
			System.out.println("starting the query #" + Utils.iter);
			result = contextModel(text);
			System.out.println();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // just call Baseline
		return result;
	}

	// original solveSa2W
	public HashSet<ScoredAnnotation> solveSa2W_original(String text) throws AnnotationException {
		lastTime = System.currentTimeMillis();

		int start = 0;
		while (start < text.length() && !Character.isAlphabetic(text.charAt(start)))
			start++;
		int end = start;
		while (end < text.length() && Character.isAlphabetic(text.charAt(end)))
			end++;

		int wid;
		try {
			wid = WikipediaApiInterface.api().getIdByTitle(text.substring(start, end));
		} catch (IOException e) {
			throw new AnnotationException(e.getMessage());
		}

		HashSet<ScoredAnnotation> result = new HashSet<>();
		if (wid != -1)
			result.add(new ScoredAnnotation(start, end - start, wid, 0.1f));

		lastTime = System.currentTimeMillis() - lastTime;

		return result;
	}

	public String getName() {
		return "query annotator using context information";
	}

	public long getLastAnnotationTime() {
		return lastTime;
	}

	public HashSet<Tag> solveC2W(String text) throws AnnotationException {
		return ProblemReduction.A2WToC2W(solveA2W(text));
	}

	public HashSet<Annotation> solveD2W(String text, HashSet<Mention> mentions) throws AnnotationException {
		return ProblemReduction.Sa2WToD2W(solveSa2W(text), mentions, threshold);
	}

	public HashSet<Annotation> solveA2W(String text) throws AnnotationException {
		return ProblemReduction.Sa2WToA2W(solveSa2W(text), threshold);
	}

	public HashSet<ScoredTag> solveSc2W(String text) throws AnnotationException {
		return ProblemReduction.Sa2WToSc2W(solveSa2W(text));
	}

	public static void main(String[] args) throws IOException {
		String query = "luxury apartments san francisco area";
		newAnnotator ann = new newAnnotator();
		ann.solveSa2W(query);
	}

}
