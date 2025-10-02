package producer.dto;

import lombok.Data;

import java.util.List;

@Data
public class NaverNewsResponse {
    private List<NewsMessage> items;
}
