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

package dev.terminalmc.moremousetweaks.mixin.gui.other;

import dev.terminalmc.moremousetweaks.config.Config;
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

@Mixin(targets = "net/minecraft/client/gui/screens/inventory/MerchantScreen$TradeOfferButton")
public class MixinTradeOfferButton implements ISpecialClickableButtonWidget {
	@Shadow
	@Final int index;

	@Override
	public boolean mouseClicked(int mouseButton) {
        if (mouseButton != 1 || !Config.get().options.quickCrafting) return false;
        Screen screen = Minecraft.getInstance().screen;
		if (screen instanceof IMerchantScreen) {
            ((IMerchantScreen) screen).mouseWheelie_setRecipeId(this.index + ((IMerchantScreen) screen).getRecipeIdOffset());
			((IMerchantScreen) screen).mouseWheelie_syncRecipeId();
			if (screen instanceof AbstractContainerScreen) {
				if (Config.get().options.wholeStackModifier.isDown()) {
                    InteractionManager.pushClickEvent(((AbstractContainerScreen<?>) screen).getMenu().containerId, 2, 1, ClickType.QUICK_MOVE);
                } else {
                    InteractionManager.pushClickEvent(((AbstractContainerScreen<?>) screen).getMenu().containerId, 2, 1, ClickType.PICKUP);
                }
			}
		}

		return true;
	}
}