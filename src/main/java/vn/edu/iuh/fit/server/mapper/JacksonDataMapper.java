package vn.edu.iuh.fit.server.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

public class JacksonDataMapper implements GenericDataMapper {

    private final ObjectMapper objectMapper;

    public JacksonDataMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Map<String, Object> toMap(Object object) {
        if (object == null) {
            return null;
        }
        return objectMapper.convertValue(object, Map.class);
    }

    @Override
    public <T> T toObject(Map<String, Object> data, Class<T> clazz) {
        if (data == null) {
            return null;
        }
        return objectMapper.convertValue(data, clazz);
    }
}
