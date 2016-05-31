package annotatorstub.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import it.unimi.dsi.fastutil.doubles.Double2DoubleLinkedOpenHashMap;


public class PBoHModelHelper {
	final static int dim = 300;
	final static String dict_path = "deps.words";
	static final HashMap<String, double[]> dict = loadEmbeddings(dict_path);
	
	public static Pair<Integer, Double> getMaxScoreAndEntity(String mention, String[] words) {
		int entities[] = WATRelatednessComputer.getLinks(mention);

		if (entities.length == 0) {
			return new Pair<Integer, Double>(0, Double.NEGATIVE_INFINITY);  // entity_id = 0 means this mention has no corresponding entity
		}
		
		double max_score = Double.NEGATIVE_INFINITY;
		int max_entity = -1;
		int l = (entities.length > 5) ? 5 : entities.length;
		for (int i = 0; i < l; i++) {
			int entity_id = entities[i];
			double log_commonness = Math.log(WATRelatednessComputer.getCommonness(mention, entity_id));
//			double log_prob_query_given_entity = getLogProbabilityOfQueryGivenEntity(words, entity_id);
//			if (log_prob_query_given_entity == 0)
//				continue;
			double score = log_commonness;
//			double score = log_prob_query_given_entity;
//			double score = log_commonness + 0.075 * log_prob_query_given_entity;
//			System.out.println("Score1: " + mention + "\t" + entity_id + "\t" + log_commonness + "\t" + log_prob_query_given_entity + "\t" + score);
			if (max_score == Double.NEGATIVE_INFINITY || score > max_score) {
				max_score = score;
				max_entity = entities[i];
			}
		}
		return new Pair<Integer, Double>(max_entity, max_score);
	}
	
	// Compute the sum of all logP(t_i | e)
	private static double getLogProbabilityOfQueryGivenEntity(String[] terms, int entityId) {
		String entity = CrawlerHelper.getWikiPageDescription(entityId);
		if (entity == null) {
			return 0;
		}
		double[] entityEbd = new double[dim];
		entityEbd = computeDocEmbedding(entity);
		if(entityEbd == null) {
			return 0;
		}
		double ret = 0;
		for (String term : terms) {
			double[] termEbd = dict.get(term);
			if (termEbd != null) {
				ret += Math.log(getProbabilityOfTermGivenEntity(termEbd, entityEbd));
			}
		}
		return ret;
	}
	
	// Compute p(t_i | e)
	private static double getProbabilityOfTermGivenEntity(double[] termEbd, double[] entityEbd) {
		return 1 / (1 + Math.exp(- innerProduct(termEbd, entityEbd)));
	}

	/**
	 * Load pre-trained word embeddings. reference:
	 * https://www.cs.bgu.ac.il/~yoavg/publications/nips2014pmi.pdf pre-trained
	 * data: https://github.com/3Top/word2vec-api Wikipedia dependency dataset
	 * 
	 * @param path
	 * @throws IOException
	 */
	private static HashMap<String, double[]> loadEmbeddings(String path) {
		System.out.println("----------------------Start loading word embeddings--------------------\n");
		HashMap<String, double[]> temp = new HashMap<String, double[]>();
		File file = new File(path);
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String str = br.readLine();
			while (str != null && !str.equals("")) {
				String[] slices = str.split(" ", 2);
				String word = slices[0];
				String[] embedding_strs = slices[1].split(" ");
				assert embedding_strs.length == dim;
				double[] embedding = new double[dim];
				for (int i = 0; i < dim; i++) {
					embedding[i] = Double.parseDouble(embedding_strs[i]);
				}
				temp.put(word, embedding);
				str = br.readLine();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("----------------------Finish loading word embeddings--------------------\n\n");
		return temp;
	}

	// Compute the Embedding of a String, which contains multiple words by 
	// taking average on each dimension of word embedding.
	private static double[] computeDocEmbedding(String str) {
		if(str == null) {
			return null;
		}
		String[] doc = str.trim().split("\\W+");
		int numOfWords = 0;
		double[] res = new double[dim];
		for (int i = 0; i < dim; i++)
			res[i] = 0;
		for (String word : doc) {
			if (dict.containsKey(word)) {
				numOfWords += 1;
				double[] word_ebd = dict.get(word);
				assert word_ebd.length == dim;
				for (int i = 0; i < dim; i++) {
					res[i] += word_ebd[i];
				}
			}
		}
		if (numOfWords == 0)
			return null;
		for (int i = 0; i < dim; i++)
			res[i] = res[i] / (double) numOfWords;
		return res;
	}

	private static double innerProduct(double[] ebd1, double[] ebd2) {
		assert ebd1.length == ebd2.length;
		double ret = .0;
		for (int i = 0; i < ebd1.length; i++) {
			ret += ebd1[i] * ebd2[i];
		}

		return ret;
	}

	public static double getCombinedSumOfLogRelatedness(Set<Integer> entities_left, Set<Integer> entities_right) {
		double sum = 0;
		for (int wid1 : entities_left) {
			for (int wid2 : entities_right) {
				double relatedness = WATRelatednessComputer.getMwRelatedness(wid1, wid2);
				if (relatedness == 0)
					relatedness = 0.01;
				sum += Math.log(relatedness);
			}
		}
		return sum;
	}

}