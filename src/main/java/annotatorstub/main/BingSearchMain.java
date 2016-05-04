package annotatorstub.main;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.BingInterface;

public class BingSearchMain {
	final int num_top_result = 3; // the number of returned results that we consider
	/**
	 * Given a query, return the concatenation of the top 3 bing search result(title + description per result).
	 * @param query The query containing the mention
	 * @return A String value representing the concatenation of the top 3 result.
	 * @throws Exception
	 */
	String getBingSearchResult(String query) throws Exception{
		BingInterface bing = new BingInterface("IRvN9mc0Zql0YWe30+gGlHtF7/uQc1WJ8YBiy/HuLiI");
		JSONObject a = bing.queryBing(query);
		JSONArray res_arr = a.getJSONObject("d").getJSONArray("results").getJSONObject(0).getJSONArray("Web");
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < num_top_result; i ++){
			JSONObject obj = res_arr.getJSONObject(i);
			sb.append(obj.getString("Title"));
			sb.append(obj.getString("Description"));
		}
		return sb.toString();		
	}

}
