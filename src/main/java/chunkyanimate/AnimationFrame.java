package chunkyanimate;

import chunkyanimate.reflection.BooleanJsonField;
import chunkyanimate.reflection.DoubleJsonField;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonValue;
import se.llbit.math.Vector3;

import java.lang.reflect.Field;
import java.util.OptionalDouble;
import java.util.function.Function;

public class AnimationFrame {
    @DoubleJsonField("fogDensity") public double fogDensity;
    @DoubleJsonField("skyFogDensity") public double skyFogDensity;
    @DoubleJsonField("fogColor.red") public double fogColorR;
    @DoubleJsonField("fogColor.green") public double fogColorG;
    @DoubleJsonField("fogColor.blue") public double fogColorB;

    @BooleanJsonField("waterWorldEnabled") public boolean waterWorldEnabled;
    @DoubleJsonField("waterWorldHeight") public double waterWorldHeight;

    @DoubleJsonField("camera.position.x") public double cameraPositionX;
    @DoubleJsonField("camera.position.y") public double cameraPositionY;
    @DoubleJsonField("camera.position.z") public double cameraPositionZ;

    @DoubleJsonField("camera.orientation.yaw") public double cameraOrientationYaw;
    @DoubleJsonField("camera.orientation.pitch") public double cameraOrientationPitch;
    @DoubleJsonField("camera.orientation.roll") public double cameraOrientationRoll;

    @DoubleJsonField("camera.fov") public double cameraFov;
    @DoubleJsonField("camera.dof") public double cameraDof;
    @DoubleJsonField("camera.focalOffset") public double cameraFocus;

    @DoubleJsonField("sun.altitude") public double sunAltitude;
    @DoubleJsonField("sun.azimuth") public double sunAzimuth;
    @DoubleJsonField("sun.intensity") public double sunIntensity;
    @DoubleJsonField("sun.color.red") public double sunColorR;
    @DoubleJsonField("sun.color.green") public double sunColorG;
    @DoubleJsonField("sun.color.blue") public double sunColorB;
    @BooleanJsonField("sun.drawTexture") public boolean sunDraw;

    @BooleanJsonField("sky.cloudsEnabled") public boolean cloudsEnabled;
    @DoubleJsonField("sky.cloudOffset.x") public double cloudOffsetX;
    @DoubleJsonField("sky.cloudOffset.y") public double cloudOffsetY;
    @DoubleJsonField("sky.cloudOffset.z") public double cloudOffsetZ;

    @DoubleJsonField("animationTime") public double animationTime;

    public AnimationFrame(Scene scene) {
        this.fogDensity = scene.getFogDensity();
        this.skyFogDensity = scene.getSkyFogDensity();
        this.fogColorR = scene.getFogColor().x;
        this.fogColorG = scene.getFogColor().y;
        this.fogColorB = scene.getFogColor().z;

        this.waterWorldEnabled = scene.isWaterPlaneEnabled();
        this.waterWorldHeight = scene.getWaterPlaneHeight();

        this.cameraPositionX = scene.camera().getPosition().x;
        this.cameraPositionY = scene.camera().getPosition().y;
        this.cameraPositionZ = scene.camera().getPosition().z;
        this.cameraOrientationYaw = scene.camera().getYaw();
        this.cameraOrientationPitch = scene.camera().getPitch();
        this.cameraOrientationRoll = scene.camera().getRoll();
        this.cameraFov = scene.camera().getFov();
        this.cameraDof = scene.camera().getDof();
        this.cameraFocus = scene.camera().getSubjectDistance();

        this.sunAltitude = scene.sun().getAltitude();
        this.sunAzimuth = scene.sun().getAzimuth();
        this.sunIntensity = scene.sun().getIntensity();
        this.sunColorR = scene.sun().getColor().x;
        this.sunColorG = scene.sun().getColor().y;
        this.sunColorB = scene.sun().getColor().z;
        this.sunDraw = scene.sun().drawTexture();

        this.cloudsEnabled = scene.sky().cloudsEnabled();
        this.cloudOffsetX = scene.sky().cloudXOffset();
        this.cloudOffsetY = scene.sky().cloudYOffset();
        this.cloudOffsetZ = scene.sky().cloudZOffset();

        this.animationTime = scene.getAnimationTime();
    }

    public AnimationFrame(JsonObject json, AnimationFrame prev) {
        this(prev);

        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                BooleanJsonField booleanField = field.getAnnotation(BooleanJsonField.class);
                if (booleanField != null) {
                    field.set(this, resolveJsonField(json, booleanField.value()).boolValue((Boolean) field.get(this)));
                    continue;
                }

                DoubleJsonField doubleField = field.getAnnotation(DoubleJsonField.class);
                if (doubleField != null) {
                    field.set(this, resolveJsonField(json, doubleField.value()).doubleValue((Double) field.get(this)));
                    continue;
                }
            } catch (IllegalAccessException e) {
                // Ignored
            }
        }
    }

    private static JsonValue resolveJsonField(JsonObject json, String field) {
        JsonValue value = json;
        for (String level : field.split("\\.")) {
            value = value.asObject().get(level);
        }
        return value;
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public AnimationFrame(AnimationFrame prev) {
        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                field.set(this, field.get(this));
            } catch (IllegalAccessException e) {
                // Ignored
            }
        }
    }

    public AnimationFrame(Function<String, OptionalDouble> fieldProvider, AnimationFrame prev) {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                Object value = field.get(this);
                Object prevValue = field.get(prev);
                if (value instanceof Double && prevValue instanceof Double) {
                    field.set(this, fieldProvider.apply(field.getName()).orElse((Double) prevValue));
                }
            } catch (IllegalAccessException e) {
                // Ignored
            }
        }
    }

    public void apply(Scene scene) {
        scene.setFogDensity(this.fogDensity);
        scene.setSkyFogDensity(this.skyFogDensity);
        scene.setFogColor(new Vector3(
                this.fogColorR,
                this.fogColorG,
                this.fogColorB
        ));

        scene.setWaterPlaneEnabled(this.waterWorldEnabled);
        scene.setWaterPlaneHeight(this.waterWorldHeight);

        scene.camera().setPosition(new Vector3(
                this.cameraPositionX,
                this.cameraPositionY,
                this.cameraPositionZ
        ));
        scene.camera().setView(
                this.cameraOrientationYaw,
                this.cameraOrientationPitch,
                this.cameraOrientationRoll
        );
        scene.camera().setFoV(this.cameraFov);
        scene.camera().setDof(this.cameraDof);
        scene.camera().setSubjectDistance(this.cameraFocus);

        scene.sun().setAltitude(this.sunAltitude);
        scene.sun().setAzimuth(this.sunAzimuth);
        scene.sun().setIntensity(this.sunIntensity);
        scene.sun().setColor(new Vector3(
                this.sunColorR,
                this.sunColorG,
                this.sunColorB
        ));
        scene.sun().setDrawTexture(this.sunDraw);

        scene.sky().setCloudsEnabled(this.cloudsEnabled);
        scene.sky().setCloudXOffset(this.cloudOffsetX);
        scene.sky().setCloudYOffset(this.cloudOffsetY);
        scene.sky().setCloudZOffset(this.cloudOffsetZ);

        scene.setAnimationTime(this.animationTime);
    }
}
