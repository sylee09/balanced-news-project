package producer.crawling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NaverApiConfig {

    @Bean
    public NaverApiValueObject naverApiValueObject(
            @Value("${Client_id}") String clientId,
            @Value("${Client_Secret}") String clientSecret) {

        NaverApiValueObject vo = new NaverApiValueObject();
        vo.setClientId(clientId);
        vo.setClientSecret(clientSecret);
        return vo;
    }
}
