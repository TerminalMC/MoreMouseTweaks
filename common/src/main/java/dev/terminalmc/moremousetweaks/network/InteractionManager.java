/*
 * Copyright 2022 Siphalor
 * Copyright 2026 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.moremousetweaks.network;

import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.ContainerInput;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages rate-limited transmission of interaction events for client-side inventory manipulation
 * operations.
 */
public class InteractionManager {

    public static final Waiter TICK_WAITER = (TriggerType type) -> type == TriggerType.TICK;

    private static final Queue<InteractionEvent> eventQueue = new ArrayDeque<>();
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private static ScheduledFuture<?> tickFuture;
    private static Waiter waiter = null;

    // Shorthand helper methods

    public static void pushClickEvent(
            int containerId,
            int slotId,
            int mouseButton,
            ContainerInput clickType
    ) {
        push(new ClickEvent(containerId, slotId, mouseButton, clickType, TICK_WAITER));
    }

    public static void pushCallbackEvent(Supplier<Waiter> callback) {
        push(new CallbackEvent(callback));
    }

    public static void pushPacketEvent(Packet<?> packet, Waiter waiter) {
        push(new PacketEvent(packet, waiter));
    }

    /**
     * Queues the event.
     */
    public static void push(InteractionEvent event) {
        synchronized (eventQueue) {
            eventQueue.add(event);
            if (waiter == null)
                triggerSend(TriggerType.INITIAL);
        }
    }

    /**
     * Clears the event queue.
     */
    public static void clear() {
        synchronized (eventQueue) {
            eventQueue.clear();
            waiter = null;
        }
    }

    /**
     * Initiates sending of all queued events.
     */
    public static void triggerSend(TriggerType type) {
        synchronized (eventQueue) {
            if (waiter == null || waiter.trigger(type)) {
                do {
                    InteractionEvent event = eventQueue.poll();
                    if (event == null) {
                        waiter = null;
                        break;
                    }

                    doSendEvent(event);
                } while (waiter.trigger(TriggerType.INITIAL));
            }
        }
    }

    /**
     * Sends the specified event.
     */
    private static void doSendEvent(InteractionEvent event) {
        Waiter blockingWaiter = triggerType -> false;
        waiter = blockingWaiter;
        Minecraft.getInstance().execute(() -> {
            synchronized (eventQueue) {
                if (waiter == blockingWaiter) {
                    waiter = event.send();
                }
            }
        });
    }

    public static void setWaiter(Waiter waiter) {
        synchronized (eventQueue) {
            InteractionManager.waiter = waiter;
        }
    }

    /**
     * Sets the tick rate of the interaction manager.
     *
     * @param intervalMs the time, in milliseconds, between ticks.
     */
    public static void setTickRate(long intervalMs) {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        tickFuture = executor.scheduleAtFixedRate(
                InteractionManager::tick,
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    public static void tick() {
        try {
            triggerSend(TriggerType.TICK);
        } catch (Exception e) {
            MoreMouseTweaks.LOG.error("Error while ticking InteractionManager", e);
        }
    }

    @FunctionalInterface
    public interface Waiter {

        boolean trigger(TriggerType type);

        @SuppressWarnings("unused")
        static Waiter equal(TriggerType type) {
            return type::equals;
        }
    }

    public enum TriggerType {
        INITIAL,
        CONTAINER_SLOT_UPDATE,
        HELD_ITEM_CHANGE,
        TICK
    }

    @FunctionalInterface
    public interface InteractionEvent {

        Waiter send();
    }

    public static class ClickEvent implements InteractionEvent {

        private final Waiter waiter;
        private final int containerId;
        private final int slotId;
        private final int mouseButton;
        private final ContainerInput clickType;

        public ClickEvent(
                int containerId,
                int slotId,
                int mouseButton,
                ContainerInput clickType,
                Waiter waiter
        ) {
            this.containerId = containerId;
            this.slotId = slotId;
            this.mouseButton = mouseButton;
            this.clickType = clickType;
            this.waiter = waiter;
        }

        @Override
        public Waiter send() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode == null) {
                MoreMouseTweaks.LOG.error("Unable to send click event: gameMode is null");
            } else if (mc.player == null) {
                MoreMouseTweaks.LOG.error("Unable to send click event: player is null");
            } else {
                mc.gameMode.handleContainerInput(
                        containerId,
                        slotId,
                        mouseButton,
                        clickType,
                        mc.player
                );
            }
            return waiter;
        }
    }

    public static class CallbackEvent implements InteractionEvent {

        private final Supplier<Waiter> callback;

        public CallbackEvent(Supplier<Waiter> callback) {
            this.callback = callback;
        }

        @Override
        public Waiter send() {
            return callback.get();
        }
    }

    public static class PacketEvent implements InteractionEvent {

        private final Packet<?> packet;
        private final Waiter waiter;

        public PacketEvent(Packet<?> packet, Waiter waiter) {
            this.packet = packet;
            this.waiter = waiter;
        }

        @Override
        public Waiter send() {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection == null) {
                MoreMouseTweaks.LOG.error("Unable to send packet event: connection is null");
            } else {
                connection.send(packet);
            }
            return waiter;
        }
    }
}
