package searchengine.tools;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class SnippetGenerator {
	private String text;
	private List<String> queryWords;


	public void setText(String text) {
		this.text = Jsoup
				.clean(text, Safelist.simpleText())
				.replaceAll("[^А-Яа-яЁё\\d\\s,.!]+", " ")
				.replaceAll("\\s+", " ");
	}

	public void setQueryWords(List<String> queryWords) {
		this.queryWords = queryWords;
	}

	public Map<Integer, String> getWordsAndPos(@NotNull String text) {
		Map<Integer, String> words = new HashMap<>();
		int pos = 0;
		int index = text.indexOf(" ");
		while (index >= 0) {
			String word = text.substring(pos, index);
			word = word.replaceAll("\\P{L}+", "");
			if (!word.isEmpty()) {
				words.put(pos, word);
			}
			pos = index + 1;
			index = text.indexOf(" ", pos);
		}
		String lastWord = text.substring(pos);
		lastWord = lastWord.replaceAll("\\P{L}+", "");
		if (!lastWord.isEmpty()) {
			words.put(pos, lastWord);
		}
		return words;
	}











}

