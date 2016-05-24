package annotatorstub.utils;

public class TextHelper {
	
	public static String[] parse(String str){
		return str.trim().toLowerCase().replaceAll("[^A-Za-z0-9 ]", " ").split("\\W+");
	}
	public static String replace(String str) {
		return str.toLowerCase().replaceAll("[^A-Za-z0-9 ]", " ");
	}

}
