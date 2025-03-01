/*
 * Copyright 2022 Siphalor
 * Copyright 2025 TerminalMC
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages rate-limited transmission of interaction events for client-side
 * manual inventory operations.
 */
public class InteractionManager {
    public static final Waiter TICK_WAITER =
            (TriggerType triggerType) -> triggerType == TriggerType.TICK;

    private static final Queue<@NotNull InteractionEvent> eventQueue = new ArrayDeque<>();
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private static ScheduledFuture<?> tickFuture;
    private static Waiter waiter = null;

    // Event queue management

    public static void pushClickEvent(int containerId, int slotId, int mouseButton, ClickType clickType) {
        push(new ClickEvent(containerId, slotId, mouseButton, clickType));
    }

    public static void pushCallbackEvent(Supplier<Waiter> callback) {
        push(new CallbackEvent(callback));
    }

    public static void pushPacketEvent(Packet<?> packet, Waiter waiter) {
        push(new PacketEvent(packet, waiter));
    }

    /**
     * Queues the specified event.
     */
    public static void push(@NotNull InteractionEvent event) {
        synchronized (eventQueue) {
            eventQueue.add(event);
            if (waiter == null) triggerSend(TriggerType.INITIAL);
        }
    }

    /**
     * Queues the specified events.
     */
    @SuppressWarnings("unused")
    public static void pushAll(Collection<@NotNull InteractionEvent> events) {
        synchronized (eventQueue) {
            eventQueue.addAll(events);
            if (waiter == null) triggerSend(TriggerType.INITIAL);
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
    public static void triggerSend(TriggerType triggerType) {
        synchronized (eventQueue) {
            if (waiter == null || waiter.trigger(triggerType)) {
                do {
                    InteractionEvent event = eventQueue.poll();
                    if (event == null) {
                        waiter = null;
                        break;
                    } else {
                        doSendEvent(event);
                    }
                } while (waiter.trigger(TriggerType.INITIAL));
            }
        }
    }

    /**
     * Sends the specified event.
     */
    private static void doSendEvent(@NotNull InteractionEvent event) {
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

    // Interaction manager state

    /**
     * Sets the tick rate of the interaction manager.
     * @param milliSeconds the time, in milliseconds, between ticks.
     */
    public static void setTickRate(long milliSeconds) {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        tickFuture = executor.scheduleAtFixedRate(InteractionManager::tick,
                milliSeconds, milliSeconds, TimeUnit.MILLISECONDS);
    }

    public static void setWaiter(Waiter waiter) {
        synchronized (eventQueue) {
            InteractionManager.waiter = waiter;
        }
    }

    public static void tick() {
        try {
            triggerSend(TriggerType.TICK);
        } catch (Exception e) {
            MoreMouseTweaks.LOG.error("Error while ticking InteractionManager", e);
        }
    }

    // Trigger type

    public enum TriggerType {
        INITIAL,
        CONTAINER_SLOT_UPDATE,
        HELD_ITEM_CHANGE,
        TICK
    }

    // Waiter

    @FunctionalInterface
    public interface Waiter {
        boolean trigger(TriggerType triggerType);

        @SuppressWarnings("unused")
        static Waiter equal(TriggerType triggerType) {
            return triggerType::equals;
        }
    }

    public static class SlotUpdateWaiter implements Waiter {
        int triggers;

        public SlotUpdateWaiter(int triggers) {
            this.triggers = triggers;
        }

        @Override
        public boolean trigger(TriggerType triggerType) {
            return triggerType == TriggerType.CONTAINER_SLOT_UPDATE && --triggers == 0;
        }
    }

    // Interaction event

    @FunctionalInterface
    public interface InteractionEvent {
        Waiter send();
    }

    public static class ClickEvent implements InteractionEvent {
        private final Waiter waiter;
        private final int containerId;
        private final int slotId;
        private final int mouseButton;
        private final ClickType clickType;

        public ClickEvent(int containerId, int slotId, int mouseButton, ClickType clickType) {
            this(containerId, slotId, mouseButton, clickType, TICK_WAITER);
        }

        public ClickEvent(int containerId, int slotId, int mouseButton, ClickType clickType, Waiter waiter) {
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
                mc.gameMode.handleInventoryMouseClick(
                        containerId, slotId, mouseButton, clickType, mc.player);
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

        @SuppressWarnings("unused")
        public PacketEvent(Packet<?> packet, int triggers) {
            this(packet, new SlotUpdateWaiter(triggers));
        }

        public PacketEvent(Packet<?> packet, Waiter waiter) {
            this.packet = packet;
            this.waiter = waiter;
        }

        @Override
        public Waiter send() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) {
                MoreMouseTweaks.LOG.error("Unable to send packet event: connection is null");
            } else {
                mc.getConnection().send(packet);
            }
            return waiter;
        }
    }
}
