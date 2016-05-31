package annotatorstub.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
//// This sample uses the Apache HTTP client from HTTP Components (http://hc.apache.org/httpcomponents-client-ga/)
import java.net.URI;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.math3.util.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BingCorrectionHelper implements Serializable {

	public static void main(String[] args) throws FileNotFoundException, IOException {
//		System.out.println(BingCorrectionHelper.correction("letterforpastorappreciateion").getFirst());
//		System.out.println(instance.spellCheckMap_1.get("reverse telephone informatio n"));
//		flush();
		
		BingCorrectionHelper bingCorrectionHelper = new BingCorrectionHelper();
		try {	
	         File inputFile = new File("queries_new.xml");
	         DocumentBuilderFactory dbFactory 
	            = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	         Document doc = dBuilder.parse(inputFile);
	         doc.getDocumentElement().normalize();
	         NodeList nList = doc.getElementsByTagName("instance");
	         for (int temp = 0; temp < nList.getLength(); temp++) {
	        	 Thread.sleep(8600);
	            Node nNode = nList.item(temp);
	            String query = nNode.getTextContent();
	            System.out.println(bingCorrectionHelper.correction(query).getFirst() + " VS " + query);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		flush(); 
	}
	
	private static String resultsCacheFilename = "spellcheck.cache";
	private static BingCorrectionHelper instance = deserialize();
	private HashMap<String, String> spellCheckMap_1 = new HashMap<>();
	private HashMap<String, HashMap<String, String>> spellCheckMap_2 = new HashMap<>();
	
	public static BingCorrectionHelper deserialize() {
		try {
			File cache = new File(resultsCacheFilename);
			if (cache.exists()) {
				ObjectInputStream oos = new ObjectInputStream(new FileInputStream(cache));
				Object o = oos.readObject();
				oos.close();
				return (BingCorrectionHelper) o;
			}
			else
				return new BingCorrectionHelper();
		} catch (Exception e) {
			return new BingCorrectionHelper();
		}		
	}
	
	public static synchronized void flush() throws FileNotFoundException, IOException {		
		new File(resultsCacheFilename).createNewFile();
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(resultsCacheFilename));
		oos.writeObject(instance);
		oos.close();
	}
	
	public static Pair<String, HashMap<String, String>> correction(String query) {
		if (instance.spellCheckMap_1.containsKey(query)) {
			return new Pair<String, HashMap<String, String>>(instance.spellCheckMap_1.get(query), instance.spellCheckMap_2.get(query));
		}
		String query_org = query;
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
		Pair pair = new Pair<String, HashMap<String, String>>(query, retMap);
		instance.spellCheckMap_1.put(query_org, query);
		instance.spellCheckMap_2.put(query_org, retMap);
		return pair;
	}
}