package annotatorstub.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CrawlerHelper {
	final static String wikiUrlPrefix = "http://en.wikipedia.org/wiki/index.html?curid=";
    public static void main(String[] args) throws Exception {
    	System.out.println(getWikiPageDescription(32787));

    }
    
//    /**
//     * Given an url path, get the html content as a result
//     * @param url_path
//     * @return A String denotes the full HTML content of the given url address
//     * @throws IOException
//     */
//    public static String getHTML(String url_path) throws IOException{
//        URL url = new URL(url_path);
//        URLConnection uc = url.openConnection();
//        BufferedReader in = new BufferedReader(new InputStreamReader(
//                                uc.getInputStream()));
//        StringBuilder sb = new StringBuilder();
//        String inputLine;
//        while ((inputLine = in.readLine()) != null) 
//        	sb.append(inputLine);
//        in.close();
//        return sb.toString();
//    }
    
    public static String getWikiPageDescription(int entity_id) throws IOException{
    	Document doc = Jsoup.connect(wikiUrlPrefix + entity_id).get();
    	Element wikipart = doc.select("div.mw-content-ltr").first();
    	Element wikipara = wikipart.select("p").first();
    	String s = wikipara.text();
    	return s;
    }
}
