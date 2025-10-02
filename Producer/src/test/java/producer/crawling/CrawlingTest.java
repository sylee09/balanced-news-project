package producer.crawling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SectionCrawlingTest {
    @Autowired
    SectionCrawling sectionCrawling;

    @Test
    @DisplayName("section crawling using api 정치, 경제, 사회, IT/과학 섹션 크롤링")
    void test1() {
        sectionCrawling.crawlSection();
    }
}
