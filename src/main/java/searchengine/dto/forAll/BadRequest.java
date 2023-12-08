package searchengine.dto.forAll;

import lombok.Value;

@Value
public class BadRequest {
    private int statusCode;
    String BadRequestText;
}
