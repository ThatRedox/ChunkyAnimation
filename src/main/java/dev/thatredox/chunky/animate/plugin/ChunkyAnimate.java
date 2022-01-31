package dev.thatredox.chunky.animate.plugin;

import se.llbit.chunky.Plugin;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.main.ChunkyOptions;
import se.llbit.chunky.renderer.PathTracingRenderer;
import se.llbit.chunky.renderer.RenderMode;
import se.llbit.chunky.renderer.RenderStatusListener;
import se.llbit.chunky.renderer.scene.PreviewRayTracer;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.ui.ChunkyFx;
import se.llbit.chunky.ui.render.RenderControlsTabTransformer;

public class ChunkyAnimate implements Plugin {
    private final AnimationManager manager = new AnimationManager();

    @Override
    public void attach(Chunky chunky) {
        manager.setChunky(chunky);

        Chunky.addRenderer(new PathTracingRenderer("AnimationPreview", "AnimationPreview", "AnimationPreviewRenderer", new PreviewRayTracer()));

        Scene scene = chunky.getSceneManager().getScene();
        chunky.getRenderController().getRenderManager().addRenderListener(new RenderStatusListener() {
            @Override
            public void setRenderTime(long time) {}

            @Override
            public void setSamplesPerSecond(int sps) {}

            @Override
            public void setSpp(int spp) {
                if (spp >= scene.getTargetSpp()) {
                    manager.frameComplete();
                }
            }

            @Override
            public void renderStateChanged(RenderMode state) {}
        });

        RenderControlsTabTransformer prev = chunky.getRenderControlsTabTransformer();
        chunky.setRenderControlsTabTransformer(tabs -> {
            tabs = prev.apply(tabs);
            tabs.add(new AnimationTab(manager));
            tabs.add(new KeyFrameTab(manager));
            return tabs;
        });
    }

    public static void main(String[] args) {
        Chunky.loadDefaultTextures();
        Chunky chunky = new Chunky(ChunkyOptions.getDefaults());
        new ChunkyAnimate().attach(chunky);
        ChunkyFx.startChunkyUI(chunky);
    }
}
