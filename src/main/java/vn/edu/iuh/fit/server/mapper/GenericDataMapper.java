package vn.edu.iuh.fit.server.mapper;

import java.util.Map;

public interface GenericDataMapper {

    Map<String, Object> toMap(Object object);

    <T> T toObject(Map<String, Object> data, Class<T> clazz);
}
