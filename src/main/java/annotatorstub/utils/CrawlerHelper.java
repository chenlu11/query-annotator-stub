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
			s = wikipara.text().trim().replaceAll(
					"\\b(a|about|above|after|again|against|all|am|an|and|any|are|aren't|as|at|be|because|been|before|being|below|between|both|but|by|can't|cannot|could|couldn't|did|didn't|do|does|doesn't|doing|don't|down|during|each|few|for|from|further|had|hadn't|has|hasn't|have|haven't|having|he|he'd|he'll|he's|her|here|here's|hers|herself|him|himself|his|how|how's|i|i'd|i'll|i'm|i've|if|in|into|is|isn't|it|it's|its|itself|let's|me|more|most|mustn't|my|myself|no|nor|not|of|off|on|once|only|or|other|ought|our|ours|ourselves|out|over|own|same|shan't|she|she'd|she'll|she's|should|shouldn't|so|some|such|than|that|that's|the|their|theirs|them|themselves|then|there|there's|these|they|they'd|they'll|they're|they've|this|those|through|to|too|under|until|up|very|was|wasn't|we|we'd|we'll|we're|we've|were|weren't|what|what's|when|when's|where|where's|which|while|who|who's|whom|why|why's|with|won't|would|wouldn't|you|you'd|you'll|you're|you've|your|yours|yourself|yourselves)\\b",
					"");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("not found:  "+ wikiUrlPrefix + entity_id );
		}
		return s;
	}

}
