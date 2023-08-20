package dev.thatredox.chunky.animate.animation;

import dev.thatredox.chunky.animate.reflection.BooleanJsonField;
import dev.thatredox.chunky.animate.reflection.DoubleField;
import dev.thatredox.chunky.animate.reflection.DoubleJsonField;
import dev.thatredox.chunky.animate.reflection.DoubleSceneField;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.Json;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonValue;
import se.llbit.log.Log;
import se.llbit.math.Vector3;

import java.lang.reflect.Field;
import java.util.OptionalDouble;
import java.util.function.Function;

public class AnimationFrame {
    @DoubleJsonField("fog.uniformDensity")
    @DoubleSceneField("fog.uniformDensity")
    @DoubleField("Fog density")
    public double fogDensity;

    @DoubleJsonField("fog.skyFogDensity")
    @DoubleSceneField("fog.skyFogDensity")
    @DoubleField("Sky fog blending")
    public double skyFogDensity;

    @DoubleJsonField("fog.fogColor.red")
    @DoubleSceneField("fog.fogColor.x")
    @DoubleField(value = "Fog color (R)", sortOrder = "fogColor.0")
    public double fogColorR;

    @DoubleJsonField("fog.fogColor.green")
    @DoubleSceneField("fog.fogColor.y")
    @DoubleField(value = "Fog color (G)", sortOrder = "fogColor.1")
    public double fogColorG;

    @DoubleJsonField("fog.fogColor.blue")
    @DoubleSceneField("fog.fogColor.z")
    @DoubleField(value = "Fog color (B)", sortOrder = "fogColor.2")
    public double fogColorB;

    @DoubleJsonField("waterWorldHeight")
    @DoubleSceneField("waterPlaneHeight")
    @DoubleField("Water world height")
    public double waterWorldHeight;

    @DoubleJsonField("camera.position.x")
    @DoubleSceneField("camera.pos.x")
    @DoubleField(value = "Camera position (X)", sortOrder = "cameraPosition.0")
    public double cameraPositionX;

    @DoubleJsonField("camera.position.y")
    @DoubleSceneField("camera.pos.y")
    @DoubleField(value = "Camera position (Y)", sortOrder = "cameraPosition.1")
    public double cameraPositionY;

    @DoubleJsonField("camera.position.z")
    @DoubleSceneField("camera.pos.z")
    @DoubleField(value = "Camera position (Z)", sortOrder = "cameraPosition.2")
    public double cameraPositionZ;

    @DoubleJsonField("camera.orientation.yaw")
    @DoubleSceneField("camera.yaw")
    @DoubleField(value = "Camera yaw", sortOrder = "cameraOrientation.0", inRadians = true)
    public double cameraOrientationYaw;

    @DoubleJsonField("camera.orientation.pitch")
    @DoubleSceneField("camera.pitch")
    @DoubleField(value = "Camera pitch", sortOrder = "cameraOrientation.1", inRadians = true)
    public double cameraOrientationPitch;

    @DoubleJsonField("camera.orientation.roll")
    @DoubleSceneField("camera.roll")
    @DoubleField(value = "Camera roll", sortOrder = "cameraOrientation.2", inRadians = true)
    public double cameraOrientationRoll;

    @DoubleJsonField("camera.fov")
    @DoubleSceneField("camera.fov")
    @DoubleField("Camera field of view")
    public double cameraFov;

    @DoubleJsonField("camera.dof")
    @DoubleSceneField("camera.dof")
    @DoubleField("Camera depth of field")
    public double cameraDof;

    @DoubleJsonField("camera.focalOffset")
    @DoubleSceneField("camera.subjectDistance")
    @DoubleField("Camera focal length")
    public double cameraFocus;

    @DoubleJsonField("exposure")
    @DoubleSceneField("exposure")
    @DoubleField("Camera exposure")
    public double cameraExposure;

    @DoubleJsonField("sun.altitude")
    @DoubleSceneField("sun.altitude")
    @DoubleField(value = "Sun altitude", inRadians = true)
    public double sunAltitude;

    @DoubleJsonField("sun.azimuth")
    @DoubleSceneField("sun.azimuth")
    @DoubleField(value = "Sun azimuth", inRadians = true)
    public double sunAzimuth;

    @DoubleJsonField("sun.intensity")
    @DoubleSceneField("sun.intensity")
    @DoubleField("Sun intensity")
    public double sunIntensity;

    @DoubleJsonField("sun.color.red")
    @DoubleSceneField("sun.color.x")
    @DoubleField(value = "Sun color (R)", sortOrder = "sunColor.0")
    public double sunColorR;

    @DoubleJsonField("sun.color.green")
    @DoubleSceneField("sun.color.y")
    @DoubleField(value = "Sun color (G)", sortOrder = "sunColor.1")
    public double sunColorG;

    @DoubleJsonField("sun.color.blue")
    @DoubleSceneField("sun.color.z")
    @DoubleField(value = "Sun color (B)", sortOrder = "sunColor.2")
    public double sunColorB;

    @DoubleJsonField("sky.cloudOffset.x")
    @DoubleSceneField("sky.cloudOffset.x")
    @DoubleField(value = "Cloud offset (X)", sortOrder = "cloudOffset.0")
    public double cloudOffsetX;

    @DoubleJsonField("sky.cloudOffset.y")
    @DoubleSceneField("sky.cloudOffset.y")
    @DoubleField(value = "Cloud offset (Y)", sortOrder = "cloudOffset.1")
    public double cloudOffsetY;

    @DoubleJsonField("sky.cloudOffset.z")
    @DoubleSceneField("sky.cloudOffset.z")
    @DoubleField(value = "Cloud offset (Z)", sortOrder = "cloudOffset.2")
    public double cloudOffsetZ;

    @DoubleJsonField("animationTime")
    @DoubleSceneField("animationTime")
    @DoubleField("Animation time")
    public double animationTime;

    public AnimationFrame(Scene scene) {

        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                DoubleSceneField doubleField = field.getAnnotation(DoubleSceneField.class);
                if (doubleField != null) {
                    field.set(this, resolveSceneDoubleField(scene, doubleField.value()));
                    continue;
                }
            } catch (IllegalAccessException e) {
                // Ignored
            }
        }

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

    public static JsonValue resolveJsonField(JsonObject json, String field) {
        JsonValue value = json;
        for (String level : field.split("\\.")) {
            value = value.asObject().get(level);
        }
        return value;
    }

    public static void writeJsonField(JsonValue value, JsonObject json, String field) {
        JsonObject obj = json;
        String[] levels = field.split("\\.");
        for (int i = 0; i < levels.length-1; i++) {
            String level = levels[i];
            if (!obj.get(level).isUnknown()) {
                obj = json.get(level).asObject();
            } else {
                JsonObject newObj = new JsonObject();
                obj.set(level, newObj);
                obj = newObj;
            }
        }
        obj.set(levels[levels.length-1], value);
    }

    public static double resolveSceneDoubleField(Scene scene, String field) {
        Object obj = scene;
        try {
            for (String level : field.split("\\.")) {
                Field f = obj.getClass().getDeclaredField(level);
                f.setAccessible(true);
                obj = f.get(obj);
            }
            return (double) obj;
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
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

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                BooleanJsonField booleanField = field.getAnnotation(BooleanJsonField.class);
                if (booleanField != null) {
                    writeJsonField(Json.of((Boolean) field.get(this)), obj, booleanField.value());
                    continue;
                }

                DoubleJsonField doubleField = field.getAnnotation(DoubleJsonField.class);
                if (doubleField != null) {
                    writeJsonField(Json.of((Double) field.get(this)), obj, doubleField.value());
                    continue;
                }
            } catch (IllegalAccessException e) {
                // Ignored
            }
        }
        return obj;
    }

    public void apply(Scene scene) {
        scene.setFogDensity(this.fogDensity);
        scene.setSkyFogDensity(this.skyFogDensity);
        scene.setFogColor(new Vector3(
                this.fogColorR,
                this.fogColorG,
                this.fogColorB
        ));

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
        scene.setExposure(this.cameraExposure);

        scene.sun().setAltitude(this.sunAltitude);
        scene.sun().setAzimuth(this.sunAzimuth);
        scene.sun().setIntensity(this.sunIntensity);
        scene.sun().setColor(new Vector3(
                this.sunColorR,
                this.sunColorG,
                this.sunColorB
        ));

        scene.sky().setCloudXOffset(this.cloudOffsetX);
        scene.sky().setCloudYOffset(this.cloudOffsetY);
        scene.sky().setCloudZOffset(this.cloudOffsetZ);

        scene.setAnimationTime(this.animationTime);
    }
}
