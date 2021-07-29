package chunkyanimate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.math.Vector3;

import java.util.ArrayList;

public class AnimationFrame {
  public static final Gson parser = new GsonBuilder().setPrettyPrinting().create();

  public Double fogDensity;
  public Double skyFogDensity;
  public Vector3 fogColor = new Vector3();

  public Boolean waterWorldEnabled;
  public Double waterWorldHeight;

  public Vector3 cameraPosition = new Vector3();
  public Vector3 cameraOrientation = new Vector3();
  public Double cameraFov;
  public Double cameraDof;
  public Double cameraFocus;

  public Double sunAltitude;
  public Double sunAzimuth;
  public Double sunIntensity;
  public Vector3 sunColor = new Vector3();
  public Boolean sunDraw;

  public Double animationTime;

  private AnimationFrame() {}

  public static AnimationFrame create(Scene scene, AnimationFrame previousFrame, String json) {
    AnimationFrame ret = parser.fromJson(json, AnimationFrame.class);
    return ret.fillMissing(previousFrame).fillMissing(scene);
  }

  public String toJson() {
    return parser.toJson(this);
  }

  public static ArrayList<AnimationFrame> loadAnimation(Scene scene, String file) {
    JsonArray arr = new JsonParser().parse(file).getAsJsonArray();
    ArrayList<AnimationFrame> ret = new ArrayList<>(arr.size());
    AnimationFrame prev = null;
    for (JsonElement el : arr)
      ret.add(prev = create(scene, prev, el.toString()));
    return ret;
  }

  public void forceFeedTheseSettingsIntoASceneObject(Scene scene) {
    scene.setFogDensity(this.fogDensity);
    scene.setSkyFogDensity(this.skyFogDensity);
    scene.setFogColor(this.fogColor);
    scene.setWaterPlaneEnabled(this.waterWorldEnabled);
    scene.setWaterPlaneHeight(this.waterWorldHeight);
    scene.camera().setView(this.cameraOrientation.z, this.cameraOrientation.x, this.cameraOrientation.y);
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

  private AnimationFrame fillMissing(AnimationFrame other) {
    if (other == null) return this;
    if (this.fogDensity==null) this.fogDensity = other.fogDensity;
    if (this.skyFogDensity==null) this.skyFogDensity = other.skyFogDensity;
    if (this.fogColor==null) this.fogColor = other.fogColor;
    if (this.waterWorldEnabled==null) this.waterWorldEnabled = other.waterWorldEnabled;
    if (this.waterWorldHeight==null) this.waterWorldHeight = other.waterWorldHeight;
    if (this.cameraPosition==null) this.cameraPosition = other.cameraPosition;
    if (this.cameraOrientation==null) this.cameraOrientation = other.cameraOrientation;
    if (this.cameraFov==null) this.cameraFov = other.cameraFov;
    if (this.cameraDof==null) this.cameraDof = other.cameraDof;
    if (this.cameraFocus==null) this.cameraFocus = other.cameraFocus;
    if (this.sunAltitude==null) this.sunAltitude = other.sunAltitude;
    if (this.sunAzimuth==null) this.sunAzimuth = other.sunAzimuth;
    if (this.sunIntensity==null) this.sunIntensity = other.sunIntensity;
    if (this.sunColor==null) this.sunColor = other.sunColor;
    if (this.sunDraw==null) this.sunDraw = other.sunDraw;
    if (this.animationTime==null) this.animationTime = other.animationTime;
    return this;
  }
  private AnimationFrame fillMissing(Scene scene) {
    if (this.fogDensity==null) this.fogDensity = scene.getFogDensity();
    if (this.skyFogDensity==null) this.skyFogDensity = scene.getSkyFogDensity();
    if (this.fogColor==null) this.fogColor = scene.getFogColor();
    if (this.waterWorldEnabled==null) this.waterWorldEnabled = scene.isWaterPlaneEnabled();
    if (this.waterWorldHeight==null) this.waterWorldHeight = scene.getWaterPlaneHeight();
    if (this.cameraPosition==null) this.cameraPosition = scene.camera().getPosition();
    if (this.cameraOrientation==null) this.cameraOrientation = new Vector3(scene.camera().getPitch(),scene.camera().getRoll(),scene.camera().getYaw());
    if (this.cameraFov==null) this.cameraFov = scene.camera().getFov();
    if (this.cameraDof==null) this.cameraDof = scene.camera().getDof();
    if (this.cameraFocus==null) this.cameraFocus = scene.camera().getSubjectDistance();
    if (this.sunAltitude==null) this.sunAltitude = scene.sun().getAltitude();
    if (this.sunAzimuth==null) this.sunAzimuth = scene.sun().getAzimuth();
    if (this.sunIntensity==null) this.sunIntensity = scene.sun().getIntensity();
    if (this.sunColor==null) this.sunColor = scene.sun().getColor();
    if (this.sunDraw==null) this.sunDraw = scene.sun().drawTexture();
    if (this.animationTime==null) this.animationTime = scene.getAnimationTime();
    return this;
  }
}
