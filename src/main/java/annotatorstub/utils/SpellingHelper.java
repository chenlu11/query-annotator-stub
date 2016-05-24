package annotatorstub.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.atteo.evo.inflector.English;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

public class SpellingHelper {
	public static final JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
	public static List<RuleMatch> matches;
	public static final boolean isUseSuggestion = false;

	public static void main(String[] args) {
//		String s = "kathy alfred;atytorney at law";
//		System.out.println(new SpellingHelper().getSuggestion(s));
		String[] words = "as sad sa erw".split(" ");

	}

	public static Pair<String, HashMap<String, String>> getSuggestion(String query) {
		HashMap<String, String> indexMap = new HashMap<>();
		try {
			matches = langTool.check(query);
			if (matches.isEmpty() || ! isUseSuggestion) {
				query = query.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "");
				return new Pair(query, indexMap);
			}
			StringBuilder sb = new StringBuilder();
			int lastIndex = 0;
			for (RuleMatch match : matches) {
				if (match.getFromPos() > lastIndex) {
					String pre = query.substring(lastIndex, match.getFromPos());
					if (!pre.equals(TextHelper.replace(pre))) {
						indexMap.put(TextHelper.replace(pre), pre);
						pre = TextHelper.replace(pre);
					}
					sb.append(pre);
					lastIndex = match.getToPos();
					String apdStr = "";
					try{
					    apdStr = TextHelper.replace(match.getSuggestedReplacements().get(0));
					}catch(Exception e) {
						System.out.println(match);
						System.out.println(match.getSuggestedReplacements());
					}
					sb.append(apdStr);
					// construct {new word -> original word} map
					indexMap.put(apdStr, query.substring(match.getFromPos(), match.getToPos()));
				} else if (match.getFromPos() == lastIndex) {
					String apdStr = TextHelper.replace(match.getSuggestedReplacements().get(0));
					sb.append(apdStr);
					// construct {new word -> original word} map
					indexMap.put(apdStr, query.substring(match.getFromPos(), match.getToPos()));
				}
				lastIndex = match.getToPos();

			}
			if (lastIndex < query.length()) {
				String apdStr = query.substring(lastIndex, query.length());
				if (!apdStr.equals(TextHelper.replace(apdStr))) {
					indexMap.put(TextHelper.replace(apdStr), apdStr);
					apdStr = TextHelper.replace(apdStr);
				}
				sb.append(apdStr);
			}
			System.out.println(new String(sb));
			return new Pair(new String(sb), indexMap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new Pair(query, indexMap);

	}

}
