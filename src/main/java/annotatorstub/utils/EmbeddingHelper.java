package annotatorstub.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.logging.Log;

import it.unipi.di.acube.batframework.utils.Pair;

public class EmbeddingHelper {
	final static int dim = 300;
	final static String dict_path = "deps.words";
	static double[] entityEbd = new double[dim];
	static final HashMap<String, double[]> dict = loadEmbeddings(dict_path);
	public static final double initialVal = -10000;
	public static final double notLinkedScore = 2000;
	
	public static void main(String[] args) {
		new EmbeddingHelper().getHighestScore("disease", new String[] {"lyme",  "disease" ,"in" ,"georgia"});
	}
	/**
	 * Use this function to compute the HIGHESTSCORE
	 * 
	 * @param mention
	 *            The s in the paper, which denotes some segmentation of the
	 *            query
	 * @param query
	 *            The query
	 * @return
	 */
	public static Pair<Integer, Double> getHighestScore(String mention, String[] queryTerms) {
		int[] entity = WATRelatednessComputer.getLinks(mention);
		// maybe do some cutting edge process here to reduce # entities
		double minScore = Double.MAX_VALUE;
		int minEntity = 0;
		for (int i = 0; i < entity.length; i++) {
			double score = getProbabilityOfEntityGivenSegmentationAndQuery(queryTerms, mention, entity[i]);
//			System.out.println(score);
			if (score < minScore) {
				minScore = score;
				minEntity = entity[i];
			}
		} 
		if (entity.length == 0) {
			return new Pair<Integer, Double>(0, notLinkedScore);
		}

		return new Pair<Integer, Double>(minEntity, minScore);
	}
	/**
	 * compute p(e | s, q)
	 * @param queryTerms
	 * @param mention
	 * @param entityId
	 * @return
	 */
	private static double getProbabilityOfEntityGivenSegmentationAndQuery(String[] queryTerms, String mention,
			int entityId) {
		return getLogCommonness(mention, entityId) + getLogAddingProbability(queryTerms, entityId);
	}
	/**
	 * compute - log P(e | s), which is simply the commonness
	 * @param segmentation
	 * @param entityId
	 * @return
	 */
	private static double getLogCommonness(String segmentation, int entityId) {
		double com = WATRelatednessComputer.getCommonness(segmentation, entityId);
		if(com == 0) {
//			System.out.println("error: commonness = 0 ->" + "mention " + segmentation + "entityId " + entityId);
		}
		return - Math.log(com);
	}
	/**
	 * compute the sum of all - logP(t_i | e), where t_i is every word in the query
	 * @param terms
	 * @param entityId
	 * @return
	 */
	private static double getLogAddingProbability(String[] terms, int entityId) {
		String entity = CrawlerHelper.getWikiPageDescription(entityId);
		if (entity == null) {
			return 0;
		}
		entityEbd = computeDocEmbedding(entity);
		if(entityEbd == null) {
			return 0;
		}
		double ret = 0;
		for (String term : terms) {
			double[] termEbd = dict.get(term);
			if (termEbd != null) {
				ret += Math.log(getProbabilityOfTermGivenEntity(termEbd));
			}
		}
		return - ret;
	}
	/**
	 * compute p(t_i | e)
	 * @param termEbd
	 * @return
	 */
	private static double getProbabilityOfTermGivenEntity(double[] termEbd) {
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

	/**
	 * Compute the Embedding of a String, which contains multiple words by
	 * taking average on each dimension of word embedding.
	 * 
	 * @param str
	 *            The Document to be computed
	 * @return
	 */
	private static double[] computeDocEmbedding(String str) {
		if(str == null) {
			return null;
		}
		String[] doc = TextHelper.parse(str);
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

	/**
	 * Compute the Cosine Similarity between two documents in the embedding
	 * space. The larger is this value, the more similar are the two documents.
	 * 
	 * @param doc1
	 *            The String Representation of Document1
	 * @param doc2
	 *            The String Representation of Document2
	 * @throws IOException
	 */
	public static double getSimilarityValue(String doc1, String doc2) throws IOException {
		if (dict == null) {
			loadEmbeddings(dict_path);
		}
		if (doc1 == null || doc2 == null) {
			return 0;
		}
		double[] ebd1 = computeDocEmbedding(doc1);
		double[] ebd2 = computeDocEmbedding(doc2);
		if (ebd1 == null || ebd2 == null) {
			return 0;
		}
		assert ebd1.length == dim && ebd2.length == dim;
		// double distance = .0;
		// for (int i = 0; i < dim; i++) {
		// distance += (ebd1[i] - ebd2[i]) * (ebd1[i] - ebd2[i]);
		// }
		// distance = Math.sqrt(distance);
		double similarity = cosineSimilarity(ebd1, ebd2);
		return similarity;
	}

	private static double cosineSimilarity(double[] vectorA, double[] vectorB) {
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < vectorA.length; i++) {
			dotProduct += vectorA[i] * vectorB[i];
			normA += Math.pow(vectorA[i], 2);
			normB += Math.pow(vectorB[i], 2);
		}
		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}

}