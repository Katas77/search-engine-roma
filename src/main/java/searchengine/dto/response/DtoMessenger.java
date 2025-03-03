package searchengine.dto.response;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
@Builder
public class DtoMessenger {
    boolean result;
    String error;
    public ResponseEntity<Object> statusOk() {
        return new ResponseEntity<>(
                DtoMessenger.builder()
                        .result(true)
                        .error("Успешное принятие и обработка запроса клиента.")
                        .build(), HttpStatus.OK);
    }

    public ResponseEntity<Object> indexationAlreadyStarted() {
        return new ResponseEntity<>(
                DtoMessenger.builder()
                        .result(false)
                        .error("Индексация уже запущена.")
                        .build(), HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Object> indexingNotRunning() {
        return new ResponseEntity<>(
                DtoMessenger.builder()
                        .result(false)
                        .error("Индексация не запущена.")
                        .build(), HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Object> indexPageFailed() {
        return new ResponseEntity<>(
                DtoMessenger.builder()
                        .result(false)
                        .error("Данная страница находится за пределами сайтов указанных в конфигурационном файле!")
                        .build(), HttpStatus.NOT_FOUND);
    }
}

