/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.moremousetweaks.mixin.gui.other;

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
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
		if (screen instanceof IMerchantScreen) {
            ((IMerchantScreen)screen).mmt$setRecipeId(
                    this.index + ((IMerchantScreen)screen).mmt$getRecipeIdOffset());
			((IMerchantScreen)screen).mmt$syncRecipeId();
			if (screen instanceof AbstractContainerScreen) {
                InteractionManager.pushClickEvent(
                        ((AbstractContainerScreen<?>)screen).getMenu().containerId, 2, 
                        MouseButton.LEFT.getValue(), options().wholeStackModifier.isDown() 
                                ? ClickType.QUICK_MOVE : ClickType.PICKUP);
			}
		}
		return true;
	}
}
