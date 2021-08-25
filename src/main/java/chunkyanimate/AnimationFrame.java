package chunkyanimate;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.JsonObject;
import se.llbit.math.Vector3;

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
                jsonCamera.get("orientation").asObject().get("roll").asDouble(prev.cameraOrientation.x),
                jsonCamera.get("orientation").asObject().get("pitch").asDouble(prev.cameraOrientation.y),
                jsonCamera.get("orientation").asObject().get("yaw").asDouble(prev.cameraOrientation.z)
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

        this.animationTime = json.get("animationTime").asDouble(prev.animationTime);
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

        scene.setAnimationTime(this.animationTime);
    }
}
