package providers;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateProcessor {
	private TemplateProcessor() {
	
	}
	
	public static String process(String toProcess, Map<String, String> keys) {
		String[] messageParts = toProcess.split("\\$\\{[A-Za-z]+}");
		StringBuilder result = new StringBuilder(messageParts[0]);
		
		Pattern templatesPattern = Pattern.compile("\\$\\{[A-Za-z]+}", Pattern.CASE_INSENSITIVE);
		Matcher matcher = templatesPattern.matcher(toProcess);
		
		int i = 0;
		while (matcher.find() && (i++ < messageParts.length)) {
			String g = matcher.group().replaceAll("[${}]", "").toLowerCase();
			result.append(keys.get(g));
			// TODO: костыль
			if(i < messageParts.length)
				result.append(messageParts[i]);
		}
		return result.toString();
	}
}
