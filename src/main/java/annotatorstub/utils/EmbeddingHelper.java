package annotatorstub.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class EmbeddingHelper {
	final static int dim = 300;
	final static String dict_path = "/Users/hanzhichao/Documents/ETH_Courses/NLP/project/eclipse_workspace/query-annotator-stub/deps.words";
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
		dict = new HashMap<String, double[]> ();
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
		for(int i = 0; i < dim; i ++) res[i] = 0;
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
		for (int i = 0; i < dim; i++)
			res[i] = res[i] / (double) numOfWords;
		return res;
	}

	/**
	 * Compute the Euclidean distance between two documents in the embedding
	 * space. The smaller is this value, the similar are the two documents.
	 * 
	 * @param doc1
	 *            The String Representation of Document1
	 * @param doc2
	 *            The String Representation of Document2
	 * @return distance: double
	 * @throws IOException
	 */
	public static double getDistanceValue(String doc1, String doc2) throws IOException {
		if (dict == null) {
			loadEmbeddings(dict_path);
		}
		if(doc1 == null || doc2 == null) {
			return Double.MAX_VALUE;
		}

		double[] ebd1 = computeDocEmbedding(doc1);
		double[] ebd2 = computeDocEmbedding(doc2);
		assert ebd1.length == dim && ebd2.length == dim;
		double distance = .0;
		for (int i = 0; i < dim; i++) {
			distance += (ebd1[i] - ebd2[i]) * (ebd1[i] - ebd2[i]);
		}
		distance = Math.sqrt(distance);
		return distance;
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