package dev.thatredox.chunky.animate.reflection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DoubleSceneField {
    /**
     * Path to field in Scene.java
     */
    String value();
}
