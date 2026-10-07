/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.client.sound.PositionedSoundInstance
 *  net.minecraft.client.sound.SoundInstance
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.text.Text
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.sound.SoundEvent
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.ingame.InventoryScreen
 *  net.minecraft.registry.entry.RegistryEntry
 */
package com.cobbleverse.retypedgyms.gui;

import com.cobbleverse.retypedgyms.client.RetypedGymsClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.registry.entry.RegistryEntry;

@Environment(value=EnvType.CLIENT)
public class WelcomeScreen
extends net.minecraft.client.gui.screen.Screen {
    private State state = State.WELCOME_PANEL;
    private static final int PANEL_W = 430;
    private static final int PANEL_H = 340;
    private int panelX;
    private int panelY;
    private static final int BTN_W = 148;
    private static final int BTN_H = 36;
    private int btnX;
    private int btnY;
    private boolean btnHovered = false;
    private long startTime;
    private long cinematicStartTime = 0L;
    private int lastSoundStage = -1;
    private static final String[] FEATURES = new String[]{"Ametheria Realm  exclusive custom gym challenges", "Kanto 8 Gyms  retyped & randomised on every restart", "Johto 8 Gyms  with full themed Pok\u00e9mon pools", "Elite Four  NEWLY ADDED TEAMS (RANDOMIZED)", "Signature Megas  every leader carries an ace Mega", "Laser Fakemon  fan-made Megas fully supported", "Auto re-roll  teams change each server restart"};
    private static final int[][] TAG_RGB = new int[][]{{192, 96, 255}, {91, 140, 255}, {76, 175, 80}, {255, 128, 0}, {255, 215, 0}, {255, 96, 160}, {144, 208, 255}};
    private static final int SPARK_COUNT = 24;
    private final float[] sparkAngle = new float[24];
    private final float[] sparkDist = new float[24];
    private final float[] sparkSpeed = new float[24];
    private final float[] sparkPhase = new float[24];
    private final float[] sparkSize = new float[24];

    public WelcomeScreen() {
        super((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"Ametheria \u00b7 Retyped Gyms"));
        this.startTime = System.currentTimeMillis();
        for (int i = 0; i < 24; ++i) {
            this.sparkAngle[i] = (float)(Math.random() * Math.PI * 2.0);
            this.sparkDist[i] = 0.6f + (float)(Math.random() * 1.4);
            this.sparkSpeed[i] = 0.4f + (float)(Math.random() * 0.8);
            this.sparkPhase[i] = (float)(Math.random() * Math.PI * 2.0);
            this.sparkSize[i] = 1.5f + (float)(Math.random() * 2.0);
        }
    }

    protected void init() {
        this.panelX = (this.width - 430) / 2;
        this.panelY = (this.height - 340) / 2;
        this.btnX = this.panelX + 141;
        this.btnY = this.panelY + 340 - 58;
    }

    public void render(net.minecraft.client.gui.DrawContext g, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float t = (float)(now - this.startTime) / 1000.0f;
        if (this.state == State.WELCOME_PANEL) {
            this.btnHovered = mouseX >= this.btnX && mouseX < this.btnX + 148 && mouseY >= this.btnY && mouseY < this.btnY + 36;
            this.drawVignette(g);
            this.drawPanel(g, t);
            this.drawTitle(g, t);
            this.drawDivider(g);
            this.drawBullets(g);
            this.drawEnterButton(g, t);
        } else {
            float ct = (float)(now - this.cinematicStartTime) / 1000.0f;
            if (ct >= 4.6f) {
                this.close();
                return;
            }
            this.renderAuraCinematic(g, t, ct);
        }
    }

    private void renderAuraCinematic(net.minecraft.client.gui.DrawContext g, float t, float ct) {
        this.checkSoundCues(ct);
        float pulse = this.getHeartbeatPulse(ct);
        float shakeMag = ct >= 3.8f ? (ct - 3.8f) * 6.0f : pulse * 2.8f;
        float shakeX = (float)(Math.sin((double)ct * 70.0) * (double)shakeMag);
        float shakeY = (float)(Math.cos((double)ct * 55.0) * (double)shakeMag);
        float cx = (float)this.width * 0.5f + shakeX;
        float cy = (float)this.height * 0.5f + shakeY;
        float baseR = Math.min((float)this.height * 0.26f, 70.0f);
        float scaleIn = (float)Math.min(1.0, Math.sin(Math.min(1.0, Math.max(0.0, ((double)ct - 0.2) / 1.0)) * Math.PI * 0.5));
        float currentR = baseR * scaleIn * (1.0f + pulse * 0.06f);
        float playerX = cx - 110.0f;
        float playerY = cy + 50.0f;
        float floorX = playerX;
        float floorY = playerY + 2.0f;
        float sphereX = cx + 52.0f;
        float floatBob = (float)Math.sin((double)ct * 1.8) * 5.0f;
        float sphereY = cy - 10.0f + floatBob;
        this.drawVoidBackground(g, cx, cy, ct, pulse);
        if (ct < 0.65f) {
            this.drawDissolvingPanel(g, t, ct);
        }
        if (scaleIn > 0.08f) {
            this.drawSummoningCircle(g, floorX, floorY, ct, pulse, scaleIn);
        }
        if (scaleIn > 0.12f) {
            this.drawPlayerTendrils(g, playerX, playerY, ct, pulse, scaleIn, false);
        }
        if (scaleIn > 0.1f) {
            this.drawSonicReverberations(g, sphereX, sphereY, currentR, ct, pulse);
        }
        if (scaleIn > 0.35f) {
            this.drawOrbitingChromeRings(g, sphereX, sphereY, currentR, ct, false);
        }
        this.drawPlayerAvatar(g, playerX, playerY, sphereX, sphereY, ct, scaleIn);
        if (scaleIn > 0.12f) {
            this.drawPlayerTendrils(g, playerX, playerY, ct, pulse, scaleIn, true);
        }
        if (currentR > 3.0f) {
            this.drawDarkChromeSphere(g, sphereX, sphereY, currentR, t, ct, pulse);
        }
        if (scaleIn > 0.35f) {
            this.drawOrbitingChromeRings(g, sphereX, sphereY, currentR, ct, true);
        }
        if (scaleIn > 0.25f) {
            this.drawSoulTendrils(g, sphereX, sphereY, playerX, playerY, currentR, t, ct, pulse);
        }
        if (scaleIn > 0.2f) {
            this.drawSoulSparks(g, sphereX, sphereY, currentR, t, ct);
        }
        this.drawHolographicHUD(g, cx, cy, currentR, ct);
        this.drawLetterboxBars(g, ct);
        if (ct >= 3.7f) {
            this.drawSupernovaFlare(g, cx, cy, ct);
        }
    }

    private float getHeartbeatPulse(float ct) {
        float p1 = 0.0f;
        if (ct >= 1.5f && ct < 2.1f) {
            float dt = ct - 1.5f;
            p1 = (float)(Math.exp((double)(-dt) * 8.0) * Math.sin((double)dt * 20.0));
        }
        float p2 = 0.0f;
        if (ct >= 2.7f && ct < 3.3f) {
            float dt = ct - 2.7f;
            p2 = (float)(Math.exp((double)(-dt) * 8.0) * Math.sin((double)dt * 22.0));
        }
        return Math.max(0.0f, Math.max(p1, p2));
    }

    private void drawVoidBackground(net.minecraft.client.gui.DrawContext g, float cx, float cy, float ct, float pulse) {
        g.fill(0, 0, this.width, this.height, -16645112);
        int steps = 14;
        float maxDist = (float)Math.max(this.width, this.height) * 0.75f;
        for (int i = steps; i >= 1; --i) {
            float frac = (float)i / (float)steps;
            int r = (int)(maxDist * frac);
            int alpha = (int)((22.0f - (float)i * 1.3f) * (0.8f + 0.2f * pulse));
            if (alpha <= 0) continue;
            int color = alpha << 24 | 0x2B36;
            WelcomeScreen.drawCircleFill(g, (int)cx, (int)cy, r, color, 3);
        }
    }

    private void drawDissolvingPanel(net.minecraft.client.gui.DrawContext g, float t, float ct) {
        float f = ct / 0.65f;
        float alpha = 1.0f - f;
        int a = (int)(alpha * 255.0f);
        if (a <= 0) {
            return;
        }
        int shrink = (int)(f * 120.0f);
        int px = this.panelX + shrink;
        int py = this.panelY + shrink;
        int pw = Math.max(0, 430 - shrink * 2);
        int ph = Math.max(0, 340 - shrink * 2);
        int bodyCol1 = a << 24 | 0xE1432;
        int bodyCol2 = a << 24 | 0x70918;
        g.fillGradient(px, py, px + pw, py + ph, bodyCol1, bodyCol2);
        int borderCol = (int)(alpha * 180.0f) << 24 | 0xE5FF;
        g.fill(px, py, px + pw, py + 1, borderCol);
        g.fill(px, py + ph - 1, px + pw, py + ph, borderCol);
    }

    private void drawSonicReverberations(net.minecraft.client.gui.DrawContext g, float cx, float cy, float R, float ct, float pulse) {
        float maxR = (float)Math.max(this.width, this.height) * 0.85f;
        for (int w = 0; w < 4; ++w) {
            float phase = (ct * 0.48f + (float)w * 0.25f) % 1.0f;
            float waveR = R + phase * (maxR - R);
            float fade = 1.0f - phase;
            int alpha = (int)(fade * fade * 160.0f);
            if (alpha <= 2) continue;
            int cyan = alpha << 24 | 0xF0FF;
            int teal = alpha / 2 << 24 | 0x7A87;
            WelcomeScreen.drawSmoothRing(g, cx, cy, waveR, 2.0f, cyan);
            WelcomeScreen.drawSmoothRing(g, cx, cy, waveR + 3.0f, 1.0f, teal);
        }
        if (pulse > 0.05f) {
            float shockR = R + (1.0f - pulse) * 180.0f;
            int shockAlpha = (int)(pulse * 220.0f);
            WelcomeScreen.drawSmoothRing(g, cx, cy, shockR, 3.5f, shockAlpha << 24 | 0xE0FFFF);
        }
    }

    private void drawSummoningCircle(net.minecraft.client.gui.DrawContext g, float floorX, float floorY, float ct, float pulse, float scaleIn) {
        float rx = 64.0f * scaleIn;
        float ry = 18.0f * scaleIn;
        if (rx < 4.0f) {
            return;
        }
        for (int step = 3; step >= 1; --step) {
            int a = (int)((22.0f + pulse * 28.0f) * ((float)step / 3.0f));
            int col = a << 24 | 0xE5FF;
            WelcomeScreen.drawSmoothEllipse(g, floorX, floorY, rx * (0.55f + (float)step * 0.15f), ry * (0.55f + (float)step * 0.15f), 2.0f, col);
        }
        int outerColor = (int)(140.0f + pulse * 80.0f) << 24 | 0xF0FF;
        WelcomeScreen.drawSmoothEllipse(g, floorX, floorY, rx, ry, 1.5f, outerColor);
        int ticks = 24;
        float tickRot = ct * 0.25f;
        for (int i = 0; i < ticks; ++i) {
            float theta = (float)((double)i * 2.0 * Math.PI / (double)ticks + (double)tickRot);
            float cos = (float)Math.cos(theta);
            float sin = (float)Math.sin(theta);
            float x1 = floorX + rx * cos;
            float y1 = floorY + ry * sin;
            float tickLen = i % 4 == 0 ? 5.0f : 3.0f;
            float x2 = floorX + (rx + tickLen) * cos;
            float y2 = floorY + (ry + tickLen * 0.35f) * sin;
            int tAlpha = (int)(80.0 + 70.0 * Math.sin((double)theta * 2.0 + (double)ct * 3.0));
            WelcomeScreen.drawLine(g, x1, y1, x2, y2, tAlpha << 24 | 0xD8F0, 1);
        }
        float innerRx = rx * 0.74f;
        float innerRy = ry * 0.74f;
        int innerColor = (int)(110.0f + pulse * 60.0f) << 24 | 0x40E8FF;
        WelcomeScreen.drawSmoothEllipse(g, floorX, floorY, innerRx, innerRy, 1.2f, innerColor);
        int glyphCount = 8;
        float glyphRot = -ct * 0.45f;
        for (int i = 0; i < glyphCount; ++i) {
            float theta = (float)((double)i * 2.0 * Math.PI / (double)glyphCount + (double)glyphRot);
            float gx = floorX + innerRx * (float)Math.cos(theta);
            float gy = floorY + innerRy * (float)Math.sin(theta);
            int nodeAlpha = (int)(160.0 + 80.0 * Math.sin((double)theta * 3.0 + (double)ct * 4.0));
            WelcomeScreen.drawDiamond(g, (int)gx, (int)gy, 2, nodeAlpha << 24 | 0xC080FF);
        }
        float starRx = rx * 0.5f;
        float starRy = ry * 0.5f;
        float starRot = ct * 0.35f;
        float[] starX = new float[8];
        float[] starY = new float[8];
        for (int i = 0; i < 8; ++i) {
            float theta = (float)((double)i * 2.0 * Math.PI / 8.0 + (double)starRot);
            starX[i] = floorX + starRx * (float)Math.cos(theta);
            starY[i] = floorY + starRy * (float)Math.sin(theta);
        }
        int starCol = (int)(55.0f + pulse * 50.0f) << 24 | 0xE5FF;
        for (int i = 0; i < 8; ++i) {
            int next = (i + 3) % 8;
            WelcomeScreen.drawLine(g, starX[i], starY[i], starX[next], starY[next], starCol, 1);
        }
        float ripple1 = ct * 0.8f % 1.0f;
        float ripRx = rx * (1.0f + ripple1 * 0.65f);
        float ripRy = ry * (1.0f + ripple1 * 0.65f);
        int ripAlpha = (int)((1.0f - ripple1) * (60.0f + pulse * 90.0f));
        if (ripAlpha > 5) {
            WelcomeScreen.drawSmoothEllipse(g, floorX, floorY, ripRx, ripRy, 1.0f, ripAlpha << 24 | 0xF5FF);
        }
    }

    private void drawPlayerTendrils(net.minecraft.client.gui.DrawContext g, float playerX, float playerY, float ct, float pulse, float scaleIn, boolean frontSide) {
        if (scaleIn < 0.12f) {
            return;
        }
        float centerTorsoY = playerY - 46.0f;
        int tentacleCount = 7;
        for (int i = 0; i < tentacleCount; ++i) {
            float baseAngle = (float)((double)i * (Math.PI * 2 / (double)tentacleCount) + (double)(ct * (0.6f + (float)(i % 3) * 0.14f)));
            float verticalAnchor = centerTorsoY + (float)Math.sin((double)i * 1.8 + (double)ct * 1.3) * 16.0f;
            float maxReach = (44.0f + (float)Math.sin((double)i * 2.3 + (double)ct * 2.0) * 12.0f + pulse * 10.0f) * scaleIn;
            int segments = 14;
            float prevX = 0.0f;
            float prevY = 0.0f;
            float prevZ = 0.0f;
            boolean prevValid = false;
            for (int s = 0; s <= segments; ++s) {
                float frac = (float)s / (float)segments;
                float dist = 12.0f + frac * maxReach;
                float coil = (float)(Math.sin((double)frac * 3.8 - (double)ct * 3.5 + (double)i * 1.3) * (double)0.45f + (double)(frac * 0.75f));
                float angle = baseAngle + coil;
                float curX = playerX + (float)Math.cos(angle) * dist;
                float curZ = (float)Math.sin(angle) * dist;
                float vertWave = (float)Math.sin((double)frac * 4.2 - (double)ct * 4.0 + (double)i * 2.1) * (14.0f * frac) - frac * 12.0f;
                float curY = verticalAnchor + vertWave;
                if (s > 0 && prevValid) {
                    float life;
                    int alpha;
                    boolean isFront;
                    float midZ = (prevZ + curZ) * 0.5f;
                    boolean bl = isFront = midZ >= 0.0f;
                    if (isFront == frontSide && (alpha = (int)((life = 1.0f - frac * 0.75f) * (130.0f + pulse * 85.0f))) > 4) {
                        if (frontSide) {
                            int coreCol = alpha << 24 | 0x90F8FF;
                            int glowCol = alpha / 2 << 24 | 0xE5FF;
                            WelcomeScreen.drawLine(g, prevX, prevY, curX, curY, glowCol, frac < 0.45f ? 3 : 2);
                            WelcomeScreen.drawLine(g, prevX, prevY, curX, curY, coreCol, frac < 0.35f ? 2 : 1);
                        } else {
                            int shadowCol = (int)((float)alpha * 0.65f) << 24 | 0x9BB8;
                            WelcomeScreen.drawLine(g, prevX, prevY, curX, curY, shadowCol, frac < 0.45f ? 2 : 1);
                        }
                    }
                }
                if (s == segments) {
                    boolean tipIsFront;
                    boolean bl = tipIsFront = curZ >= 0.0f;
                    if (tipIsFront == frontSide) {
                        int tipAlpha = (int)(180.0 + 70.0 * Math.sin((double)ct * 6.0 + (double)i));
                        int tipCol = tipAlpha << 24 | (frontSide ? 63743 : 47320);
                        WelcomeScreen.drawDiamond(g, (int)curX, (int)curY, frontSide ? 2 : 1, tipCol);
                    }
                }
                prevX = curX;
                prevY = curY;
                prevZ = curZ;
                prevValid = true;
            }
        }
    }

    private void drawPlayerAvatar(net.minecraft.client.gui.DrawContext g, float playerX, float playerY, float sphereX, float sphereY, float ct, float scaleIn) {
        float phaseAlpha;
        int pA;
        if (this.client == null || this.client.player == null) {
            return;
        }
        if (scaleIn < 0.12f) {
            return;
        }
        int scale = (int)(58.0f * Math.min(1.0f, (ct - 0.2f) / 0.7f));
        if (scale < 6) {
            return;
        }
        int x1 = (int)(playerX - 44.0f);
        int y1 = (int)(playerY - 96.0f);
        int x2 = (int)(playerX + 44.0f);
        int y2 = (int)(playerY + 12.0f);
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity((net.minecraft.client.gui.DrawContext)g, (int)x1, (int)y1, (int)x2, (int)y2, (int)scale, (float)0.0625f, (float)sphereX, (float)sphereY, (net.minecraft.entity.LivingEntity)this.client.player);
        if (ct < 1.3f && (pA = (int)((phaseAlpha = Math.max(0.0f, 1.0f - (ct - 0.2f) / 1.1f)) * 85.0f)) > 0) {
            int scanCol = pA << 24 | 0xE5FF;
            for (int y = y1; y < y2; y += 3) {
                float glitch = (float)Math.sin((double)y * 0.45 + (double)ct * 18.0);
                if (!(glitch > 0.25f)) continue;
                g.fill(x1, y, x2, y + 1, scanCol);
            }
        }
    }

    private void drawSoulTendrils(net.minecraft.client.gui.DrawContext g, float cx, float cy, float playerX, float playerY, float R, float t, float ct, float pulse) {
        int ribbonCount = 6;
        float baseLen = R * (0.85f + pulse * 0.35f);
        for (int i = 0; i < ribbonCount; ++i) {
            float rootAngle = (float)((double)i * 2.0 * Math.PI / (double)ribbonCount + (double)(ct * 0.2f));
            float len = baseLen * (0.8f + 0.3f * (float)Math.sin((double)i * 2.5 + (double)ct * 2.2));
            float prevX = cx + (float)Math.cos(rootAngle) * R;
            float prevY = cy + (float)Math.sin(rootAngle) * R;
            int segments = 10;
            for (int s = 1; s <= segments; ++s) {
                float segFrac = (float)s / (float)segments;
                float turb = (float)(Math.sin((double)segFrac * 4.5 - (double)ct * 3.5 + (double)i) * 0.18);
                float curAngle = rootAngle + turb;
                float curDist = R + segFrac * len;
                float nextX = cx + (float)Math.cos(curAngle) * curDist;
                float nextY = cy + (float)Math.sin(curAngle) * curDist;
                float life = 1.0f - segFrac;
                int alpha = (int)(life * (110.0f + pulse * 80.0f));
                if (alpha > 4) {
                    int col = alpha << 24 | 0xF5FF;
                    WelcomeScreen.drawLine(g, prevX, prevY, nextX, nextY, col, 1);
                }
                prevX = nextX;
                prevY = nextY;
            }
        }
        float startAngle = (float)(Math.PI + Math.sin((double)ct * 1.8) * (double)0.12f);
        float startX = cx + (float)Math.cos(startAngle) * R;
        float startY = cy + (float)Math.sin(startAngle) * R;
        float targetX = playerX + 8.0f;
        float targetY = playerY - 46.0f;
        float pX = startX;
        float pY = startY;
        int conduitSegs = 20;
        for (int s = 1; s <= conduitSegs; ++s) {
            float f = (float)s / (float)conduitSegs;
            float lx = startX + f * (targetX - startX);
            float ly = startY + f * (targetY - startY);
            float arc = -((float)Math.sin((double)f * Math.PI)) * 22.0f;
            float wave = (float)Math.sin((double)f * 9.0 - (double)ct * 5.0) * 3.5f;
            float nx = lx;
            float ny = ly + arc + wave;
            int a = (int)((double)(140.0f + pulse * 95.0f) * Math.sin((double)f * Math.PI));
            if (a > 6) {
                WelcomeScreen.drawLine(g, pX, pY, nx, ny, a << 24 | 0x90F8FF, 2);
                WelcomeScreen.drawLine(g, pX, pY, nx, ny, a / 2 << 24 | 0xE5FF, 3);
            }
            pX = nx;
            pY = ny;
        }
        int impactAlpha = (int)(180.0 + 75.0 * Math.sin((double)ct * 6.0));
        WelcomeScreen.drawDiamond(g, (int)targetX, (int)targetY, 2, impactAlpha << 24 | 0xFFFFFF);
        WelcomeScreen.drawDiamond(g, (int)targetX, (int)targetY, 4, impactAlpha / 2 << 24 | 0xF0FF);
        for (int r = 1; r <= 2; ++r) {
            float rippleR = (ct * 18.0f + (float)r * 10.0f) % 20.0f;
            float ripFrac = Math.max(0.0f, 1.0f - rippleR / 20.0f);
            int ripCol = (int)(ripFrac * 100.0f) << 24 | 0xF5FF;
            WelcomeScreen.drawSmoothEllipse(g, targetX, targetY, rippleR, rippleR * 0.5f, 1.0f, ripCol);
        }
    }

    private void drawOrbitingChromeRings(net.minecraft.client.gui.DrawContext g, float cx, float cy, float R, float ct, boolean frontSide) {
        this.draw3DRing(g, cx, cy, R * 1.42f, ct * 0.55f, 0.28f, ct * 0.18f, ct, frontSide, 61695);
        this.draw3DRing(g, cx, cy, R * 1.72f, -ct * 0.4f + 0.9f, -1.22f, ct * 0.22f, ct, frontSide, 6342911);
        this.draw3DRing(g, cx, cy, R * 1.95f, ct * 0.28f + 2.1f, 0.9f, -ct * 0.14f, ct, frontSide, 3182784);
    }

    private void draw3DRing(net.minecraft.client.gui.DrawContext g, float cx, float cy, float radius, float yaw, float pitch, float roll, float ct, boolean frontSide, int accentColor) {
        int steps = 60;
        float prevScreenX = 0.0f;
        float prevScreenY = 0.0f;
        boolean prevValid = false;
        float cyaw = (float)Math.cos(yaw);
        float syaw = (float)Math.sin(yaw);
        float cpitch = (float)Math.cos(pitch);
        float spitch = (float)Math.sin(pitch);
        float croll = (float)Math.cos(roll);
        float sroll = (float)Math.sin(roll);
        for (int i = 0; i <= steps; ++i) {
            float theta = (float)((double)i * 2.0 * Math.PI / (double)steps);
            float x0 = radius * (float)Math.cos(theta);
            float y0 = radius * (float)Math.sin(theta);
            float z0 = 0.0f;
            float x1 = x0 * croll - y0 * sroll;
            float y1 = x0 * sroll + y0 * croll;
            float z1 = z0;
            float x2 = x1;
            float y2 = y1 * cpitch - z1 * spitch;
            float z2 = y1 * spitch + z1 * cpitch;
            float x3 = x2 * cyaw + z2 * syaw;
            float y3 = y2;
            float z3 = -x2 * syaw + z2 * cyaw;
            boolean isFront = z3 >= 0.0f;
            float screenX = cx + x3;
            float screenY = cy + y3;
            if (i > 0 && prevValid && isFront == frontSide) {
                int segColor;
                float lightDot = Math.abs((x3 - prevScreenX + cx) / radius);
                int chromeBrightness = (int)(140.0f + lightDot * 115.0f);
                chromeBrightness = Math.clamp((long)chromeBrightness, 80, 255);
                int alpha, rCol, gCol, bCol;
                if (frontSide) {
                    alpha = 232;
                    rCol = (int)((float)chromeBrightness * 0.85f);
                    gCol = (int)((float)chromeBrightness * 0.95f);
                    bCol = chromeBrightness;
                    segColor = alpha << 24 | rCol << 16 | gCol << 8 | bCol;
                } else {
                    alpha = 136;
                    rCol = (int)((float)chromeBrightness * 0.35f);
                    gCol = (int)((float)chromeBrightness * 0.45f);
                    bCol = (int)((float)chromeBrightness * 0.55f);
                    segColor = alpha << 24 | rCol << 16 | gCol << 8 | bCol;
                }
                WelcomeScreen.drawLine(g, prevScreenX, prevScreenY, screenX, screenY, segColor, frontSide ? 2 : 1);
            }
            prevScreenX = screenX;
            prevScreenY = screenY;
            prevValid = true;
        }
        if (frontSide) {
            float beadTheta = (float)((double)(ct * 1.8f) % (Math.PI * 2));
            float bx0 = radius * (float)Math.cos(beadTheta);
            float by0 = radius * (float)Math.sin(beadTheta);
            float bx1 = bx0 * croll - by0 * sroll;
            float by1 = bx0 * sroll + by0 * croll;
            float by2 = by1 * cpitch;
            float bz2 = by1 * spitch;
            float bx3 = bx1 * cyaw + bz2 * syaw;
            float bz3 = -bx1 * syaw + bz2 * cyaw;
            if (bz3 >= 0.0f) {
                float beadX = cx + bx3;
                float beadY = cy + by2;
                WelcomeScreen.drawDiamond(g, (int)beadX, (int)beadY, 3, -1);
                WelcomeScreen.drawDiamond(g, (int)beadX, (int)beadY, 5, -1878985217);
            }
        }
    }

    private void drawDarkChromeSphere(net.minecraft.client.gui.DrawContext g, float cx, float cy, float R, float t, float ct, float pulse) {
        int fAlpha;
        int fi;
        int glossR;
        int iR = (int)R;
        if (iR <= 2) {
            return;
        }
        int[] coronaRadii = new int[]{(int)(R * 2.4f), (int)(R * 1.95f), (int)(R * 1.6f), (int)(R * 1.35f), (int)(R * 1.18f)};
        int[] coronaAlphas = new int[]{6, 12, 22, 38, 60};
        int[] coronaColors = new int[]{4128, 6184, 9525, 12352, 18520};
        for (int ci = 0; ci < coronaRadii.length; ++ci) {
            int cAlpha = (int)((float)coronaAlphas[ci] * (0.85f + 0.15f * pulse));
            if (cAlpha <= 0) continue;
            WelcomeScreen.drawCircleFill(g, (int)cx, (int)cy, coronaRadii[ci], cAlpha << 24 | coronaColors[ci], 2);
        }
        int contactAlpha = (int)(90.0f + pulse * 55.0f);
        WelcomeScreen.drawCircleFill(g, (int)cx, (int)cy, (int)(R * 1.1f), contactAlpha << 24 | 0xC8E8, 1);
        WelcomeScreen.drawCircleFill(g, (int)cx, (int)cy, iR, -16711164, 1);
        int scanlines = Math.min(iR * 2, 160);
        float stepY = 2.0f * R / (float)scanlines;
        float horizonShift = (float)(Math.sin((double)ct * 0.8) * 0.04);
        for (int step = 0; step < scanlines; ++step) {
            int q3;
            int q1;
            float dy = -R + ((float)step + 0.5f) * stepY;
            float ny = dy / R;
            float dySq = dy * dy;
            if (dySq >= R * R) continue;
            float hw = (float)Math.sqrt(R * R - dySq);
            int yScreen = (int)(cy + dy);
            int xRight = (int)(cx + hw);
            int xLeft = (int)(cx - hw);
            if (xRight <= xLeft + 1) continue;
            float distFromEquator = Math.abs(ny - horizonShift);
            float chromeBand = (float)Math.exp((double)(-distFromEquator * distFromEquator) * 240.0);
            float fresnelRim = (float)Math.pow(Math.abs(ny), 3.5);
            if (chromeBand > 0.008f) {
                float b = chromeBand;
                int sheenR = (int)(b * 185.0f);
                int sheenG = (int)(b * 215.0f);
                int sheenB = (int)(b * 235.0f);
                int horizonBright = 0xFF000000 | sheenR << 16 | sheenG << 8 | sheenB;
                int edgeCol = (int)(b * 180.0f);
                int rimColCh = 0xFF000000 | (int)((float)edgeCol * 0.8f) << 8 | edgeCol;
                int q12 = (int)((float)xLeft + hw * 0.18f);
                int q2 = (int)cx;
                int q32 = (int)((float)xRight - hw * 0.18f);
                g.fillGradient(xLeft, yScreen, q12, yScreen + 1, rimColCh, horizonBright);
                g.fillGradient(q12, yScreen, q2, yScreen + 1, horizonBright, -2560257);
                g.fillGradient(q2, yScreen, q32, yScreen + 1, -2560257, horizonBright);
                g.fillGradient(q32, yScreen, xRight, yScreen + 1, horizonBright, rimColCh);
                continue;
            }
            if (fresnelRim > 0.12f) {
                int fr = (int)(fresnelRim * 70.0f);
                int fresnelCol = 0xFF000000 | (int)((float)fr * 0.6f) << 8 | fr;
                q1 = (int)((float)xLeft + hw * 0.08f);
                q3 = (int)((float)xRight - hw * 0.08f);
                g.fillGradient(xLeft, yScreen, q1, yScreen + 1, fresnelCol, -16711164);
                g.fill(q1, yScreen, q3, yScreen + 1, -16711164);
                g.fillGradient(q3, yScreen, xRight, yScreen + 1, -16711164, fresnelCol);
                continue;
            }
            int skyRefl = ny < -0.25f ? (int)((-ny - 0.25f) * 28.0f) : 0;
            int darkBody = 0xFF000000 | skyRefl << 16 | skyRefl << 8 | (int)((float)skyRefl * 1.5f);
            q1 = (int)((float)xLeft + hw * 0.06f);
            q3 = (int)((float)xRight - hw * 0.06f);
            g.fillGradient(xLeft, yScreen, q1, yScreen + 1, -16711164, darkBody);
            g.fill(q1, yScreen, q3, yScreen + 1, darkBody);
            g.fillGradient(q3, yScreen, xRight, yScreen + 1, darkBody, -16711164);
        }
        int glossX = (int)(cx - R * 0.3f);
        int glossY = (int)(cy - R * 0.36f);
        for (int gr = glossR = (int)(R * 0.38f); gr >= 1; --gr) {
            float gf = (float)gr / (float)glossR;
            int gAlpha = (int)((1.0f - gf) * (1.0f - gf) * 55.0f);
            if (gAlpha <= 0) continue;
            WelcomeScreen.drawCircleFill(g, glossX, glossY, gr, gAlpha << 24 | 0x90C8F8, 2);
        }
        int glintX = (int)(cx - R * 0.36f);
        int glintY = (int)(cy - R * 0.4f);
        WelcomeScreen.drawDiamond(g, glintX, glintY, 2, -1);
        WelcomeScreen.drawDiamond(g, glintX, glintY, 4, -1596391425);
        int flareLen = (int)(R * 0.55f);
        for (fi = 1; fi <= flareLen && (fAlpha = (int)(200.0 * Math.exp(-((float)fi) / ((float)flareLen * 0.3f)))) > 2; ++fi) {
            g.fill(glintX - fi, glintY, glintX - fi + 1, glintY + 1, fAlpha << 24 | 0xFFFFFF);
            g.fill(glintX + fi, glintY, glintX + fi + 1, glintY + 1, fAlpha << 24 | 0xFFFFFF);
        }
        for (fi = 1; fi <= (int)((float)flareLen * 0.7f) && (fAlpha = (int)(180.0 * Math.exp(-((float)fi) / ((float)flareLen * 0.28f)))) > 2; ++fi) {
            g.fill(glintX, glintY - fi, glintX + 1, glintY - fi + 1, fAlpha << 24 | 0xFFFFFF);
            g.fill(glintX, glintY + fi, glintX + 1, glintY + fi + 1, fAlpha << 24 | 0xCCEEFF);
        }
        int rimAlpha = (int)(200.0f + pulse * 55.0f);
        WelcomeScreen.drawSmoothRing(g, cx, cy, R, 1.5f, rimAlpha << 24 | 0x40C8F0);
        WelcomeScreen.drawSmoothRing(g, cx, cy, R - 1.0f, 1.0f, rimAlpha / 2 << 24 | 0xE8FF);
    }

    private void drawSoulSparks(net.minecraft.client.gui.DrawContext g, float cx, float cy, float R, float t, float ct) {
        int count = Math.min(12, 24);
        for (int i = 0; i < count; ++i) {
            float ang = this.sparkAngle[i] + ct * this.sparkSpeed[i] * 0.7f;
            float dist = R * (this.sparkDist[i] * 0.85f + 0.1f * (float)Math.sin((double)ct * 1.8 + (double)this.sparkPhase[i]));
            float sx = cx + (float)Math.cos(ang) * dist;
            float sy = cy + (float)Math.sin(ang) * (dist * 0.6f);
            float pulse = (float)(0.5 + 0.5 * Math.sin((double)ct * 3.0 + (double)this.sparkPhase[i]));
            int alpha = (int)(120.0f + pulse * 110.0f);
            int sparkCol = alpha << 24 | 0x70F5FF;
            int size = (int)Math.max(1.5f, this.sparkSize[i] * 0.8f);
            WelcomeScreen.drawDiamond(g, (int)sx, (int)sy, size, sparkCol);
        }
    }

    private void drawHolographicHUD(net.minecraft.client.gui.DrawContext g, float cx, float cy, float R, float ct) {
        float hudAlpha = Math.min(1.0f, Math.max(0.0f, (ct - 0.8f) / 0.8f));
        int a = (int)(hudAlpha * 255.0f);
        if (a <= 5) {
            return;
        }
        int cyanHud = a << 24 | 0xE5FF;
        int darkTealHud = a / 2 << 24 | 0x6677;
        int headerY = 16;
        String subHeader = "// COBBLEVERSE :: AMETHERIA REALM SEQUENCE //";
        g.drawCenteredTextWithShadow(this.textRenderer, subHeader, (int)cx, headerY, darkTealHud);
        String mainHeader = "GYM PROTOCOL: SYNCHRONIZED";
        g.drawCenteredTextWithShadow(this.textRenderer, mainHeader, (int)cx, headerY + 12, cyanHud);
        int reticleW = (int)((float)this.width * 0.42f);
        int reticleH = (int)((float)this.height * 0.36f);
        int arm = 16;
        int left = (int)(cx - (float)reticleW);
        int right = (int)(cx + (float)reticleW);
        int top = (int)(cy - (float)reticleH);
        int bottom = (int)(cy + (float)reticleH);
        g.fill(left, top, left + arm, top + 1, cyanHud);
        g.fill(left, top, left + 1, top + arm, cyanHud);
        g.fill(right - arm, top, right, top + 1, cyanHud);
        g.fill(right - 1, top, right, top + arm, cyanHud);
        g.fill(left, bottom - 1, left + arm, bottom, cyanHud);
        g.fill(left, bottom - arm, left + 1, bottom, cyanHud);
        g.fill(right - arm, bottom - 1, right, bottom, cyanHud);
        g.fill(right - 1, bottom - arm, right, bottom, cyanHud);
        String statusText = ct < 1.4f ? "\u25b8 INITIALIZING AMETHERIA SOUL CORE..." : (ct < 2.6f ? "\u25b8 CONVERGING DARK CHROME SPHERE MATRIX..." : (ct < 3.8f ? "\u2726 AURA RESONANCE: 100% \u00b7 REALM AWAITS \u2726" : "\u26a1 COMMENCING TRANSIT..."));
        int statusCol = ct >= 2.6f && ct < 3.8f ? a << 24 | 0xFFE066 : cyanHud;
        int statusY = this.height - 52;
        g.drawCenteredTextWithShadow(this.textRenderer, statusText, (int)cx, statusY, statusCol);
        int barW = 200;
        int barH = 3;
        int barX = (int)(cx - (float)(barW / 2));
        int barY = statusY + 14;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, a / 3 << 24 | 0x3344);
        float progress = Math.min(1.0f, ct / 4.2f);
        g.fill(barX, barY, barX + (int)((float)barW * progress), barY + barH, cyanHud);
        String skipPrompt = "[ CLICK ANYWHERE OR PRESS ESC TO ENTER ]";
        g.drawCenteredTextWithShadow(this.textRenderer, skipPrompt, (int)cx, this.height - 20, a / 2 << 24 | 0x8899AA);
    }

    private void drawLetterboxBars(net.minecraft.client.gui.DrawContext g, float ct) {
        float barProgress = Math.min(1.0f, ct * 2.2f);
        int barH = (int)(Math.min((float)this.height * 0.12f, 44.0f) * barProgress);
        if (barH <= 0) {
            return;
        }
        g.fill(0, 0, this.width, barH, -16711164);
        g.fill(0, barH - 1, this.width, barH, -16718337);
        g.fill(0, this.height - barH, this.width, this.height, -16711164);
        g.fill(0, this.height - barH, this.width, this.height - barH + 1, -16718337);
    }

    private void drawSupernovaFlare(net.minecraft.client.gui.DrawContext g, float cx, float cy, float ct) {
        float f = (ct - 3.7f) / 0.9f;
        if (f <= 0.0f) {
            return;
        }
        int alpha = (int)(Math.min(1.0f, f * 1.6f) * 255.0f);
        int flashCol = alpha << 24 | 0xEEFFFF;
        g.fill(0, 0, this.width, this.height, flashCol);
    }

    private void checkSoundCues(float ct) {
        if (this.lastSoundStage < 0 && ct >= 0.0f) {
            this.lastSoundStage = 0;
            this.playClientSound(0);
        } else if (this.lastSoundStage < 1 && ct >= 1.55f) {
            this.lastSoundStage = 1;
            this.playClientSound(1);
        } else if (this.lastSoundStage < 2 && ct >= 2.75f) {
            this.lastSoundStage = 2;
            this.playClientSound(2);
        } else if (this.lastSoundStage < 3 && ct >= 3.75f) {
            this.lastSoundStage = 3;
            this.playClientSound(3);
        }
    }

    private void playClientSound(int stage) {
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc == null || mc.getSoundManager() == null) {
                return;
            }
            switch (stage) {
                case 0: {
                    mc.getSoundManager().play((net.minecraft.client.sound.SoundInstance)net.minecraft.client.sound.PositionedSoundInstance.master((net.minecraft.registry.entry.RegistryEntry)net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, (float)1.0f));
                    mc.getSoundManager().play((net.minecraft.client.sound.SoundInstance)net.minecraft.client.sound.PositionedSoundInstance.master((net.minecraft.sound.SoundEvent)net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE, (float)0.65f, (float)0.7f));
                    break;
                }
                case 1: {
                    mc.getSoundManager().play((net.minecraft.client.sound.SoundInstance)net.minecraft.client.sound.PositionedSoundInstance.master((net.minecraft.sound.SoundEvent)net.minecraft.sound.SoundEvents.ENTITY_WARDEN_HEARTBEAT, (float)0.85f, (float)0.95f));
                    break;
                }
                case 2: {
                    mc.getSoundManager().play((net.minecraft.client.sound.SoundInstance)net.minecraft.client.sound.PositionedSoundInstance.master((net.minecraft.sound.SoundEvent)net.minecraft.sound.SoundEvents.ENTITY_WARDEN_HEARTBEAT, (float)1.05f, (float)1.0f));
                    mc.getSoundManager().play((net.minecraft.client.sound.SoundInstance)net.minecraft.client.sound.PositionedSoundInstance.master((net.minecraft.sound.SoundEvent)net.minecraft.sound.SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, (float)1.35f, (float)0.65f));
                    break;
                }
                case 3: {
                    mc.getSoundManager().play((net.minecraft.client.sound.SoundInstance)net.minecraft.client.sound.PositionedSoundInstance.master((net.minecraft.sound.SoundEvent)net.minecraft.sound.SoundEvents.ENTITY_WARDEN_SONIC_BOOM, (float)1.15f, (float)0.8f));
                    mc.getSoundManager().play((net.minecraft.client.sound.SoundInstance)net.minecraft.client.sound.PositionedSoundInstance.master((net.minecraft.sound.SoundEvent)net.minecraft.sound.SoundEvents.BLOCK_END_PORTAL_SPAWN, (float)1.4f, (float)0.7f));
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void drawVignette(net.minecraft.client.gui.DrawContext g) {
        g.fill(0, 0, this.width, this.height, -737802224);
        int steps = 14;
        for (int i = 1; i <= steps; ++i) {
            float f = (float)i / (float)steps;
            int alpha = (int)(180.0f * f * f * f);
            int col = alpha << 24;
            int bx = (int)((float)this.width * f * 0.45f);
            int by = (int)((float)this.height * f * 0.45f);
            if (bx <= 0 || by <= 0) continue;
            g.fill(0, 0, this.width, by, col);
            g.fill(0, this.height - by, this.width, this.height, col);
            g.fill(0, 0, bx, this.height, col);
            g.fill(this.width - bx, 0, this.width, this.height, col);
        }
    }

    private void drawPanel(net.minecraft.client.gui.DrawContext g, float t) {
        int px = this.panelX;
        int py = this.panelY;
        int pw = 430;
        int ph = 340;
        for (int i = 8; i >= 1; --i) {
            int alpha = 12 + i * 7;
            g.fill(px - i, py + i * 2, px + pw + i, py + ph + i * 2, alpha << 24);
        }
        g.fillGradient(px, py, px + pw, py + ph, -15854542, -16316136);
        g.fillGradient(px, py, px + 3, py + ph, 1430282495, 538984576);
        g.fillGradient(px + pw - 3, py, px + pw, py + ph, 1436565759, 545276048);
        g.fillGradient(px + 2, py + 2, px + pw - 2, py + (int)((float)ph * 0.24f), 0x2EFFFFFF, 0xFFFFFF);
        float shimFrac = t * 0.45f % 1.0f;
        int shimY = py + (int)(shimFrac * (float)(ph + 60)) - 30;
        int shimH = 28;
        if (shimY + shimH > py && shimY < py + ph) {
            int clamTop = Math.max(shimY, py + 1);
            int clamMid = Math.max(shimY + shimH / 2, py + 1);
            int clamBot = Math.min(shimY + shimH, py + ph - 1);
            if (clamTop < clamMid) {
                g.fillGradient(px + 2, clamTop, px + pw - 2, clamMid, 0xFFFFFF, 0x12FFFFFF);
            }
            if (clamMid < clamBot) {
                g.fillGradient(px + 2, clamMid, px + pw - 2, clamBot, 0x12FFFFFF, 0xFFFFFF);
            }
        }
        g.fillGradient(px, py, px + pw, py + 1, -10979112, -7315216);
        g.fillGradient(px, py + ph - 1, px + pw, py + ph, -14012312, -11519872);
        g.fill(px, py, px + 1, py + ph, -13082432);
        g.fill(px + pw - 1, py, px + pw, py + ph, -10469216);
        g.fill(px, py, px + 2, py + 2, -7820545);
        g.fill(px + pw - 2, py, px + pw, py + 2, -5603073);
        g.fill(px, py + ph - 2, px + 2, py + ph, -10057507);
        g.fill(px + pw - 2, py, px + pw, py + ph, -7838004);
    }

    private void drawTitle(net.minecraft.client.gui.DrawContext g, float t) {
        int cx = this.panelX + 215;
        float hue = t * 0.12f % 1.0f;
        int col1 = 0xFF000000 | WelcomeScreen.hsvToRgb(hue, 0.25f, 1.0f);
        int col2 = 0xFF000000 | WelcomeScreen.hsvToRgb((hue + 0.12f) % 1.0f, 0.55f, 1.0f);
        int ty = this.panelY + 11;
        g.drawCenteredTextWithShadow(this.textRenderer, "W E L C O M E   T O", cx, ty, -1885292306);
        g.drawCenteredTextWithShadow(this.textRenderer, "A M E T H E R I A", cx + 1, ty + 12, 0x60000000);
        g.drawCenteredTextWithShadow(this.textRenderer, "A M E T H E R I A", cx, ty + 11, col1);
        g.drawCenteredTextWithShadow(this.textRenderer, "COBBLEVERSE  \u00b7  RETYPED GYMS", cx + 1, ty + 24, 0x60000000);
        g.drawCenteredTextWithShadow(this.textRenderer, "COBBLEVERSE  \u00b7  RETYPED GYMS", cx, ty + 23, col2);
        String ver = "v1.0.0";
        g.drawText(this.textRenderer, ver, this.panelX + 430 - this.textRenderer.getWidth(ver) - 8, this.panelY + 8, 1885376716, false);
    }

    private void drawDivider(net.minecraft.client.gui.DrawContext g) {
        int dy = this.panelY + 50;
        int m = 18;
        int cx = this.panelX + 215;
        g.fillGradient(this.panelX + m, dy, cx, dy + 1, 5271696, -9400065);
        g.fillGradient(cx, dy, this.panelX + 430 - m, dy + 1, -9400065, 5271696);
    }

    private void drawBullets(net.minecraft.client.gui.DrawContext g) {
        int startY = this.panelY + 59;
        int lineH = 19;
        int tagX = this.panelX + 13;
        int textX = this.panelX + 28;
        for (int i = 0; i < FEATURES.length; ++i) {
            int[] rgb = TAG_RGB[i % TAG_RGB.length];
            int tagColor = 0xFF000000 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
            int glowColor = 0x30000000 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
            int row = startY + i * lineH;
            g.fill(tagX, row + 2, tagX + 4, row + lineH - 3, tagColor);
            g.fill(tagX + 4, row + 2, tagX + 9, row + lineH - 3, glowColor);
            String raw = FEATURES[i];
            int split = raw.indexOf("  ");
            if (split > 0) {
                String label = raw.substring(0, split);
                String detail = raw.substring(split + 2);
                g.drawText(this.textRenderer, label, textX, row + 4, tagColor, false);
                g.drawText(this.textRenderer, "  " + detail, textX + this.textRenderer.getWidth(label), row + 4, -4469522, false);
                continue;
            }
            g.drawText(this.textRenderer, raw, textX, row + 4, -4469522, false);
        }
    }

    private void drawEnterButton(net.minecraft.client.gui.DrawContext g, float t) {
        float pulse = (float)(0.82 + 0.18 * Math.sin((double)t * 2.8));
        int bx = this.btnX;
        int by = this.btnY;
        int bw = 148;
        int bh = 36;
        for (int i = 9; i >= 1; --i) {
            double baseAlpha = this.btnHovered ? 44.0 - (double)i * 4.5 : 22.0 - (double)i * 2.2;
            int alpha = (int)Math.max(0.0, baseAlpha * (double)pulse);
            g.fill(bx - i, by - i, bx + bw + i, by + bh + i, alpha << 24 | 0xFFB200);
        }
        int topCol = this.btnHovered ? -10128 : -1523680;
        int botCol = this.btnHovered ? -3371008 : -5607936;
        g.fillGradient(bx, by, bx + bw, by + bh, topCol, botCol);
        g.fillGradient(bx + 2, by + 2, bx + bw - 2, by + bh / 2 + 2, 0x2AFFFFFF, 0xFFFFFF);
        g.fill(bx, by, bx + bw, by + 1, -6008);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, -8956672);
        g.fill(bx, by, bx + 1, by + bh, -9664);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, -8956672);
        String label = "ENTER";
        int textY = by + (bh - 8) / 2;
        int shadowColor = this.btnHovered ? 0x35FFFFFF : 0x18FFFFFF;
        int textColor = this.btnHovered ? -15202304 : -14019584;
        g.drawCenteredTextWithShadow(this.textRenderer, label, bx + bw / 2 + 1, textY + 1, shadowColor);
        g.drawCenteredTextWithShadow(this.textRenderer, label, bx + bw / 2, textY, textColor);
    }

    public void startCinematic() {
        if (this.state == State.AURA_CINEMATIC) {
            return;
        }
        this.state = State.AURA_CINEMATIC;
        this.cinematicStartTime = System.currentTimeMillis();
        this.lastSoundStage = -1;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.state == State.WELCOME_PANEL) {
            if (button == 0 && this.btnHovered) {
                this.startCinematic();
                return true;
            }
        } else if (this.state == State.AURA_CINEMATIC) {
            this.close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.state == State.WELCOME_PANEL) {
            if (keyCode == 257 || keyCode == 335 || keyCode == 32) {
                this.startCinematic();
                return true;
            }
        } else if (this.state == State.AURA_CINEMATIC && (keyCode == 256 || keyCode == 257 || keyCode == 335 || keyCode == 32)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void close() {
        if (this.state == State.AURA_CINEMATIC) {
            RetypedGymsClient.triggerSpawnAura();
        }
        super.close();
    }

    public boolean shouldCloseOnEsc() {
        return this.state == State.AURA_CINEMATIC;
    }

    public boolean shouldPause() {
        return false;
    }

    private static void drawCircleFill(net.minecraft.client.gui.DrawContext g, int cx, int cy, int r, int color, int step) {
        if (r <= 0) {
            return;
        }
        int r2 = r * r;
        for (int dy = -r; dy <= r; dy += step) {
            int hw = (int)Math.sqrt(Math.max(0, r2 - dy * dy));
            g.fill(cx - hw, cy + dy, cx + hw, cy + dy + step, color);
        }
    }

    private static void drawSmoothRing(net.minecraft.client.gui.DrawContext g, float cx, float cy, float radius, float stroke, int color) {
        if (radius <= 1.0f) {
            return;
        }
        int steps = Math.min(72, Math.max(28, (int)(radius * 0.7f)));
        float prevX = cx + radius;
        float prevY = cy;
        for (int i = 1; i <= steps; ++i) {
            float theta = (float)((double)i * 2.0 * Math.PI / (double)steps);
            float nextX = cx + radius * (float)Math.cos(theta);
            float nextY = cy + radius * (float)Math.sin(theta);
            WelcomeScreen.drawLine(g, prevX, prevY, nextX, nextY, color, (int)Math.max(1.0f, stroke));
            prevX = nextX;
            prevY = nextY;
        }
    }

    private static void drawSmoothEllipse(net.minecraft.client.gui.DrawContext g, float cx, float cy, float rx, float ry, float stroke, int color) {
        if (rx <= 1.0f || ry <= 1.0f) {
            return;
        }
        int steps = Math.min(64, Math.max(24, (int)(rx * 0.6f)));
        float prevX = cx + rx;
        float prevY = cy;
        for (int i = 1; i <= steps; ++i) {
            float theta = (float)((double)i * 2.0 * Math.PI / (double)steps);
            float nextX = cx + rx * (float)Math.cos(theta);
            float nextY = cy + ry * (float)Math.sin(theta);
            WelcomeScreen.drawLine(g, prevX, prevY, nextX, nextY, color, (int)Math.max(1.0f, stroke));
            prevX = nextX;
            prevY = nextY;
        }
    }

    private static void drawDiamond(net.minecraft.client.gui.DrawContext g, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; ++dy) {
            int span = radius - Math.abs(dy);
            g.fill(x - span, y + dy, x + span + 1, y + dy + 1, color);
        }
    }

    private static void drawLine(net.minecraft.client.gui.DrawContext g, float x1, float y1, float x2, float y2, int color, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist <= 0.001f) {
            return;
        }
        float nx = -dy / dist;
        float ny = dx / dist;
        float halfThick = (float)thickness * 0.5f;
        int steps = (int)Math.max(1.0f, dist * 0.8f);
        for (int i = 0; i <= steps; ++i) {
            float f = (float)i / (float)steps;
            int px = (int)(x1 + f * dx);
            int py = (int)(y1 + f * dy);
            g.fill(px - (int)halfThick, py - (int)halfThick, px + (int)Math.ceil(halfThick) + 1, py + (int)Math.ceil(halfThick) + 1, color);
        }
    }

    private static int hsvToRgb(float h, float s, float v) {
        int hi = (int)(h * 6.0f) % 6;
        float f = h * 6.0f - (float)((int)(h * 6.0f));
        int p = (int)(v * (1.0f - s) * 255.0f);
        int q = (int)(v * (1.0f - f * s) * 255.0f);
        int t2 = (int)(v * (1.0f - (1.0f - f) * s) * 255.0f);
        int vi = (int)(v * 255.0f);
        return switch (hi) {
            case 0 -> vi << 16 | t2 << 8 | p;
            case 1 -> q << 16 | vi << 8 | p;
            case 2 -> p << 16 | vi << 8 | t2;
            case 3 -> p << 16 | q << 8 | vi;
            case 4 -> t2 << 16 | p << 8 | vi;
            default -> vi << 16 | p << 8 | q;
        };
    }

    private static enum State {
        WELCOME_PANEL,
        AURA_CINEMATIC;

    }
}

