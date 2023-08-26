package searchengine.dto.searh;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import searchengine.dto.searh.SearchData;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SearchResponse {
    private boolean result;
    private int count;
    List<SearchData> data;
}
