package annotatorstub.utils;

//// This sample uses the Apache HTTP client from HTTP Components (http://hc.apache.org/httpcomponents-client-ga/)
import java.net.URI;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class BingCorrectionHelper {

	public static void main(String[] args) {
		System.out.println(BingCorrectionHelper.correction("atytorney at law").getFirst());
	}

	public static Pair<String, HashMap<String, String>> correction(String query) {
		HashMap<String, String> retMap = new HashMap<>();
		try {
			HttpClient httpclient = HttpClients.createDefault();
			URIBuilder builder = new URIBuilder("https://bingapis.azure-api.net/api/v5/spellcheck");
			builder.setParameter("mode", "spell");
			URI uri = builder.build();
			HttpPost request = new HttpPost(uri);
			request.setHeader("Content-Type", "application/x-www-form-urlencoded");
			request.setHeader("Ocp-Apim-Subscription-Key", "bcecdbb4e673423bb1aca81bb1d4798f");
			StringEntity reqEntity = new StringEntity("Text=" + query);
			request.setEntity(reqEntity);
			HttpResponse response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				JSONArray a = new JSONObject(EntityUtils.toString(entity)).getJSONArray("flaggedTokens");
				for (int i = 0; i < a.length(); i++) {
					JSONObject obj = a.getJSONObject(i);
					String originalWord = obj.getString("token");
					String correctWord = obj.getJSONArray("suggestions").getJSONObject(0).getString("suggestion");
//					System.out.println(originalWord + " " + correctWord);
					retMap.put(correctWord, originalWord);
					query = query.replaceFirst(originalWord, correctWord);
				}
			}
		} catch (Exception e) {
		}
		return new Pair<String, HashMap<String, String>>(query, retMap);
	}
}