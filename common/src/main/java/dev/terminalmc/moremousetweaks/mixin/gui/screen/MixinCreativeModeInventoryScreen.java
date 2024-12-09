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

package dev.terminalmc.moremousetweaks.mixin.gui.screen;

import dev.terminalmc.moremousetweaks.util.ScrollAction;
import dev.terminalmc.moremousetweaks.util.inject.ISpecialScrollableScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Creative inventory tab scrolling.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen 
        extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> 
        implements ISpecialScrollableScreen {

    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    protected abstract void selectTab(CreativeModeTab itemGroup_1);

    @Shadow
    protected abstract void slotClicked(@NotNull Slot slot, int invSlot, int button, 
                                        @NotNull ClickType slotActionType);

    public MixinCreativeModeInventoryScreen(CreativeModeInventoryScreen.ItemPickerMenu menu, 
                                            Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public ScrollAction mmt$onMouseScrolledSpecial(double mouseX, double mouseY, double scrollAmount) {
        if (options().scrollCreativeTabs) {
            double relMouseY = mouseY - this.topPos;
            double relMouseX = mouseX - this.leftPos;
            boolean yOverTopTabs = (-32 <= relMouseY) && (relMouseY <= 0);
            boolean yOverBottomTabs = (this.imageHeight <= relMouseY) 
                    && (relMouseY <= this.imageHeight + 32);
            boolean overTabs = (0 <= relMouseX) && (relMouseX <= this.imageWidth) 
                    && (yOverTopTabs || yOverBottomTabs);

            if (overTabs) {
                List<CreativeModeTab> groupsToDisplay = CreativeModeTabs.tabs();
                int selectedTabIndex = groupsToDisplay.indexOf(selectedTab);
                if (selectedTabIndex < 0) {
                    return ScrollAction.FAILURE;
                }
                selectTab(groupsToDisplay.get(
                        Mth.clamp((int)(selectedTabIndex + Math.round(scrollAmount)), 0, 
                                groupsToDisplay.size() - 1)));
                return ScrollAction.SUCCESS;
            }
        }

        return ScrollAction.PASS;
    }
}
