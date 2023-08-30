package searchengine.dto.forAll;

import lombok.Value;

@Value
public class BadRequest {
    boolean result;
    String BadRequestText;
}
