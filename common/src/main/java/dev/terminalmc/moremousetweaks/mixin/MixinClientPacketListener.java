/*
 * Copyright 2020-2022 Siphalor
 * Copyright 2024 TerminalMC
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

package dev.terminalmc.moremousetweaks.mixin;

import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener extends ClientCommonPacketListenerImpl {
    protected MixinClientPacketListener(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
    public void onHeldItemChangeBegin(ClientboundSetCarriedItemPacket packet, CallbackInfo ci) {
        InteractionManager.triggerSend(InteractionManager.TriggerType.HELD_ITEM_CHANGE);
    }

    @Inject(method = "handleContainerSetSlot", at = @At("RETURN"))
    public void onGuiSlotUpdateBegin(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        MoreMouseTweaks.lastUpdatedSlot = packet.getSlot();
        InteractionManager.triggerSend(InteractionManager.TriggerType.CONTAINER_SLOT_UPDATE);
    }
}