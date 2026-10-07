/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
 *  net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.minecraft.particle.ParticleEffect
 *  net.minecraft.particle.ParticleTypes
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.network.ClientPlayerEntity
 */
package com.cobbleverse.retypedgyms.client;

import com.cobbleverse.retypedgyms.client.SpawnRevealEffect;
import com.cobbleverse.retypedgyms.gui.WelcomeScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(value=EnvType.CLIENT)
public class RetypedGymsClient
implements ClientModInitializer {
    private static boolean pendingWelcome = false;
    private static boolean shownThisSession = false;
    private static int auraTicksRemaining = 0;

    public static void triggerSpawnAura() {
        auraTicksRemaining = 36;
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.1f, 0.8f);
        }
        SpawnRevealEffect.trigger();
    }

    public void onInitializeClient() {
        SpawnRevealEffect.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!shownThisSession) {
                pendingWelcome = true;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingWelcome && client.player != null && client.currentScreen == null) {
                pendingWelcome = false;
                shownThisSession = true;
                client.setScreen((net.minecraft.client.gui.screen.Screen)new WelcomeScreen());
            }
            if (auraTicksRemaining > 0 && client.player != null && client.world != null) {
                RetypedGymsClient.spawnInWorldAura(client, --auraTicksRemaining);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register((LiteralArgumentBuilder)ClientCommandManager.literal((String)"gymwelcome").executes(context -> {
                pendingWelcome = true;
                return 1;
            }));
            dispatcher.register((LiteralArgumentBuilder)ClientCommandManager.literal((String)"gymreveal").executes(context -> {
                RetypedGymsClient.triggerSpawnAura();
                return 1;
            }));
        });
    }

    private static void spawnInWorldAura(net.minecraft.client.MinecraftClient client, int ticksLeft) {
        net.minecraft.client.network.ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        if (ticksLeft >= 32) {
            int step = 36 - ticksLeft;
            double radius = (double)step * 0.65;
            for (int i = 0; i < 16; ++i) {
                double theta = (double)i * Math.PI * 2.0 / 16.0;
                double rx = px + Math.cos(theta) * radius;
                double rz = pz + Math.sin(theta) * radius;
                client.world.addParticle((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, rx, py + 0.05, rz, 0.0, 0.01, 0.0);
            }
        }
        for (int i = 0; i < 3; ++i) {
            double ox = (Math.random() - 0.5) * 1.5;
            double oy = Math.random() * 1.5;
            double oz = (Math.random() - 0.5) * 1.5;
            client.world.addParticle((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, px + ox, py + oy, pz + oz, 0.0, 0.02, 0.0);
        }
        if (ticksLeft % 3 == 0) {
            double ox = (Math.random() - 0.5) * 0.8;
            double oz = (Math.random() - 0.5) * 0.8;
            client.world.addParticle((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.SCULK_CHARGE_POP, px + ox, py + 0.1, pz + oz, 0.0, 0.03, 0.0);
        }
        if (ticksLeft == 18) {
            player.playSound(net.minecraft.sound.SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.85f, 1.0f);
        }
    }
}

