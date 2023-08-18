package searchengine.request;

import lombok.Value;

@Value
public class BadRequest {
    boolean result;
    String BadRequestText;
}
