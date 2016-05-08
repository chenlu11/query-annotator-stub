package annotatorstub.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import annotatorstub.annotator.FakeAnnotator;

import java.util.*;
import java.io.*;
import it.unipi.di.acube.BingInterface;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.utils.AnnotationException;

public class BingSearchHelper {
	public static void main(String[] args) throws Exception{
		String query = "I like Vodka sauce";
		query = "strawberry fields forever";
		String result = new BingSearchHelper().getBingSearchResult(query);
		System.out.printf("%s", result);
	}
	final int num_top_result = 3; // the number of returned results that we consider
	/**
	 * Given a query, return the concatenation of the top 3 bing search result (title + description per result).
	 * @param query The query containing the mention
	 * @return A String value representing the concatenation of the top 3 result.
	 * @throws Exception
	 */
//	String getBingSearchResult(String query) throws Exception{		
//		BingInterface bing = new BingInterface("IRvN9mc0Zql0YWe30+gGlHtF7/uQc1WJ8YBiy/HuLiI");
//		JSONObject a = bing.queryBing(query);
//		JSONArray res_arr = a.getJSONObject("d").getJSONArray("results").getJSONObject(0).getJSONArray("Web");
//		StringBuilder sb = new StringBuilder();
//		for(int i = 0; i < num_top_result; i ++){
//			JSONObject obj = res_arr.getJSONObject(i);
//			sb.append(obj.getString("Title"));
//			sb.append(obj.getString("Description"));
//		}
//		return sb.toString();
//	}
	public String getBingSearchResult(String query) throws Exception{	
		String headFileName = "/Users/hanzhichao/Documents/ETH_Courses/NLP/project/eclipse_workspace/query-annotator-stub/dump_searchResult/query_file.ser";
		HashMap<String, String> map = new HashMap<String, String>();
		map = readHeadFile(headFileName);
		JSONObject a;
		
		String result = null;
		BingInterface bing = new BingInterface("IRvN9mc0Zql0YWe30+gGlHtF7/uQc1WJ8YBiy/HuLiI");
		if(map.containsKey(query)){
			System.out.printf("%s found in query-file\n", query);
			String contextFileName = map.get(query);
//			a = readFile(contextFileName);
			result = readFile(contextFileName);
		}else{
			System.out.printf("%s not found in query-file... ", query);			
			a = bing.queryBing(query);
			JSONArray res_arr = a.getJSONObject("d").getJSONArray("results").getJSONObject(0).getJSONArray("Web");
			int mapSize = map.size();
			String contextFileName = "/Users/hanzhichao/Documents/ETH_Courses/NLP/project/eclipse_workspace/query-annotator-stub/dump_searchResult/" + Integer.toString(mapSize+1) + ".ser";
			map.put(query, contextFileName);
			creatHeadFile(headFileName, map);
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<num_top_result; i++){
				JSONObject obj = res_arr.getJSONObject(i);		
				sb.append(obj.getString("Title"));
				sb.append(obj.getString("Description"));
			}
			result = sb.toString();
			dumpFile(result, contextFileName);
		}		
		return result;
	}
	
	public void creatHeadFile(String filename, HashMap<String, String> map){		
		try{
			FileOutputStream fileOut = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(map);
			out.close();
			fileOut.close();
			System.out.printf("query-fileName file is saved in %s \n", filename);
	    }catch(IOException i){
			i.printStackTrace();
		}
	}
	
	public HashMap<String, String> readHeadFile(String filename){
		HashMap<String, String> map = new HashMap<String,String>();
		try{
			FileInputStream fileIn = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			map = (HashMap) in.readObject();
			in.close();
			fileIn.close();
		}catch(IOException i){
			i.printStackTrace();
         	return new HashMap<String,String>();
		}catch(ClassNotFoundException c){
			System.out.println("class not found");
        	c.printStackTrace();
        	return new HashMap<String,String>();
		}
		return map;
	}
	
	public void dumpFile(String context, String filename){
		try{
			FileOutputStream fileOut = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(context);
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in %s \n", filename);
	    }catch(IOException i){
			i.printStackTrace();
		}
	}
	
	public String readFile(String filename){
		String context;
		try{
			FileInputStream fileIn = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			context = (String) in.readObject();
			in.close();
			fileIn.close();
		}catch(IOException i){
			i.printStackTrace();
         	return null;
		}catch(ClassNotFoundException c){
			System.out.println("context not found");
        	c.printStackTrace();
        	return null;
		}
//		System.out.println(context);
		return context;
	}

}
