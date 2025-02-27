import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class App {
    public static void main(String[] args) {
        Monitoring monitoring = new Monitoring();
//        monitoring.getNews("팔란티어", 10, 1, "date");
//        monitoring.getNews("팔란티어", 10, 1, SortType.date);
        monitoring.getNews(System.getenv("KEYWORD"), 10, 1, SortType.date);
    }
}

enum SortType {
    sim("sim"), date("date");

    final String value;

    SortType(String value) {
        this.value = value;
    }
}

class Monitoring {
    private final Logger logger;

    public Monitoring() {
//        logger = Logger.getLogger("Monitoring");
        logger = Logger.getLogger(Monitoring.class.getName());
        logger.setLevel(Level.SEVERE);
        logger.info("Monitoring 객체 생성");
    }

    // 1. 검색어를 통해서 최근 10개의 뉴스를 받아올게요
    public void getNews(String keyword, int display, int start, SortType sort) {
//    public void getNews(String keyword, int display, int start, String sort) {
        // https://developers.naver.com/docs/serviceapi/search/news/news.md#%EB%89%B4%EC%8A%A4
//        String url = "https://openapi.naver.com/v1/search/news.json";
//        String params = "query=%s&display=%d&start=%d&sort=%s".formatted(
//                keyword, display, start, sort.value
//        );
//        HttpClient client = HttpClient.newHttpClient(); // 클라이언트
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(url + "?" + params))
//                .GET()
//                .header("X-Naver-Client-Id", "*****")
//                .header("X-Naver-Client-Secret", "*****")
//                .build();
        String imageLink = "";
        try {
//            HttpResponse<String> response = client.send(request,
//                    HttpResponse.BodyHandlers.ofString());
//            String response = getDataFromAPI(keyword, display, start, sort);
            String response = getDataFromAPI("news.json", keyword, display, start, sort);
            String[] tmp = response.split("title\":\"");
            // 0번째를 제외하곤 데이터
            String[] result = new String[display];
            for (int i = 1; i < tmp.length; i++) {
                result[i - 1] = tmp[i].split("\",")[0];
            }
            logger.info(Arrays.toString(result));
            File file = new File("%d_%s.txt".formatted(new Date().getTime(), keyword));
            if (!file.exists()) {
                logger.info(file.createNewFile() ? "신규 생성" : "이미 있음");
            }
            try (FileWriter fileWriter = new FileWriter(file)) {
                for (String s : result) {
                    fileWriter.write(s + "\n");
                }
                logger.info("기록 성공");
            } // flush 및 close.
            logger.info("제목 목록 생성 완료");
            String imageResponse = getDataFromAPI("image", keyword, display, start, SortType.sim);
//            logger.info(imageResponse);
            // 2. 이미지
            imageLink = imageResponse
                    .split("link\":\"")[1].split("\",")[0]
                    .split("\\?")[0]
                    .replace("\\", "");
            logger.info(imageLink);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageLink))
                    .build();
            String[] tmp2 = imageLink.split("\\.");
            Path path = Path.of("%d_%s.%s".formatted(
                    new Date().getTime(), keyword, tmp2[tmp2.length - 1]));
            HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofFile(path));
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    private String getDataFromAPI(String path, String keyword, int display, int start, SortType sort) throws Exception {
//        String url = "https://openapi.naver.com/v1/search/news.json";
        String url = "https://openapi.naver.com/v1/search/%s".formatted(path);
        String params = "query=%s&display=%d&start=%d&sort=%s".formatted(
                keyword, display, start, sort.value
        );
        HttpClient client = HttpClient.newHttpClient(); // 클라이언트
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "?" + params))
                .GET()
                .header("X-Naver-Client-Id", System.getenv("NAVER_CLIENT_ID"))
                .header("X-Naver-Client-Secret", System.getenv("NAVER_CLIENT_SECRET"))
                .build();
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            // http 요청을 했을 때 잘 왔는지 보는 것
            logger.info(Integer.toString(response.statusCode()));
            logger.info(response.body());
            // split하든 나중에 GSON, Jackson
            return response.body();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            throw new Exception("연결 에러");
        }
    }
}
