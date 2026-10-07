/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.hit.HitResult$net.minecraft.util.hit.HitResult$Type
 *  net.minecraft.particle.ParticleEffect
 *  net.minecraft.particle.ParticleTypes
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.client.render.BufferRenderer
 *  net.minecraft.client.render.BufferBuilder
 *  net.minecraft.client.render.Tessellator
 *  net.minecraft.client.render.VertexFormats
 *  net.minecraft.client.render.VertexFormat$net.minecraft.client.render.VertexFormat$DrawMode
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.world.RaycastContext
 *  net.minecraft.world.RaycastContext$net.minecraft.util.hit.EntityHitResult
 *  net.minecraft.world.RaycastContext$net.minecraft.world.RaycastContext$ShapeType
 *  net.minecraft.util.hit.BlockHitResult
 *  net.minecraft.client.render.Camera
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.client.world.ClientWorld
 *  net.minecraft.client.network.ClientPlayerEntity
 *  net.minecraft.client.render.GameRenderer
 *  com.mojang.blaze3d.systems.VertexSorter
 *  net.minecraft.client.render.BuiltBuffer
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package com.cobbleverse.retypedgyms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.render.BuiltBuffer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class SpawnRevealEffect {
    private static final float TOTAL_DURATION = 4.2f;
    private static final float RING_STAGGER = 0.55f;
    private static final float RING_TRAVEL = 3.2f;
    private static final float FADE_START = 2.0f;
    private static final float FADE_END = 3.5f;
    private static final int RING_COUNT = 4;
    private static final float MAX_RADIUS = 54.0f;
    private static final float RIBBON_WIDTH = 0.75f;
    private static final float CURTAIN_HEIGHT = 0.65f;
    private static final float TENDRIL_DURATION = 120.0f;
    private static final float TENDRIL_FADE_IN = 1.5f;
    private static final float TENDRIL_FADE_OUT = 6.0f;
    private static final float TENDRIL_MAX_LENGTH = 9.5f;
    private static final int TENDRIL_SEGMENTS = 30;
    private static final int TENDRIL_COUNT = 4;
    private static final float TENDRIL_BASE_WIDTH = 0.2f;
    private static final float TENDRIL_TIP_WIDTH = 0.035f;
    private static boolean active = false;
    private static long startMs = 0L;
    private static int lastSoundIndex = -1;
    private static boolean tendrilActive = false;
    private static long tendrilStartMs = 0L;
    private static double originX = 0.0;
    private static double originY = 0.0;
    private static double originZ = 0.0;

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SpawnRevealEffect::renderWorld);
    }

    public static void trigger() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null) {
            originX = mc.player.getX();
            originY = mc.player.getY();
            originZ = mc.player.getZ();
        }
        active = true;
        startMs = System.currentTimeMillis();
        lastSoundIndex = -1;
        tendrilActive = false;
    }

    private static void renderWorld(WorldRenderContext context) {
        float blackAlpha;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) {
            return;
        }
        net.minecraft.client.render.Camera camera = context.camera();
        net.minecraft.util.math.Vec3d camPos = context.camera().getPos();
        net.minecraft.client.util.math.MatrixStack ps = context.matrixStack();
        Matrix4f cameraMatrix = ps.peek().getPositionMatrix();
        if (tendrilActive) {
            SpawnRevealEffect.renderTendrils(context.camera(), cameraMatrix, camPos, mc.world, mc.player, mc);
        }
        if (!active) {
            return;
        }
        float t = (float)(System.currentTimeMillis() - startMs) / 1000.0f;
        if (t >= 4.2f) {
            active = false;
            return;
        }
        net.minecraft.client.network.ClientPlayerEntity player = mc.player;
        net.minecraft.client.world.ClientWorld level = mc.world;
        if (t >= 2.0f && !tendrilActive) {
            tendrilActive = true;
            tendrilStartMs = System.currentTimeMillis();
        }
        if (t < 2.0f) {
            blackAlpha = 0.94f;
        } else if (t < 3.5f) {
            float f = (t - 2.0f) / 1.5f;
            float inv = 1.0f - f;
            blackAlpha = 0.94f * (inv * inv);
        } else {
            blackAlpha = 0.0f;
        }
        if (blackAlpha > 0.01f) {
            SpawnRevealEffect.renderWorldBlackout(blackAlpha);
        }
        net.minecraft.client.render.Tessellator tess = net.minecraft.client.render.Tessellator.getInstance();
        int currentRingLaunch = (int)(t / 0.55f);
        if (currentRingLaunch > lastSoundIndex && currentRingLaunch < 4) {
            lastSoundIndex = currentRingLaunch;
            player.playSound(net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f + (float)currentRingLaunch * 0.15f);
        }
        RenderSystem.disableCull();
        for (int ri = 0; ri < 4; ++ri) {
            List<RingPoint> points;
            float prog;
            float e;
            float currentRadius;
            float ringT = t - (float)ri * 0.55f;
            if (ringT <= 0.0f || (currentRadius = (e = 1.0f - (float)Math.pow(1.0 - (double)(prog = Math.min(1.0f, ringT / 3.2f)), 3.0)) * 54.0f) < 0.1f) continue;
            float life = 1.0f - prog;
            float fadeIn = Math.min(1.0f, prog / 0.08f);
            float alphaFactor = fadeIn * (life * life);
            int baseAlpha = (int)(alphaFactor * 255.0f);
            if (baseAlpha <= 2 || (points = SpawnRevealEffect.sampleRingPoints(level, player, currentRadius)).size() < 3) continue;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getPositionColorProgram);
            int sonarAlpha = (int)((float)baseAlpha * 0.35f);
            if (sonarAlpha > 2) {
                SpawnRevealEffect.renderGroundRibbon(tess, cameraMatrix, camPos, points, 0.75f, 0, 220, 255, sonarAlpha);
                SpawnRevealEffect.renderEnergyCurtain(tess, cameraMatrix, camPos, points, 0.48749998f, 0, 200, 255, (int)((float)sonarAlpha * 0.6f));
            }
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)false);
            SpawnRevealEffect.renderGroundRibbon(tess, cameraMatrix, camPos, points, 0.75f, 0, 245, 255, baseAlpha);
            SpawnRevealEffect.renderEnergyCurtain(tess, cameraMatrix, camPos, points, 0.65f, 0, 240, 255, (int)((float)baseAlpha * 0.85f));
            SpawnRevealEffect.renderCoreRibbon(tess, cameraMatrix, camPos, points, 220, 255, 255, baseAlpha);
            RenderSystem.depthMask((boolean)true);
            RenderSystem.disableBlend();
            if ((mc.inGameHud.getTicks() + ri) % 3 != 0 || !(prog < 0.85f)) continue;
            SpawnRevealEffect.spawnRingParticles(level, points);
        }
        if (t < 0.5f) {
            SpawnRevealEffect.renderOriginFlash(tess, cameraMatrix, camPos, t / 0.5f);
        }
        RenderSystem.enableCull();
    }

    private static void renderWorldBlackout(float alpha) {
        Matrix4f savedProj = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        Matrix4f invModelView = new Matrix4f((Matrix4fc)RenderSystem.getModelViewMatrix()).invert();
        Matrix4f identityProj = new Matrix4f();
        RenderSystem.setProjectionMatrix((Matrix4f)identityProj, (com.mojang.blaze3d.systems.VertexSorter)com.mojang.blaze3d.systems.VertexSorter.BY_Z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getPositionColorProgram);
        net.minecraft.client.render.Tessellator tess = net.minecraft.client.render.Tessellator.getInstance();
        net.minecraft.client.render.BufferBuilder buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        int a = (int)(alpha * 255.0f);
        buf.vertex(invModelView, -1.0f, -1.0f, 0.0f).color(0, 0, 0, a);
        buf.vertex(invModelView, 1.0f, -1.0f, 0.0f).color(0, 0, 0, a);
        buf.vertex(invModelView, 1.0f, 1.0f, 0.0f).color(0, 0, 0, a);
        buf.vertex(invModelView, -1.0f, 1.0f, 0.0f).color(0, 0, 0, a);
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram((net.minecraft.client.render.BuiltBuffer)buf.end());
        RenderSystem.setProjectionMatrix((Matrix4f)savedProj, (com.mojang.blaze3d.systems.VertexSorter)com.mojang.blaze3d.systems.VertexSorter.BY_DISTANCE);
        RenderSystem.enableCull();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static List<RingPoint> sampleRingPoints(net.minecraft.client.world.ClientWorld level, net.minecraft.client.network.ClientPlayerEntity player, float radius) {
        int steps = Math.max(48, Math.min(144, (int)(radius * 7.0f)));
        ArrayList<RingPoint> points = new ArrayList<RingPoint>(steps + 1);
        double startYOffset = Math.max(16.0, (double)radius * 0.45);
        double endYOffset = -30.0;
        for (int i = 0; i <= steps; ++i) {
            net.minecraft.util.math.Vec3d rayEnd;
            double sin;
            double z;
            double angle = (double)i * 2.0 * Math.PI / (double)steps;
            double cos = Math.cos(angle);
            double x = originX + (double)radius * cos;
            net.minecraft.util.math.Vec3d rayStart = new net.minecraft.util.math.Vec3d(x, originY + startYOffset, z = originZ + (double)radius * (sin = Math.sin(angle)));
            net.minecraft.world.RaycastContext ctx = new net.minecraft.world.RaycastContext(rayStart, rayEnd = new net.minecraft.util.math.Vec3d(x, originY + endYOffset, z), net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, (net.minecraft.entity.Entity)player);
            net.minecraft.util.hit.BlockHitResult hit = level.raycast(ctx);
            double y = hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK ? hit.getPos().y + 0.08 : originY + 0.08;
            points.add(new RingPoint(x, y, z, cos, sin));
        }
        return points;
    }

    private static void renderGroundRibbon(net.minecraft.client.render.Tessellator tess, Matrix4f matrix, net.minecraft.util.math.Vec3d camPos, List<RingPoint> points, float width, int r, int g, int b, int a) {
        if (points.size() < 2) {
            return;
        }
        net.minecraft.client.render.BufferBuilder buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        float halfW = width * 0.5f;
        for (int i = 0; i < points.size() - 1; ++i) {
            RingPoint p1 = points.get(i);
            RingPoint p2 = points.get(i + 1);
            float o1x = (float)(p1.x + p1.nx * (double)halfW - camPos.x);
            float o1y = (float)(p1.y - camPos.y);
            float o1z = (float)(p1.z + p1.nz * (double)halfW - camPos.z);
            float o2x = (float)(p2.x + p2.nx * (double)halfW - camPos.x);
            float o2y = (float)(p2.y - camPos.y);
            float o2z = (float)(p2.z + p2.nz * (double)halfW - camPos.z);
            float i2x = (float)(p2.x - p2.nx * (double)halfW - camPos.x);
            float i2y = (float)(p2.y - camPos.y);
            float i2z = (float)(p2.z - p2.nz * (double)halfW - camPos.z);
            float i1x = (float)(p1.x - p1.nx * (double)halfW - camPos.x);
            float i1y = (float)(p1.y - camPos.y);
            float i1z = (float)(p1.z - p1.nz * (double)halfW - camPos.z);
            buf.vertex(matrix, o1x, o1y, o1z).color(r, g, b, a);
            buf.vertex(matrix, o2x, o2y, o2z).color(r, g, b, a);
            buf.vertex(matrix, i2x, i2y, i2z).color(r, g, b, (int)((float)a * 0.8f));
            buf.vertex(matrix, i1x, i1y, i1z).color(r, g, b, (int)((float)a * 0.8f));
        }
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram((net.minecraft.client.render.BuiltBuffer)buf.end());
    }

    private static void renderEnergyCurtain(net.minecraft.client.render.Tessellator tess, Matrix4f matrix, net.minecraft.util.math.Vec3d camPos, List<RingPoint> points, float height, int r, int g, int b, int a) {
        if (points.size() < 2) {
            return;
        }
        net.minecraft.client.render.BufferBuilder buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        for (int i = 0; i < points.size() - 1; ++i) {
            RingPoint p1 = points.get(i);
            RingPoint p2 = points.get(i + 1);
            float x1 = (float)(p1.x - camPos.x);
            float y1 = (float)(p1.y - camPos.y);
            float z1 = (float)(p1.z - camPos.z);
            float x2 = (float)(p2.x - camPos.x);
            float y2 = (float)(p2.y - camPos.y);
            float z2 = (float)(p2.z - camPos.z);
            buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
            buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
            buf.vertex(matrix, x2, y2 + height, z2).color(r, g, b, 0);
            buf.vertex(matrix, x1, y1 + height, z1).color(r, g, b, 0);
        }
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram((net.minecraft.client.render.BuiltBuffer)buf.end());
    }

    private static void renderCoreRibbon(net.minecraft.client.render.Tessellator tess, Matrix4f matrix, net.minecraft.util.math.Vec3d camPos, List<RingPoint> points, int r, int g, int b, int a) {
        if (points.size() < 2) {
            return;
        }
        net.minecraft.client.render.BufferBuilder buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        float halfW = 0.08f;
        for (int i = 0; i < points.size() - 1; ++i) {
            RingPoint p1 = points.get(i);
            RingPoint p2 = points.get(i + 1);
            float o1x = (float)(p1.x + p1.nx * (double)halfW - camPos.x);
            float o1y = (float)(p1.y + 0.02 - camPos.y);
            float o1z = (float)(p1.z + p1.nz * (double)halfW - camPos.z);
            float o2x = (float)(p2.x + p2.nx * (double)halfW - camPos.x);
            float o2y = (float)(p2.y + 0.02 - camPos.y);
            float o2z = (float)(p2.z + p2.nz * (double)halfW - camPos.z);
            float i2x = (float)(p2.x - p2.nx * (double)halfW - camPos.x);
            float i2y = (float)(p2.y + 0.02 - camPos.y);
            float i2z = (float)(p2.z - p2.nz * (double)halfW - camPos.z);
            float i1x = (float)(p1.x - p1.nx * (double)halfW - camPos.x);
            float i1y = (float)(p1.y + 0.02 - camPos.y);
            float i1z = (float)(p1.z - p1.nz * (double)halfW - camPos.z);
            buf.vertex(matrix, o1x, o1y, o1z).color(r, g, b, a);
            buf.vertex(matrix, o2x, o2y, o2z).color(r, g, b, a);
            buf.vertex(matrix, i2x, i2y, i2z).color(r, g, b, a);
            buf.vertex(matrix, i1x, i1y, i1z).color(r, g, b, a);
        }
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram((net.minecraft.client.render.BuiltBuffer)buf.end());
    }

    private static void renderOriginFlash(net.minecraft.client.render.Tessellator tess, Matrix4f matrix, net.minecraft.util.math.Vec3d camPos, float progress) {
        float life = 1.0f - progress;
        int alpha = (int)(life * life * 220.0f);
        if (alpha <= 2) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getPositionColorProgram);
        net.minecraft.client.render.BufferBuilder buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        float flashR = progress * 4.0f;
        int spokes = 24;
        float ox = (float)(originX - camPos.x);
        float oy = (float)(originY + 0.1 - camPos.y);
        float oz = (float)(originZ - camPos.z);
        for (int i = 0; i < spokes; ++i) {
            double a1 = (double)i * 2.0 * Math.PI / (double)spokes;
            double a2 = (double)(i + 1) * 2.0 * Math.PI / (double)spokes;
            float x1 = (float)((double)ox + Math.cos(a1) * (double)flashR);
            float z1 = (float)((double)oz + Math.sin(a1) * (double)flashR);
            float x2 = (float)((double)ox + Math.cos(a2) * (double)flashR);
            float z2 = (float)((double)oz + Math.sin(a2) * (double)flashR);
            buf.vertex(matrix, ox, oy, oz).color(0, 240, 255, alpha);
            buf.vertex(matrix, ox, oy, oz).color(0, 240, 255, alpha);
            buf.vertex(matrix, x2, oy, z2).color(0, 240, 255, 0);
            buf.vertex(matrix, x1, oy, z1).color(0, 240, 255, 0);
        }
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram((net.minecraft.client.render.BuiltBuffer)buf.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void spawnRingParticles(net.minecraft.client.world.ClientWorld level, List<RingPoint> points) {
        int count = Math.min(10, points.size() / 5);
        int step = points.size() / count;
        for (int i = 0; i < points.size(); i += step) {
            RingPoint pt = points.get(i);
            level.addParticle((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.GLOW, pt.x, pt.y + 0.2, pt.z, 0.0, 0.02, 0.0);
            if (!(Math.random() < 0.35)) continue;
            level.addParticle((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y + 0.15, pt.z, 0.0, 0.01, 0.0);
        }
    }

    private static void renderTendrils(net.minecraft.client.render.Camera camera, Matrix4f matrix, net.minecraft.util.math.Vec3d camPos, net.minecraft.client.world.ClientWorld level, net.minecraft.client.network.ClientPlayerEntity player, net.minecraft.client.MinecraftClient mc) {
        float alpha;
        float f;
        float t = (float)(System.currentTimeMillis() - tendrilStartMs) / 1000.0f;
        if (t >= 120.0f) {
            tendrilActive = false;
            return;
        }
        if (t < 1.5f) {
            f = t / 1.5f;
            alpha = f * f;
        } else if (t > 114.0f) {
            f = (120.0f - t) / 6.0f;
            alpha = f * f * f;
        } else {
            alpha = 1.0f;
        }
        net.minecraft.client.render.Tessellator tess = net.minecraft.client.render.Tessellator.getInstance();
        RenderSystem.disableCull();
        for (int ti = 0; ti < 4; ++ti) {
            int si;
            float baseAngle = (float)((double)ti * Math.PI * 0.5);
            ArrayList<double[]> seg = new ArrayList<double[]>(31);
            for (si = 0; si <= 30; ++si) {
                float frac = (float)si / 30.0f;
                float dist = frac * 9.5f;
                float wiggleAmp = frac * frac * 1.6f;
                float w1 = (float)(Math.sin((double)t * 2.1 + (double)si * 0.5 + (double)ti * 2.0) * (double)wiggleAmp);
                float w2 = (float)(Math.sin((double)t * 1.35 - (double)si * 0.38 + (double)ti * 1.55) * (double)wiggleAmp * (double)0.45f);
                float wiggle = (w1 + w2) * 0.75f;
                float perpAngle = baseAngle + 1.5707964f;
                double sx = originX + (double)dist * Math.cos(baseAngle) + (double)wiggle * Math.cos(perpAngle);
                double sz = originZ + (double)dist * Math.sin(baseAngle) + (double)wiggle * Math.sin(perpAngle);
                double sy = SpawnRevealEffect.getTendrilGroundY(level, player, sx, sz);
                seg.add(new double[]{sx, sy, sz});
            }
            if ((mc.inGameHud.getTicks() + ti * 7) % 8 == 0) {
                for (si = 24; si <= 30; ++si) {
                    if (!(Math.random() < 0.15)) continue;
                    double[] pt = (double[])seg.get(si);
                    level.addParticle((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, pt[0], pt[1] + 0.1, pt[2], (Math.random() - 0.5) * 0.05, 0.04, (Math.random() - 0.5) * 0.05);
                }
            }
            SpawnRevealEffect.renderTendrilRibbon(tess, matrix, camPos, seg, alpha, t, ti);
        }
        RenderSystem.enableCull();
    }

    private static double getTendrilGroundY(net.minecraft.client.world.ClientWorld level, net.minecraft.client.network.ClientPlayerEntity player, double x, double z) {
        net.minecraft.util.math.Vec3d rayStart = new net.minecraft.util.math.Vec3d(x, originY + 12.0, z);
        net.minecraft.util.math.Vec3d rayEnd = new net.minecraft.util.math.Vec3d(x, originY - 20.0, z);
        net.minecraft.world.RaycastContext ctx = new net.minecraft.world.RaycastContext(rayStart, rayEnd, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, (net.minecraft.entity.Entity)player);
        net.minecraft.util.hit.BlockHitResult hit = level.raycast(ctx);
        if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            return hit.getPos().y + 0.06;
        }
        return originY + 0.06;
    }

    private static void renderTendrilRibbon(net.minecraft.client.render.Tessellator tess, Matrix4f matrix, net.minecraft.util.math.Vec3d camPos, List<double[]> seg, float alpha, float t, int ti) {
        float pulse2;
        float pulse1;
        float hw2;
        float hw1;
        float w2;
        float w1;
        double rz;
        double rx;
        double flen;
        double fdz;
        double fdx;
        float frac2;
        float frac1;
        double[] p2;
        double[] p1;
        int i;
        if (seg.size() < 2) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getPositionColorProgram);
        net.minecraft.client.render.BufferBuilder buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        for (i = 0; i < seg.size() - 1; ++i) {
            p1 = seg.get(i);
            p2 = seg.get(i + 1);
            frac1 = (float)i / 30.0f;
            frac2 = (float)(i + 1) / 30.0f;
            fdx = p2[0] - p1[0];
            fdz = p2[2] - p1[2];
            flen = Math.sqrt(fdx * fdx + fdz * fdz);
            if (flen < 1.0E-6) {
                fdx = 1.0;
                fdz = 0.0;
                flen = 1.0;
            }
            rx = -fdz / flen;
            rz = fdx / flen;
            w1 = 0.2f + -0.165f * frac1;
            w2 = 0.2f + -0.165f * frac2;
            hw1 = w1 * 0.5f;
            hw2 = w2 * 0.5f;
            pulse1 = (float)(Math.sin((double)t * 3.5 - (double)frac1 * 10.0 + (double)ti * 1.5) * 0.3 + 0.7);
            pulse2 = (float)(Math.sin((double)t * 3.5 - (double)frac2 * 10.0 + (double)ti * 1.5) * 0.3 + 0.7);
            int sa1 = (int)(alpha * 55.0f * pulse1);
            int sa2 = (int)(alpha * 55.0f * pulse2);
            float o1x = (float)(p1[0] + rx * (double)hw1 - camPos.x);
            float o1y = (float)(p1[1] - camPos.y);
            float o1z = (float)(p1[2] + rz * (double)hw1 - camPos.z);
            float o2x = (float)(p2[0] + rx * (double)hw2 - camPos.x);
            float o2y = (float)(p2[1] - camPos.y);
            float o2z = (float)(p2[2] + rz * (double)hw2 - camPos.z);
            float i2x = (float)(p2[0] - rx * (double)hw2 - camPos.x);
            float i2z = (float)(p2[2] - rz * (double)hw2 - camPos.z);
            float i1x = (float)(p1[0] - rx * (double)hw1 - camPos.x);
            float i1z = (float)(p1[2] - rz * (double)hw1 - camPos.z);
            buf.vertex(matrix, o1x, o1y, o1z).color(0, 200, 255, sa1);
            buf.vertex(matrix, o2x, o2y, o2z).color(0, 200, 255, sa2);
            buf.vertex(matrix, i2x, o2y, i2z).color(0, 200, 255, sa2);
            buf.vertex(matrix, i1x, o1y, i1z).color(0, 200, 255, sa1);
        }
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram((net.minecraft.client.render.BuiltBuffer)buf.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        for (i = 0; i < seg.size() - 1; ++i) {
            p1 = seg.get(i);
            p2 = seg.get(i + 1);
            frac1 = (float)i / 30.0f;
            frac2 = (float)(i + 1) / 30.0f;
            fdx = p2[0] - p1[0];
            fdz = p2[2] - p1[2];
            flen = Math.sqrt(fdx * fdx + fdz * fdz);
            if (flen < 1.0E-6) {
                fdx = 1.0;
                fdz = 0.0;
                flen = 1.0;
            }
            rx = -fdz / flen;
            rz = fdx / flen;
            w1 = 0.2f + -0.165f * frac1;
            w2 = 0.2f + -0.165f * frac2;
            hw1 = w1 * 0.5f;
            hw2 = w2 * 0.5f;
            pulse1 = (float)(Math.sin((double)t * 4.0 - (double)frac1 * 10.0 + (double)ti * 1.5) * 0.5 + 0.5);
            pulse2 = (float)(Math.sin((double)t * 4.0 - (double)frac2 * 10.0 + (double)ti * 1.5) * 0.5 + 0.5);
            float tipFade1 = 1.0f - frac1 * 0.65f;
            float tipFade2 = 1.0f - frac2 * 0.65f;
            int r1 = (int)(pulse1 * 160.0f);
            int g1 = (int)(200.0f + pulse1 * 55.0f);
            int r2 = (int)(pulse2 * 160.0f);
            int g2 = (int)(200.0f + pulse2 * 55.0f);
            int a1 = (int)(alpha * tipFade1 * (120.0f + pulse1 * 135.0f));
            int a2 = (int)(alpha * tipFade2 * (120.0f + pulse2 * 135.0f));
            float o1x = (float)(p1[0] + rx * (double)hw1 - camPos.x);
            float o1y = (float)(p1[1] - camPos.y);
            float o1z = (float)(p1[2] + rz * (double)hw1 - camPos.z);
            float o2x = (float)(p2[0] + rx * (double)hw2 - camPos.x);
            float o2y = (float)(p2[1] - camPos.y);
            float o2z = (float)(p2[2] + rz * (double)hw2 - camPos.z);
            float i2x = (float)(p2[0] - rx * (double)hw2 - camPos.x);
            float i2z = (float)(p2[2] - rz * (double)hw2 - camPos.z);
            float i1x = (float)(p1[0] - rx * (double)hw1 - camPos.x);
            float i1z = (float)(p1[2] - rz * (double)hw1 - camPos.z);
            buf.vertex(matrix, o1x, o1y, o1z).color(r1, g1, 255, a1);
            buf.vertex(matrix, o2x, o2y, o2z).color(r2, g2, 255, a2);
            buf.vertex(matrix, i2x, o2y, i2z).color(r2, g2, 255, (int)((float)a2 * 0.75f));
            buf.vertex(matrix, i1x, o1y, i1z).color(r1, g1, 255, (int)((float)a1 * 0.75f));
        }
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram((net.minecraft.client.render.BuiltBuffer)buf.end());
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
    }

    private static class RingPoint {
        final double x;
        final double y;
        final double z;
        final double nx;
        final double nz;

        RingPoint(double x, double y, double z, double nx, double nz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.nz = nz;
        }
    }
}

