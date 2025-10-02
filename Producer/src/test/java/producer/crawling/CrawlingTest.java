package producer.crawling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CrawlingTest {
    @Autowired
    Crawling crawling;

    @Test
    @DisplayName("section crawling using api 정치, 경제, 사회, IT/과학 섹션 크롤링")
    void test1() {
        crawling.crawl();
    }

    @Test
    @DisplayName("중복되는 기사는 더이상 크롤링 절차를 진행하지 않는다.")
    void test2() {

    }
}
