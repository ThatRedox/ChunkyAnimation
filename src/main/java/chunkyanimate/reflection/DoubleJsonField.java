package chunkyanimate.reflection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DoubleJsonField {
    /**
     * Path to JSON field.
     */
    String value();
}
