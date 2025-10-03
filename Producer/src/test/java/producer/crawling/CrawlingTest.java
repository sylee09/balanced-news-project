package producer.crawling;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlingTest {

    @Mock
    NaverApiValueObject vo;
    @Mock
    NewsDetailCrawler detailCrawler;

    @Spy
    Cache<String, Boolean> cache = Caffeine.newBuilder().build();

    @InjectMocks
    Crawling crawling;
    Crawling crawlingSpy;

    @BeforeEach
    void setup() {
        when(vo.getClientId()).thenReturn("id");
        when(vo.getClientSecret()).thenReturn("secret");

        crawlingSpy = spy(crawling);

        String fakeJson = "{ \"items\": [" +
                "{\"title\":\"t1\",\"link\":\"https://news.naver.com/article/001/000000001\"," +
                "\"url\":\"https://news.naver.com/article/001/000000001\"}" +
                "]}";
        doReturn(fakeJson).when(crawlingSpy).get(anyString(), anyMap());
    }

    @Test
    @DisplayName("성공적인 크롤링 동작: 상세 크롤러가 호출되고 캐시에 저장된다.")
    void test1() {
        String url = "https://news.naver.com/article/001/000000001";
        when(detailCrawler.crawlArticleContent(url)).thenReturn("Body");

        crawlingSpy.crawl();

        verify(detailCrawler, times(1)).crawlArticleContent(url);

        Boolean cached = cache.getIfPresent(url);
        assertThat(cached)
                .isTrue();
    }

    @Test
    @DisplayName("중복되는 기사는 더이상 크롤링 절차를 진행하지 않는다.")
    void test2() {
        String url = "https://news.naver.com/article/001/000000001";
        cache.put(url, Boolean.TRUE);

        crawlingSpy.crawl();

        verify(detailCrawler, never()).crawlArticleContent(anyString());
    }
}
