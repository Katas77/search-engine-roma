package searchengine.dto.forAll;

import lombok.Value;

@Value
public class BadRequest {
    private    boolean result;
    String BadRequestText;
}
