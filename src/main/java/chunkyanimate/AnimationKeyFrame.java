package chunkyanimate;


import se.llbit.json.JsonNumber;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonString;
import se.llbit.json.JsonValue;

import java.lang.reflect.Field;
import java.util.*;

public class AnimationKeyFrame {
    private static String[] interpolatableFieldsResult = null;

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

    public static String[] interpolatableFields() {
        if (interpolatableFieldsResult == null) {
            SortedSet<String> out = new TreeSet<>();
            for (Field field : AnimationFrame.class.getDeclaredFields()) {
                if (field.getType() == double.class) {
                    out.add(field.getName());
                }
            }
            interpolatableFieldsResult = out.toArray(new String[0]);
        }
        return interpolatableFieldsResult;
    }
}
