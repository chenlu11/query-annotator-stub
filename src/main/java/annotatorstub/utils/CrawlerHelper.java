package annotatorstub.utils;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CrawlerHelper {
	final static String wikiUrlPrefix = "http://en.wikipedia.org/wiki/index.html?curid=";

	public static void main(String[] args) throws Exception {
		System.out.println(getWikiPageDescription(32787));

	}

	/**
	 * Get Wikipedia entity description given the entity id
	 * 
	 * @param entity_id
	 * @return String: the description of this entity in wikipedia
	 * @throws IOException
	 */
	public static String getWikiPageDescription(int entity_id) {
		Document doc;
		String s = null;
		try {
			doc = Jsoup.connect(wikiUrlPrefix + entity_id).get();

			Element wikipart = doc.select("div.mw-content-ltr").first();
			Element wikipara = wikipart.select("p").first();
			s = wikipara.text().trim();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		return s;
	}
}
