package dev.thatredox.chunky.animate.animation;


import dev.thatredox.chunky.animate.reflection.DoubleField;
import se.llbit.json.JsonNumber;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonString;
import se.llbit.json.JsonValue;

import java.lang.reflect.Field;
import java.util.*;

public class AnimationKeyFrame {
    private static Field[] interpolatableFieldsResult = null;

    public final SortedMap<String, Double> interpFields = new TreeMap<>();
    public String keyframeName = "Keyframe";

    public AnimationKeyFrame() {
    }

    public AnimationKeyFrame(String keyframeName) {
        this();
        this.keyframeName = keyframeName;
    }

    public AnimationKeyFrame(JsonObject obj) {
        this();
        for (Map.Entry<String, JsonValue> entry : obj.toMap().entrySet()) {
            if (entry.getKey().equals("keyframeName")) {
                this.keyframeName = entry.getValue().asString(keyframeName);
            } else {
                double value = entry.getValue().doubleValue(Double.NaN);
                if (!Double.isNaN(value)) {
                    interpFields.put(entry.getKey(), value);
                }
            }
        }
    }

    public JsonObject toJson() {
        JsonObject out = new JsonObject();
        out.set("keyframeName", new JsonString(keyframeName));
        for (Map.Entry<String, Double> entry : interpFields.entrySet()) {
            out.set(entry.getKey(), new JsonNumber(entry.getValue()));
        }
        return out;
    }

    public static Field[] interpolatableFields() {
        if (interpolatableFieldsResult == null) {
            SortedSet<Field> out = new TreeSet<>((o1, o2) -> {
                String fieldName1 = o1.getName();
                String fieldName2 = o2.getName();

                String secondaryCompare1 = "";
                String secondaryCompare2 = "";

                String[] parts;

                DoubleField df1 = o1.getAnnotation(DoubleField.class);
                parts = df1.sortOrder().split("\\.");
                if (parts.length == 2) {
                    fieldName1 = parts[0];
                    secondaryCompare1 = parts[1];
                }

                DoubleField df2 = o2.getAnnotation(DoubleField.class);
                parts = df2.sortOrder().split("\\.");
                if (parts.length == 2) {
                    fieldName2 = parts[0];
                    secondaryCompare2 = parts[1];
                }

                if (fieldName1.equals(fieldName2)) {
                    return secondaryCompare1.compareTo(secondaryCompare2);
                } else {
                    return fieldName1.compareTo(fieldName2);
                }
            });
            for (Field field : AnimationFrame.class.getDeclaredFields()) {
                if (field.getType() == double.class && field.isAnnotationPresent(DoubleField.class)) {
                    out.add(field);
                }
            }
            interpolatableFieldsResult = out.toArray(new Field[0]);
        }
        return interpolatableFieldsResult;
    }
}
