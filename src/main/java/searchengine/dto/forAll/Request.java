package searchengine.dto.forAll;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class Request {
    boolean result;
    String error;


    public ResponseEntity<Object> statusOk() {
        return new ResponseEntity<>(
                new Request(
                        true,
                        "Успешное принятие и обработка запроса клиента."),
                HttpStatus.OK);
    }

    public ResponseEntity<Object> indexationAlreadyStarted() {
        return new ResponseEntity<>(
                new Request(
                        false,
                        "Индексация уже запущена."),
                HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Object> indexingNotRunning() {
        return new ResponseEntity<>(
                new Request(
                        false,
                        "Индексация не запущена."),
                HttpStatus.BAD_REQUEST);
    }


    public ResponseEntity<Object> indexPageFailed() {
        return new ResponseEntity<>(
                new Request(false,
                        "Данная страница находится за пределами сайтов,  " +
                                "указанных в конфигурационном файле!"),
                HttpStatus.NOT_FOUND);
    }
}

