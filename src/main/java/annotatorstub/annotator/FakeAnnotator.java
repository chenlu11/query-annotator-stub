package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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


public class FakeAnnotator implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = -1f;
	public static void main(String[] args) throws AnnotationException, IOException{
		BaseLine("I like Vodka sauce");
		
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
	public static List<String> construct_mentions(List<String> words) throws Exception{
		System.out.println("input words: "+words.toString());
		List<String> Mentions = new ArrayList<String>();
//		WikipediaApiInterface api = WikipediaApiInterface.api();
		WATRelatednessComputer.setCache("relatedness.cache");
		
//		String query = text.replaceAll("[^A-Za-z0-9 ]", " ");
//		String[] words = query.split("\\s+");
//		for(int i=0; i<words.length; i++){
//			words[i] = words[i].replaceAll("[^\\w]", "");
//		}
		int[] mention_length = new int[words.size()]; // record the length of longest mention beginning at each postion,  initial values are 0's
		for(int i=0; i<words.size(); i++){
			mention_length[i] = 0;
		}			
		for(int i=0; i<words.size(); i++){			
			// construct mention using words in range [i, j] (including position i and position j)
			for(int j=i; j<words.size(); j++){
				StringBuilder builder = new StringBuilder();
				for(int k=i; k<=j; k++){ // 
					if(builder.length()>0){
						builder.append(" ");
					}					
					builder.append(words.get(k));
				}
				String mention_i_j = builder.toString();
				System.out.println("mention_i_j is: " + mention_i_j);
				if(WATRelatednessComputer.getLp(mention_i_j) >0){ // is p(e|m)>0, add this mention to candidate set (longest mention)
					if(j-i+1>mention_length[i]){
						mention_length[i] = j - i + 1;
					}
				}			
			}
		}
		// compute the longest mention, with the words in range [begin_pos, end_pos] (including begin_pos & end_pos)
		int begin_pos = 0; // begin position of longest mention
		int max_length = 0;// length of longest mention
		for(int i=0; i<words.size(); i++){
			if(mention_length[i]>max_length){
				max_length = mention_length[i];
				begin_pos = i;
			}
		} 
		if(max_length==0){
			return Mentions; // return emtpy Mentions
		}
		int end_pos = begin_pos + max_length - 1; // end position of longest mention
		StringBuilder builder = new StringBuilder();
		for(int i=begin_pos; i<=end_pos; i++){
			if(builder.length()>0){
				builder.append(" ");
			}
			builder.append(words.get(i));
		}
		String longest_mention = builder.toString();
		
		// add longest mention in this words list
//		System.out.println("max length: "+ max_length + "; begin position: " + begin_pos + "; end position: " + end_pos);
//		System.out.println("longest mention: "+ longest_mention + "; longest mention size: " +  longest_mention.length());
		
		if(longest_mention.length()>0){ 
			Mentions.add(longest_mention);
		}
		else{
			return Mentions;
		}
		// split this words list by the longest mention and compute the Mentions in the first sublist
		if(begin_pos>0){// Mentions.subList(beginPos, endPos): in range [beginPos, endPos), beginPos: inclusive; endPos: exclusive
			List<String> previous_Mentions = construct_mentions(words.subList(0, begin_pos));
			if(previous_Mentions.size()>0){
				for(int i=0; i<previous_Mentions.size(); i++){ 
					Mentions.add(previous_Mentions.get(i));
				}
			}
		}
		// compute the Mentions in the second sublist
		if(end_pos<words.size()-1 && end_pos>=0){
			System.out.println("end position: "+end_pos);
			List<String> following_Mentions = construct_mentions(words.subList(end_pos+1, words.size()));
			if(following_Mentions.size()>0){
				for(int i=0; i<following_Mentions.size(); i++){
					Mentions.add(following_Mentions.get(i));
				}
			}
		}
		
	
		
		return Mentions;
		
			
	}
	public static void BaseLine(String text) throws AnnotationException, IOException {
		lastTime = System.currentTimeMillis();
		String[] words;
		// split string to words list
		String query = text.replaceAll("[^A-Za-z0-9 ]", " "); // only remain A-Za-z0-9 and replace other charaters with space
		words = query.split("\\s+");
		for(int i=0; i<words.length; i++){
			words[i] = words[i].replaceAll("[^\\w]", "");
		}
		ArrayList<String> Word_List = new ArrayList<String>();
		Collections.addAll(Word_List,words);
		
		// find all possible mentions that could be linked (s.t. all m that satisfies p(e|m)>0)
		List<String> Mentions = new ArrayList<String>();
		try {
			Mentions = construct_mentions(Word_List);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		for(int i=0; i<Mentions.size(); i++)
			System.out.println("mentions: "+ Mentions.get(i));
		System.out.println("score: "+ WATRelatednessComputer.getLp("Vodka sauce"));
		
		// compute the most possible entity (concept) for each mention
		WikipediaApiInterface api = WikipediaApiInterface.api();
		for(int i=0; i<Mentions.size(); i++){
			String Query = Mentions.get(i);
			double largest_prob = 0; // the probability p(e|m), initialed with 0
			int entity_id = 0;
			for(int id: WATRelatednessComputer.getLinks(Query)){
				double score = WATRelatednessComputer.getCommonness(Query, id);
				if(largest_prob<score){
					largest_prob  = score;
					entity_id = id;
				}				
			}
			System.out.println(Query + " is mostly linked to " + api.getTitlebyId(entity_id) + " (http://en.wikipedia.org/wiki/index.html?curid=" + entity_id + ") " + " with prob: "+ largest_prob);
		}
		
		
		
	}
	
	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
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
		return "Simple yet uneffective query annotator";
	}
}
