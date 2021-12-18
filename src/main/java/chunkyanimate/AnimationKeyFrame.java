package chunkyanimate;

import com.sun.javafx.scene.control.behavior.OptionalBoolean;

import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class AnimationKeyFrame {
    public static final int INTERP_FIELDS;
    static {
        int numFields = 0;
        Field[] fields = AnimationKeyFrame.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == OptionalDouble.class) {
                numFields++;
            }
        }
        INTERP_FIELDS = numFields;
    }

    public OptionalDouble fogDensity = OptionalDouble.empty();
    public OptionalDouble skyFogDensity = OptionalDouble.empty();
    public OptionalDouble fogColorX = OptionalDouble.empty();
    public OptionalDouble fogColorY = OptionalDouble.empty();
    public OptionalDouble fogColorZ = OptionalDouble.empty();

    public OptionalBoolean waterWorldEnabled = OptionalBoolean.ANY;
    public OptionalDouble waterWorldHeight = OptionalDouble.empty();

    public OptionalDouble cameraPositionX = OptionalDouble.empty();
    public OptionalDouble cameraPositionY = OptionalDouble.empty();
    public OptionalDouble cameraPositionZ = OptionalDouble.empty();

    public OptionalDouble cameraOrientationYaw = OptionalDouble.empty();
    public OptionalDouble cameraOrientationPitch = OptionalDouble.empty();
    public OptionalDouble cameraOrientationRoll = OptionalDouble.empty();

    public OptionalDouble cameraFov = OptionalDouble.empty();
    public OptionalDouble cameraDof = OptionalDouble.empty();
    public OptionalDouble cameraFocus = OptionalDouble.empty();

    public OptionalDouble sunAltitude = OptionalDouble.empty();
    public OptionalDouble sunAzimuth = OptionalDouble.empty();
    public OptionalDouble sunIntensity = OptionalDouble.empty();
    public OptionalDouble sunColorX = OptionalDouble.empty();
    public OptionalDouble sunColorY = OptionalDouble.empty();
    public OptionalDouble sunColorZ = OptionalDouble.empty();
    public OptionalBoolean sunDraw = OptionalBoolean.ANY;

    public OptionalBoolean cloudsEnabled = OptionalBoolean.ANY;
    public OptionalDouble cloudOffsetX = OptionalDouble.empty();
    public OptionalDouble cloudOffsetY = OptionalDouble.empty();
    public OptionalDouble cloudOffsetZ = OptionalDouble.empty();

    public OptionalDouble animationTime = OptionalDouble.empty();

    public SortedMap<String, OptionalDouble> getInterpFields() {
        Field[] fields = this.getClass().getDeclaredFields();
        TreeMap<String, OptionalDouble> interpFields = new TreeMap<>();

        for (Field field : fields) {
            try {
                Object value = field.get(this);
                if (value instanceof OptionalDouble) {
                    interpFields.put(field.getName(), (OptionalDouble) value);
                }
            } catch (IllegalAccessException e) {
                // Ignored
            }
        }

        assert interpFields.size() == INTERP_FIELDS;
        return interpFields;
    }
}
