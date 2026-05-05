package vn.edu.iuh.fit.server.util.id.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;
import vn.edu.iuh.fit.server.util.id.TrainIdGenerator;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@IdGeneratorType(TrainIdGenerator.class)
public @interface GeneratedTrainId {}

