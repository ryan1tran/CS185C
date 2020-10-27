package hmm;

public class FileProcessor {
	
	
	public static String stripExt(String string) {
		return string.replaceFirst("[.][^.]+$", "");
	}
}
