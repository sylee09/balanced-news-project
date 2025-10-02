package producer.crawling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import producer.dto.NaverNewsResponse;
import producer.dto.NewsMessage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class Crawling {

    private final NaverApiValueObject vo;
    private final ObjectMapper objectMapper = new ObjectMapper()
            //JSON에는 있지만, Mapping될 Object에는 없는 필드를 무시해야하는 경우
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Cache<String, Boolean> newsUrlCache;
    private final NewsDetailCrawler newsDetailCrawler;

    String[] section = new String[]{"정치", "경제", "사회", "IT/과학"};

    public void crawl() {

        String text = null;
        String apiURL = null;

        try {
            for (String sec : section) {
                text = URLEncoder.encode(sec, "UTF-8");
                apiURL = "https://openapi.naver.com/v1/search/news.json?query=" + text + "&display=100";
                Map<String, String> requestHeaders = new HashMap<>();
                requestHeaders.put("X-Naver-Client-Id", vo.getClientId());
                requestHeaders.put("X-Naver-Client-Secret", vo.getClientSecret());

                // Naver API 요청/응답 받음
                String responseBody = get(apiURL, requestHeaders);

                // API 요청 결과를 JSON으로 매핑
                NaverNewsResponse apiResponse = objectMapper.readValue(responseBody, NaverNewsResponse.class);

                if (apiResponse.getItems() != null) {
                    for (NewsMessage message : apiResponse.getItems()) {
                        message.setCategory(sec);
                        String articleUrl = message.getUrl();

                        // 이미 처리한 기사면 더이상 진행 하지 않음
                        if (newsUrlCache.getIfPresent(articleUrl) != null) {
                            log.info("이미 처리한 기사");
                            continue;
                        }

                        // 상세 내용 크롤링
                        String articleDetail = newsDetailCrawler.crawlArticleContent(articleUrl);

                        // 카프카로 전송

                        // 처리 완료 했으므로 캐시에 이값을 저장
                        newsUrlCache.put(articleUrl, Boolean.TRUE);
                        log.info("message: {}", message);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("검색어 인코딩 실패", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String get(String apiUrl, Map<String, String> requestHeaders) {
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readBody(con.getInputStream());
            } else {
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        }finally {
            con.disconnect();
        }
    }

    private HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);

        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();

            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }

            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는 데 실패했습니다.", e);
        }
    }
}