package producer.crawling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.time.LocalDateTime;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        for (String sec : section) {
            try {
                String q = URLEncoder.encode(sec, UTF_8); // UTF-8은 checked X
                String apiURL = "https://openapi.naver.com/v1/search/news.json?query=" + q + "&display=100";

                Map<String, String> headers = Map.of(
                        "X-Naver-Client-Id", vo.getClientId(),
                        "X-Naver-Client-Secret", vo.getClientSecret()
                );

                String responseBody = get(apiURL, headers); // 200이 아니면 여기서 예외 던지도록 하는 게 더 안전
                NaverNewsResponse apiResponse = objectMapper.readValue(responseBody, NaverNewsResponse.class);

                if (apiResponse.getItems() == null) continue;

                for (NewsMessage message : apiResponse.getItems()) {
                    try {
                        message.setCategory(sec);
                        String articleUrl = message.getUrl();

                        if (articleUrl == null || !articleUrl.contains("news.naver.com")) continue;
                        if (newsUrlCache.getIfPresent(articleUrl) != null) {
                            log.info("이미 처리한 기사: {}", articleUrl);
                            continue;
                        }

                        String articleDetail = newsDetailCrawler.crawlArticleContent(articleUrl);
                        message.setDetail(articleDetail);
                        message.setCreatedDateTime(LocalDateTime.now());

                        // TODO: 카프카 전송

                        newsUrlCache.put(articleUrl, Boolean.TRUE);
                        log.info("OK: {}", articleUrl);
                    } catch (Exception itemEx) {
                        // 개별 기사 실패는 스킵하고 다음 기사 진행
                        log.warn("기사 처리 실패 (skip) url={} err={}", message.getUrl(), itemEx.toString());
                    }
                }
            } catch (Exception sectionEx) {
                // 섹션 전체 실패는 기록하고 다음 섹션 진행
                log.warn("섹션 수집 실패 (skip) section={} err={}", sec, sectionEx.toString());
            }
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