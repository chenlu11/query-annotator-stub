package annotatorstub.utils;

import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.math3.util.Pair;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class PluralToSingularHelper {
	public static StanfordCoreNLP pipeline = null;
	public static void main(String[] args) {
	}
	
	public static Pair<String, HashMap<String, String>> getSingular(String query, HashMap<String, String> src) {
		if(pipeline == null) {
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit, pos, lemma");
			pipeline = new StanfordCoreNLP(props, false);
		}
		HashMap<String, String> ret = new HashMap<>();
		Annotation document = pipeline.process(query);
		StringBuilder sb = new StringBuilder();
		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(TextAnnotation.class);
				String lemma = token.get(LemmaAnnotation.class);
				query = query.replace(word, lemma);
				if(src.containsKey(word)){
					ret.put(lemma, src.get(word));
				} else {
					ret.put(lemma, word);
				}
//				System.out.println("lemmatized version :" + lemma);
			}
		}
		return new Pair<String, HashMap<String, String>> (query, ret);
	}
}