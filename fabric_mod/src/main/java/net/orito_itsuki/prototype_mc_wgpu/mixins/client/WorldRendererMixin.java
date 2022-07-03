package net.orito_itsuki.prototype_mc_wgpu.mixins.client;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.orito_itsuki.prototype_mc_wgpu.mixins_impl.client.WorldRendererMixinImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(at = @At("TAIL"), method = "render")
    private void onRender(
            MatrixStack matrixStack,
            float f,
            long l,
            boolean bl,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f matrix4f,
            CallbackInfo ci) {
        WorldRendererMixinImpl.INSTANCE.onRenderer((WorldRenderer)(Object)this, matrixStack, f, l, bl, camera, gameRenderer, lightmapTextureManager, matrix4f, ci);
    }
}
