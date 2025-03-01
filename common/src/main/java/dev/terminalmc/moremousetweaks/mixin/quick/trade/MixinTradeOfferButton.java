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

package dev.terminalmc.moremousetweaks.mixin.quick.trade;

import dev.terminalmc.moremousetweaks.util.inject.IMerchantScreen;
import dev.terminalmc.moremousetweaks.util.inject.ISpecialClickableButtonWidget;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import yalter.mousetweaks.MouseButton;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Quick-trading via RMB click.
 */
@Mixin(targets = "net/minecraft/client/gui/screens/inventory/MerchantScreen$TradeOfferButton")
public class MixinTradeOfferButton implements ISpecialClickableButtonWidget {
    @Shadow
    @Final int index;

    @Override
    public boolean mmt$mouseClicked(int mouseButton) {
        if (!options().quickCrafting || mouseButton != MouseButton.RIGHT.getValue()) return false;
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof IMerchantScreen merchantScreen) {
            merchantScreen.mmt$setRecipeId(this.index + merchantScreen.mmt$getRecipeIdOffset());
            merchantScreen.mmt$syncRecipeId();
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                InteractionManager.pushClickEvent(containerScreen.getMenu().containerId, 2,
                        MouseButton.LEFT.getValue(), Screen.hasShiftDown()
                                ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            }
        }
        return true;
    }
}
