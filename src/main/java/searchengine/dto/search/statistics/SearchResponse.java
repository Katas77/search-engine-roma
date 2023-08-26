package searchengine.dto.search.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SearchResponse {
    private boolean result;
    private int count;
    List<SearchData> data;
}
