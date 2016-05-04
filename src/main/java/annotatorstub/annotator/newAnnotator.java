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


public class newAnnotator implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = -1f;
	public static void main(String[] args) throws AnnotationException, IOException{
		String text = "I like Vodka sauce";
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
		
//		HashSet<ScoredAnnotation> annotations = ann.BaseLine("I like Vodka sauce");		
//		for (Annotation a : annotations){
//			System.out.printf("mention: %s: , link is: http://en.wikipedia.org/wiki/index.html?curid=%d\n", 
//					query.substring(a.getPosition(), a.getPosition()+a.getLength()), a.getConcept());
//		}		
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
			String men = mentions.get(i);
			for(int id: WATRelatednessComputer.getLinks(men)){
				ArrayList<String> pair = new ArrayList<String>();
				pair.add(men);
				pair.add(Integer.toString(id));
				mention_entity_pair.add(pair);
			}
		}
		return mention_entity_pair;
	}
	
	
	// construct_mentions not used 
	
	// BASELINE 
		
	// Call BASELINE
	
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
		return "Simple yet uneffective query annotator";
	}

	@Override
	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
		// TODO Auto-generated method stub
		return null;
	}
}
