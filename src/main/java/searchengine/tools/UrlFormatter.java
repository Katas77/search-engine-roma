package searchengine.tools;




public class UrlFormatter {
	public static int findNthOccurrence( String str, char ch, int n) {
		int index = str.indexOf(ch);
		while (--n > 0 && index != -1) {
			index = str.indexOf(ch, index + 1);
		}
		return index;
	}

	public static String getShortUrl(String url) {
		return url.substring(0, findNthOccurrence(url, '/', 3) + 1);
	}
}
/*not null*/