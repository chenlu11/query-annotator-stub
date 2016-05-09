package annotatorstub.annotator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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


	public static void main(String[] args) throws Exception{
		String text = "I like Vodka sauce";
//		text = "strawberry fields forever"; 
		String query = text.replaceAll("[^A-Za-z0-9 ]", " "); // only remain A-Za-z0-9 and replace other charaters with space
		String[] words = query.split("\\s+");
		for(int i=0; i<words.length; i++){
			words[i] = words[i].replaceAll("[^\\w]", "");
		}
		ArrayList<String> Word_List = new ArrayList<String>();
		Collections.addAll(Word_List,words);
		
		newAnnotator ann = new newAnnotator();
		
		/*---------------------------example--------------------------*/
		ArrayList<String> mentions = ann.construct_m(Word_List);
		for(int i=0; i<mentions.size(); i++)
			System.out.println(mentions.get(i));
		ArrayList<ArrayList<String>> m_e_pairs = ann.construct_m_e_pair(Word_List);
		for(int i=0; i<m_e_pairs.size(); i++)
			System.out.println(m_e_pairs.get(i));
		
		HashSet<ScoredAnnotation> annotations = ann.contextModel(text);
		for (Annotation a : annotations){
			System.out.printf("mention: %s: , link is: http://en.wikipedia.org/wiki/index.html?curid=%d\n", 
					query.substring(a.getPosition(), a.getPosition()+a.getLength()), a.getConcept());
		}
		
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
	/**
	 * construct all possible mentions
	 * @param words
	 * @return
	 */
	public ArrayList<String> construct_m(ArrayList<String> words){
		ArrayList<String> mentions = new ArrayList<String>();
		for(int i=0; i<words.size(); i++){
        	String begin_at_i=null;
        	for(int j=i; j<words.size(); j++){
        		if(begin_at_i==null){
        			begin_at_i = words.get(j);
        		}
        		else{
        			begin_at_i = begin_at_i + " " + words.get(j);
        		}
        		mentions.add(begin_at_i);
        	}
        }
		return mentions;
	}
	/**
	 * Construct (mention,entity) pair
	 * @param words
	 * @return
	 */
	public ArrayList<ArrayList<String>> construct_m_e_pair(ArrayList<String> words){
		ArrayList<String> mentions = construct_m(words);
//		WikipediaApiInterface api = WikipediaApiInterface.api();
		ArrayList<ArrayList<String>> mention_entity_pair = new ArrayList<ArrayList<String>>();
		for(int i=0; i<mentions.size(); i++){
			ArrayList<String> pair = new ArrayList<String>();
			String men = mentions.get(i);
			pair.add(men);
			for(int id: WATRelatednessComputer.getLinks(men)){				
				pair.add(Integer.toString(id));				
			}
			mention_entity_pair.add(pair);
		}
		return mention_entity_pair;
	}
	/**
	 *  use context information to choose the most suitable (mention, entity) pairs
	 * @param text
	 * @return
	 * @throws Exception 
	 * @throws AnnotationException
	 */
	public double MEscore(String query, String mention, int entity_id) throws Exception{		
		String text_men = BingSearchHelper.getBingSearchResult(query);
		String text_entity = CrawlerHelper.getWikiPageDescription(entity_id);
		double result = EmbeddingHelper.getDistanceValue(text_men, text_entity);
		return result;
	}
	static class MentionScore{
		public String mention;
		public double score;	
		MentionScore(String m, double s){
			this.mention = m;
			this.score = s;
		}
	}
	static void  quickSort(MentionScore[] ms, int start, int end) {
		if (start >= end - 1)
			return;
		double pivot = ms[start].score;
		int low = start;
		int high = end - 1;
		while (low < high) {
			while(ms[high].score < pivot && low < high) high --;
			if(low < high) {
				ms[low].score = ms[high].score;
				low += 1;
			}
			while(ms[low].score >= pivot && low < high) low ++;
			if (low < high) {
				ms[high].score = ms[low].score;
				high -= 1;
			} 
		}
		assert low == high;
		ms[low].score = pivot;
		quickSort(ms, start, low);
		quickSort(ms, low + 1, end);
		 return;
	}

	public  HashSet<ScoredAnnotation> contextModel(String text) throws Exception{
		
		String[] words;
		// split string to words list
		String query = text.replaceAll("[^A-Za-z0-9 ]", " "); // only remain A-Za-z0-9 and replace other charaters with space
		words = query.split("\\s+");
		for(int i=0; i<words.length; i++){
			words[i] = words[i].replaceAll("[^\\w]", "");
		}
		ArrayList<String> Word_List = new ArrayList<String>();
		Collections.addAll(Word_List,words);
		
		ArrayList<ArrayList<String>> m_e_pairs = construct_m_e_pair(Word_List);
		HashMap<String, ArrayList<Float>> m_e_value = new HashMap<String, ArrayList<Float>> ();
		
		for(int i=0; i<m_e_pairs.size(); i++){
			String men = m_e_pairs.get(i).get(0);			
			int len = m_e_pairs.get(i).size();
			if(len>=2){ // have possible entities
				ArrayList<Float> value_list = new ArrayList<Float>();
				for(int j=1; j<m_e_pairs.get(i).size(); j++){
					int id = Integer.parseInt(m_e_pairs.get(i).get(j));
					// compute the value of <men, id> pair
					Float value = new Float(MEscore(text, men, id)); // suppose
					System.out.printf("%s, %d, value: %f \n", men, id, value);
					value_list.add(value);
				}
				m_e_value.put(men, value_list);
			}			
		}
		// for each mention, only keep the entity with largest value 
		HashMap<String, String> m_e = new HashMap<String, String>();
		HashMap<String, Float> m_s = new HashMap<String, Float>();
		List<MentionScore> m_s_list = new ArrayList<MentionScore>();
		for(int i=0; i<m_e_pairs.size(); i++){
			String men = m_e_pairs.get(i).get(0);
			int len = m_e_pairs.get(i).size();
			if(len>=2){
				ArrayList<Float> e_value = m_e_value.get(men);
//				double largest_value = 0;
				double smallest_value = 100;
				String e_id = null;
				for(int j=1; j<m_e_pairs.get(i).size(); j++){
					String id = m_e_pairs.get(i).get(j);
					Float value_id = e_value.get(j-1);
//					if(value_id>largest_value){
//						largest_value = value_id;
//						e_id = id;
//					}
					if(value_id<smallest_value){
						smallest_value = value_id;
						e_id = id;
					}
				}
				m_e.put(men, e_id);
//				m_s.put(men, new Float(largest_value));
				m_s.put(men, new Float(smallest_value));
//				m_s_list.add(new MentionScore(men, largest_value));
				m_s_list.add(new MentionScore(men, smallest_value));
			}
		}
		
		MentionScore[] m_s_array = m_s_list.toArray(new MentionScore[m_s_list.size()]);
//		System.out.println(m_e);
//		for(int i = 0; i < m_s_array.length; i ++) System.out.println("---------" + m_s_array[i].score);
		quickSort(m_s_array, 0, m_s_array.length);
		for(int i=0; i<m_s_array.length; i++){
			System.out.printf("%s: %f, ", m_s_array[i].mention, m_s_array[i].score);
		}
		System.out.printf("\n");
		
		Set<String> final_mentions = new HashSet<String>();
		Set<String> existed_words = new HashSet<String>();
		// for(int i=0; i<m_s_array.length; i++){
		for(int i=m_s_array.length-1; i>=0; i--){
			String mention_now_next = m_s_array[i].mention;
			String[] new_words = mention_now_next.trim().split("\\s+");
			boolean flag = false;
			for(int j=0; j<new_words.length; j++){
				String word_next = new_words[j].replaceAll("[^\\w]", "");
				if(existed_words.contains(word_next)){
					flag = true;
					break;
				}
			}
			if(flag==false){
				final_mentions.add(mention_now_next);				
				existed_words.addAll(new HashSet<String>(Arrays.asList(new_words)));				
			}						
		}
		
		HashSet<ScoredAnnotation> result = new HashSet<> ();
		
		for(String mention:final_mentions){
//			System.out.printf("%s, ", s);
			String[] words_in_query;
			words_in_query = mention.split("\\s+");
			int start_pos = text.indexOf(words_in_query[0]); // start position of the mention in the text
			int query_length = mention.length();
			int end_pos = start_pos + query_length; // end position of the mention in the text
			
			result.add(new ScoredAnnotation(start_pos, end_pos- start_pos, Integer.parseInt(m_e.get(mention)), m_s.get(mention) ) );
		}
		
		return result;
		
	}
	
	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException{
		HashSet<ScoredAnnotation> result = new HashSet<>();
		try {
			Utils.iter += 1;
			System.out.println("starting " + Utils.iter + " th query");
			result = contextModel(text);
			System.out.println("finished " + Utils.iter + " th query");
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



	
}
