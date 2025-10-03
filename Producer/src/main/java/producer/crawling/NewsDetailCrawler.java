package producer.crawling;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

@Service
@Slf4j
public class NewsDetailCrawler {

    private final String[] NAVER_BODY_SELECTORS = new String[]{
            "#dic_area",
            "#newsct_article",
            ".newsct_article",
            "article .article_body",
            "article"
    };

    public String crawlArticleContent(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(5000)
                .get();
        Element content = null;
        for (String sel : NAVER_BODY_SELECTORS) {
            content = doc.selectFirst(sel);
            if (content != null) {
                break;
            }
        }
        if (content == null) content = doc.body();

        // 3. 추출된 요소에서 텍스트만 가져와서 불필요한 공백과 줄바꿈을 정리.
        String fullText = content.text();

        // 4. 추출된 텍스트를 반환.
        return fullText;
    }
}
