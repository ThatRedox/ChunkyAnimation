package chunkyanimate.reflection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DoubleField {
    /**
     * Friendly name.
     */
    String value();

    /**
     * Sort order. Must be in the form "{a}.{b}".
     * Fields are sorted by {a} and fields with the same {a} are sorted by {b}.
     */
    String sortOrder() default "";

    /**
     * Value is in radians.
     */
    boolean inRadians() default false;
}
