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

import dev.terminalmc.moremousetweaks.MoreMouseTweaks;
import dev.terminalmc.moremousetweaks.config.Config;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.ScrollAction;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookResults;
import dev.terminalmc.moremousetweaks.util.inject.IRecipeBookWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeBookComponent.class)
public abstract class MixinRecipeBookComponent implements IRecipeBookWidget {
	@Shadow 
    @Final private RecipeBookPage recipeBookPage;
	@Shadow 
    private int width;
	@Shadow 
    private int xOffset;
	@Shadow 
    @Final private List<RecipeBookTabButton> tabButtons;
	@Shadow 
    private RecipeBookTabButton selectedTab;
	@Shadow 
    protected abstract void updateCollections(boolean boolean_1);
	@Shadow
	private int height;
	@Shadow
	public abstract boolean isVisible();
	@Shadow
	private boolean ignoreTextInput;
	@Shadow 
    protected Minecraft minecraft;
	@Shadow 
    @Final private StackedContents stackedContents;
	@Shadow 
    protected RecipeBookMenu<?, ?> menu;

	@Override
	public ScrollAction mouseWheelie_scrollRecipeBook(double mouseX, double mouseY, double scrollAmount) {
		if (!this.isVisible())
			return ScrollAction.PASS;
		int top = (this.height - 166) / 2;
		if (mouseY < top || mouseY >= top + 166)
			return ScrollAction.PASS;
		int left = (this.width - 147) / 2 - this.xOffset;
		if (mouseX >= left && mouseX < left + 147) {
			// Ugly approach since assigning the casted value causes a runtime mixin error
			int maxPage = ((IRecipeBookResults) recipeBookPage).mouseWheelie_getPageCount() - 1;
			((IRecipeBookResults) recipeBookPage).mouseWheelie_setCurrentPage(Mth.clamp((int) (((IRecipeBookResults) recipeBookPage).mouseWheelie_getCurrentPage() + Math.round(scrollAmount)), 0, Math.max(maxPage, 0)));
			((IRecipeBookResults) recipeBookPage).mouseWheelie_refreshResultButtons();
			return ScrollAction.SUCCESS;
		} else if(mouseX >= left - 30 && mouseX < left) {
			int index = tabButtons.indexOf(selectedTab);
			int newIndex = Mth.clamp(index + (int) (Math.round(scrollAmount)), 0, tabButtons.size() - 1);
			if (newIndex != index) {
				selectedTab.setStateTriggered(false);
				selectedTab = tabButtons.get(newIndex);
				selectedTab.setStateTriggered(true);
				updateCollections(true);
			}
			return ScrollAction.SUCCESS;
		}
		return ScrollAction.PASS;
	}

	@Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handlePlaceRecipe(ILnet/minecraft/world/item/crafting/RecipeHolder;Z)V", shift = At.Shift.AFTER))
	public void mouseClicked(double x, double y, int mouseButton, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		if (Config.get().options.quickCrafting & mouseButton == 1) {
			int resSlot = menu.getResultSlotIndex();
			RecipeHolder<?> recipe = recipeBookPage.getLastClickedRecipe();
			if (canCraftMore(recipe)) {
				InteractionManager.clear();
				InteractionManager.setWaiter((InteractionManager.TriggerType triggerType) -> MoreMouseTweaks.lastUpdatedSlot >= menu.getSize());
			}
			InteractionManager.pushClickEvent(menu.containerId, resSlot, 0, Config.get().options.wholeStackModifier.isDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void keyPressed(int int1, int int2, int int3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		if (Config.get().options.quickCrafting && isVisible() && !minecraft.player.isSpectator()) {
			if (Minecraft.getInstance().options.keyDrop.matches(int1, int2)) {
				ignoreTextInput = false;
				RecipeHolder<?> oldRecipeEntry = recipeBookPage.getLastClickedRecipe();
				if (this.recipeBookPage.mouseClicked(MoreMouseTweaks.getMouseX(), MoreMouseTweaks.getMouseY(), 0, (this.width - 147) / 2 - this.xOffset, (this.height - 166) / 2, 147, 166)) {
					RecipeHolder<?> recipeEntry = recipeBookPage.getLastClickedRecipe();
					RecipeCollection resultCollection = recipeBookPage.getLastClickedRecipeCollection();
					if (!resultCollection.isCraftable(recipeEntry)) {
						return;
					}
					int resSlot = menu.getResultSlotIndex();
					if (Config.get().options.allOfKindModifier.isDown()) {
						if (oldRecipeEntry != recipeEntry || menu.slots.get(resSlot).getItem().isEmpty() || canCraftMore(recipeEntry)) {
							InteractionManager.push(new InteractionManager.PacketEvent(new ServerboundPlaceRecipePacket(menu.containerId, recipeEntry, true), (triggerType) -> MoreMouseTweaks.lastUpdatedSlot >= menu.getSize()));
						}
						int cnt = stackedContents.getBiggestCraftableStack(recipeEntry, recipeEntry.value().getResultItem(minecraft.level.registryAccess()).getMaxStackSize(), null);
						for (int i = 1; i < cnt; i++) {
							InteractionManager.pushClickEvent(menu.containerId, resSlot, 1, ClickType.THROW);
						}
					} else {
						if (oldRecipeEntry != recipeEntry || menu.slots.get(resSlot).getItem().isEmpty()) {
							InteractionManager.push(new InteractionManager.PacketEvent(new ServerboundPlaceRecipePacket(menu.containerId, recipeEntry, false), (triggerType) -> MoreMouseTweaks.lastUpdatedSlot >= menu.getSize()));
						}
					}
					InteractionManager.push(new InteractionManager.CallbackEvent(() -> {
						minecraft.gameMode.handleInventoryMouseClick(menu.containerId, menu.getResultSlotIndex(), 0, ClickType.THROW, minecraft.player);
						updateCollections(false);
						return InteractionManager.TICK_WAITER;
					}, true));
					callbackInfoReturnable.setReturnValue(true);
				}
			}
		}
	}

	@Unique
	private boolean canCraftMore(RecipeHolder<?> recipeEntry) {
		return getBiggestCraftingStackSize() < stackedContents.getBiggestCraftableStack(recipeEntry, recipeEntry.value().getResultItem(minecraft.level.registryAccess()).getMaxStackSize(), null);
	}

	@Unique
	private int getBiggestCraftingStackSize() {
		int resSlot = menu.getResultSlotIndex();
		int cnt = 0;
		for (int i = 0; i < menu.getSize(); i++) {
			if (i == resSlot) continue;
			cnt = Math.max(cnt, menu.slots.get(i).getItem().getCount());
		}
		return cnt;
	}
}
