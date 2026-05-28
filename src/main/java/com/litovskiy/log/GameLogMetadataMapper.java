package com.litovskiy.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litovskiy.log.metadata.EmptyLogMetadata;
import com.litovskiy.log.metadata.GrowLogMetadata;
import com.litovskiy.log.metadata.JackpotLogMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameLogMetadataMapper {

    private final ObjectMapper objectMapper;

    public JsonNode toJson(LogMetadata metadata) {
        return objectMapper.valueToTree(metadata);
    }

    public <T extends LogMetadata> T fromJson(JsonNode node, Class<T> type) {
        return objectMapper.convertValue(node, type);
    }

    public Class<? extends LogMetadata> getMetadataClass(Action action) {
        return switch (action) {
            case GROW -> GrowLogMetadata.class;
            //case SLOW -> SlowLogMetadata.class;
            //case TRANSFER -> TransferLogMetadata.class;
            //case PRAY -> PrayLogMetadata.class;
            //case TURTLE -> TurtleLogMetadata.class;
            //case UNLUCKY -> TurtleLogMetadata.class;
            case JACKPOT -> JackpotLogMetadata.class;
            default -> EmptyLogMetadata.class;
        };
    }
}