package annotatorstub.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class EmbeddingHelper {
	final static int dim = 300;
	final static String dict_path = "deps.words";
	static HashMap<String, double[]> dict = null;

	/**
	 * Load pre-trained word embeddings. reference:
	 * https://www.cs.bgu.ac.il/~yoavg/publications/nips2014pmi.pdf pre-trained
	 * data: https://github.com/3Top/word2vec-api Wikipedia dependency dataset
	 * 
	 * @param path
	 * @throws IOException
	 */
	public static void loadEmbeddings(String path) throws IOException {
		System.out.println("----------------------Start loading word embeddings--------------------\n");
		dict = new HashMap<String, double[]>();
		File file = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(file));
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
			dict.put(word, embedding);
			str = br.readLine();
		}
		System.out.println("----------------------Finish loading word embeddings--------------------\n\n");
	}

	/**
	 * Compute the Embedding of a String, which contains multiple words by
	 * taking average on each dimension of word embedding.
	 * 
	 * @param str
	 *            The Document to be computed
	 * @return
	 */
	public static double[] computeDocEmbedding(String str) {
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
	/**
	 * Compute the inversed similarity between two documents
	 * @param doc1
	 * @param doc2
	 * @return Double: the inversed similarity between two documents
	 * @throws IOException
	 */
	public static double getInversedSimilarityValue(String doc1, String doc2) throws IOException {
		if (dict == null) {
			loadEmbeddings(dict_path);
		}
		if (doc1 == null || doc2 == null) {
			return Double.MAX_VALUE;
		}

		double[] ebd1 = computeDocEmbedding(doc1);
		double[] ebd2 = computeDocEmbedding(doc2);
		assert ebd1.length == dim && ebd2.length == dim;
		double similarity = .0;
		double norm1 = 0;
		double norm2 = 0;
		for (int i = 0; i < dim; i++) {
			similarity += ebd1[i] * ebd2[i];
			norm1 += ebd1[i] * ebd1[i];
			norm2 += ebd2[i] * ebd2[i];
		}
		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		similarity = similarity / (norm1 * norm2);
		return 1 / similarity;
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
//		double distance = .0;
//		for (int i = 0; i < dim; i++) {
//			distance += (ebd1[i] - ebd2[i]) * (ebd1[i] - ebd2[i]);
//		}
//		distance = Math.sqrt(distance);
		double distance = cosineSimilarity(ebd1, ebd2);
		return distance;
	}
	
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
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

	// /**
	// * Compute the inner product of two documents in the embedding space. (The
	// projection of one doc on the other doc)
	// * @param doc1 The String Representation of Document1
	// * @param doc2 The String Representation of Document2
	// * @return
	// * @throws IOException
	// */
	// public static double getProjectionValue(String doc1, String doc2) throws
	// IOException{
	// if(dict == null)
	// loadEmbeddings(dict_path);
	//
	// double[] ebd1 = computeDocEmbedding(doc1);
	// double[] ebd2 = computeDocEmbedding(doc2);
	// assert ebd1.length == dim && ebd2.length == dim;
	// double inner_product = .0;
	// for(int i = 0; i < dim; i++){
	// inner_product += ebd1[i] * ebd2[i];
	// }
	// return inner_product;
	// }

}