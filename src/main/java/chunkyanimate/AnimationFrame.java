package chunkyanimate;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.JsonObject;
import se.llbit.math.Vector3;

import java.lang.reflect.Field;
import java.util.OptionalDouble;
import java.util.function.Function;

public class AnimationFrame {
    public double fogDensity;
    public double skyFogDensity;
    public Vector3 fogColor = new Vector3();

    public boolean waterWorldEnabled;
    public double waterWorldHeight;

    public Vector3 cameraPosition = new Vector3();
    public Vector3 cameraOrientation = new Vector3();
    public double cameraFov;
    public double cameraDof;
    public double cameraFocus;

    public double sunAltitude;
    public double sunAzimuth;
    public double sunIntensity;
    public Vector3 sunColor = new Vector3();
    public boolean sunDraw;

    public boolean cloudsEnabled;
    public Vector3 cloudOffset = new Vector3();

    public double animationTime;

    public AnimationFrame(Scene scene) {
        this.fogDensity = scene.getFogDensity();
        this.skyFogDensity = scene.getSkyFogDensity();
        this.fogColor.set(scene.getFogColor());

        this.waterWorldEnabled = scene.isWaterPlaneEnabled();
        this.waterWorldHeight = scene.getWaterPlaneHeight();

        this.cameraPosition.set(scene.camera().getPosition());
        this.cameraOrientation.set(scene.camera().getYaw(), scene.camera().getPitch(), scene.camera().getRoll());
        this.cameraFov = scene.camera().getFov();
        this.cameraDof = scene.camera().getDof();
        this.cameraFocus = scene.camera().getSubjectDistance();

        this.sunAltitude = scene.sun().getAltitude();
        this.sunAzimuth = scene.sun().getAzimuth();
        this.sunIntensity = scene.sun().getIntensity();
        this.sunColor.set(scene.sun().getColor());
        this.sunDraw = scene.sun().drawTexture();

        this.cloudsEnabled = scene.sky().cloudsEnabled();
        this.cloudOffset.set(
                scene.sky().cloudXOffset(),
                scene.sky().cloudYOffset(),
                scene.sky().cloudZOffset()
        );

        this.animationTime = scene.getAnimationTime();
    }

    public AnimationFrame(JsonObject json, AnimationFrame prev) {
        this.fogDensity = json.get("fogDensity").asDouble(prev.fogDensity);
        this.skyFogDensity = json.get("skyFogDensity").asDouble(prev.skyFogDensity);
        this.fogColor.set(
                json.get("fogColor").asObject().get("red").asDouble(prev.fogColor.x),
                json.get("fogColor").asObject().get("green").asDouble(prev.fogColor.y),
                json.get("fogColor").asObject().get("blue").asDouble(prev.fogColor.z)
        );

        this.waterWorldEnabled = json.get("waterWorldEnabled").asBoolean(prev.waterWorldEnabled);
        this.waterWorldHeight = json.get("waterWorldHeight").asDouble(prev.waterWorldHeight);

        JsonObject jsonCamera = json.get("camera").asObject();
        this.cameraPosition.set(
                jsonCamera.get("position").asObject().get("x").asDouble(prev.cameraPosition.x),
                jsonCamera.get("position").asObject().get("y").asDouble(prev.cameraPosition.y),
                jsonCamera.get("position").asObject().get("z").asDouble(prev.cameraPosition.z)
        );
        this.cameraOrientation.set(
                jsonCamera.get("orientation").asObject().get("yaw").asDouble(prev.cameraOrientation.x),
                jsonCamera.get("orientation").asObject().get("pitch").asDouble(prev.cameraOrientation.y),
                jsonCamera.get("orientation").asObject().get("roll").asDouble(prev.cameraOrientation.z)
        );
        this.cameraFov = jsonCamera.get("fov").asDouble(prev.cameraFov);
        this.cameraDof = jsonCamera.get("dof").asDouble(prev.cameraDof);
        this.cameraFocus = jsonCamera.get("focalOffset").asDouble(prev.cameraFocus);

        JsonObject jsonSun = json.get("sun").asObject();
        this.sunAltitude = jsonSun.get("altitude").asDouble(prev.sunAltitude);
        this.sunAzimuth = jsonSun.get("azimuth").asDouble(prev.sunAzimuth);
        this.sunIntensity = jsonSun.get("intensity").asDouble(prev.sunIntensity);
        this.sunColor.set(
                jsonSun.get("color").asObject().get("red").asDouble(prev.sunColor.x),
                jsonSun.get("color").asObject().get("green").asDouble(prev.sunColor.y),
                jsonSun.get("color").asObject().get("blue").asDouble(prev.sunColor.z)
        );
        this.sunDraw = jsonSun.get("drawTexture").asBoolean(prev.sunDraw);

        JsonObject jsonSky = json.get("sky").asObject();
        this.cloudsEnabled = jsonSky.get("cloudsEnabled").asBoolean(prev.cloudsEnabled);
        this.cloudOffset.set(
                jsonSky.get("cloudOffset").asObject().get("x").asDouble(prev.cloudOffset.x),
                jsonSky.get("cloudOffset").asObject().get("y").asDouble(prev.cloudOffset.y),
                jsonSky.get("cloudOffset").asObject().get("z").asDouble(prev.cloudOffset.z)
        );

        this.animationTime = json.get("animationTime").asDouble(prev.animationTime);
    }

    public AnimationFrame(AnimationFrame prev) {
        this.fogDensity = prev.fogDensity;
        this.skyFogDensity = prev.skyFogDensity;
        this.fogColor.set(prev.fogColor);

        this.waterWorldEnabled = prev.waterWorldEnabled;
        this.waterWorldHeight = prev.waterWorldHeight;

        this.cameraPosition.set(prev.cameraPosition);
        this.cameraOrientation.set(prev.cameraOrientation);
        this.cameraFov = prev.cameraFov;
        this.cameraDof = prev.cameraDof;
        this.cameraFocus = prev.cameraFocus;

        this.sunAltitude = prev.sunAltitude;
        this.sunAzimuth = prev.sunAzimuth;
        this.sunIntensity = prev.sunIntensity;
        this.sunColor.set(prev.sunColor);
        this.sunDraw = prev.sunDraw;

        this.cloudsEnabled = prev.cloudsEnabled;
        this.cloudOffset.set(prev.cloudOffset);

        this.animationTime = prev.animationTime;
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

        fogColor.set(
                fieldProvider.apply("fogColorX").orElse(prev.fogColor.x),
                fieldProvider.apply("fogColorY").orElse(prev.fogColor.y),
                fieldProvider.apply("fogColorZ").orElse(prev.fogColor.z)
        );
        cameraPosition.set(
                fieldProvider.apply("cameraPositionX").orElse(prev.cameraPosition.x),
                fieldProvider.apply("cameraPositionY").orElse(prev.cameraPosition.y),
                fieldProvider.apply("cameraPositionZ").orElse(prev.cameraPosition.z)
        );
        cameraOrientation.set(
                fieldProvider.apply("cameraOrientationYaw").orElse(prev.cameraOrientation.x),
                fieldProvider.apply("cameraOrientationPitch").orElse(prev.cameraOrientation.y),
                fieldProvider.apply("cameraOrientationRoll").orElse(prev.cameraOrientation.z)
        );
        sunColor.set(
                fieldProvider.apply("sunColorX").orElse(prev.sunColor.x),
                fieldProvider.apply("sunColorY").orElse(prev.sunColor.y),
                fieldProvider.apply("sunColorZ").orElse(prev.sunColor.z)
        );
        cloudOffset.set(
                fieldProvider.apply("cloudOffsetX").orElse(prev.cloudOffset.x),
                fieldProvider.apply("cloudOffsetY").orElse(prev.cloudOffset.y),
                fieldProvider.apply("cloudOffsetZ").orElse(prev.cloudOffset.z)
        );
    }

    public void apply(Scene scene) {
        scene.setFogDensity(this.fogDensity);
        scene.setSkyFogDensity(this.skyFogDensity);
        scene.setFogColor(this.fogColor);

        scene.setWaterPlaneEnabled(this.waterWorldEnabled);
        scene.setWaterPlaneHeight(this.waterWorldHeight);

        scene.camera().setPosition(this.cameraPosition);
        scene.camera().setView(this.cameraOrientation.x, this.cameraOrientation.y, this.cameraOrientation.z);
        scene.camera().setFoV(this.cameraFov);
        scene.camera().setDof(this.cameraDof);
        scene.camera().setSubjectDistance(this.cameraFocus);

        scene.sun().setAltitude(this.sunAltitude);
        scene.sun().setAzimuth(this.sunAzimuth);
        scene.sun().setIntensity(this.sunIntensity);
        scene.sun().setColor(this.sunColor);
        scene.sun().setDrawTexture(this.sunDraw);

        scene.sky().setCloudsEnabled(this.cloudsEnabled);
        scene.sky().setCloudXOffset(this.cloudOffset.x);
        scene.sky().setCloudYOffset(this.cloudOffset.y);
        scene.sky().setCloudZOffset(this.cloudOffset.z);

        scene.setAnimationTime(this.animationTime);
    }
}
