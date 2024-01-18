package searchengine.utils.indexing;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;


public class UserAgent {
    public ArrayList<String> userAgentList;

    public UserAgent() {
        userAgentList = new ArrayList<>();
        try {
            userAgentList = (ArrayList<String>) Files.readAllLines(Paths.get("data/userAgent.txt"));
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    public String userAgentGet() {
        Random random = new Random();
        int max = userAgentList.size() - 1;
        return this.userAgentList.get(random.nextInt(max));
    }


}
