package producer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewsMessage {
    String title;
    @JsonProperty("originallink")
    String url;
    String category;
    LocalDateTime createdDateTime;
    String detail;
}
