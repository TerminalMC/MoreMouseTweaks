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

package dev.terminalmc.moremousetweaks.mixin.quick.trade;

import dev.terminalmc.moremousetweaks.inventory.helper.ComparisonHelper;
import dev.terminalmc.moremousetweaks.inventory.helper.InteractionHelper;
import dev.terminalmc.moremousetweaks.network.InteractionManager;
import dev.terminalmc.moremousetweaks.util.InputUtil;
import dev.terminalmc.moremousetweaks.util.KeyUtil;
import dev.terminalmc.moremousetweaks.util.inject.IMerchantMenu;
import dev.terminalmc.moremousetweaks.util.inject.IMerchantScreen;
import dev.terminalmc.moremousetweaks.util.inject.ISpecialClickableButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import yalter.mousetweaks.MouseButton;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.terminalmc.moremousetweaks.config.Config.options;

/**
 * Quick-trading.
 */
@Mixin(targets = "net/minecraft/client/gui/screens/inventory/MerchantScreen$TradeOfferButton")
public abstract class TradeOfferButtonMixin implements ISpecialClickableButtonWidget {

    @Shadow
    @Final
    int index;

    @Unique
    private IMerchantScreen mmt$iMerchantScreen;

    @Unique
    private MerchantMenu mmt$merchantMenu;

    @Unique
    private IMerchantMenu mmt$iMerchantMenu;

    /**
     * Quick-trading via RMB click.
     */
    @Override
    public boolean mmt$mouseClicked(int mouseButton) {
        if (!options().useQuickTrading || mouseButton != MouseButton.RIGHT.getValue())
            return false;

        Screen screen = Minecraft.getInstance().screen;
        boolean allCast = false;

        if (screen instanceof AbstractContainerScreen<?> cs) {
            if (cs instanceof MerchantScreen ms) {
                if (ms instanceof IMerchantScreen ims) {
                    mmt$iMerchantScreen = ims;
                    if (cs.getMenu() instanceof MerchantMenu mm) {
                        mmt$merchantMenu = mm;
                        if (mm instanceof IMerchantMenu imm) {
                            mmt$iMerchantMenu = imm;
                            allCast = true;
                        }
                    }
                }
            }
        }
        if (!allCast)
            return false;

        int offerId = index + mmt$iMerchantScreen.mmt$getOfferIdOffset();
        mmt$iMerchantScreen.mmt$setOfferId(offerId);
        mmt$iMerchantScreen.mmt$syncOfferId();

        if (KeyUtil.hasShiftDown() && InputUtil.isMatchingSlotsKeyDown()) {
            mmt$bulkQuickTrade(offerId);
        } else {
            InteractionHelper.pickup(
                    mmt$merchantMenu.containerId,
                    mmt$iMerchantMenu.mmt$getResultSlot()
            );
        }

        return true;
    }

    /**
     * Exhaustively select and shift-trade the offer.
     * <p>
     * Figuring out how many trades are needed, with the info we have at this point, is possible but
     * annoying. Instead, we queue the theoretical max number of interactions and abort early if the
     * trade becomes unavailable.
     */
    @Unique
    private void mmt$bulkQuickTrade(int offerId) {
        MerchantOffer offer = mmt$iMerchantMenu.mmt$getOffer(offerId);
        int containerId = mmt$merchantMenu.containerId;
        int resultSlotId = mmt$iMerchantMenu.mmt$getResultSlot();

        // Count the number of nonempty inventory slots
        int maxOps = 0;
        for (Slot slot : Minecraft.getInstance().player.containerMenu.slots) {
            if (slot.hasItem()) {
                maxOps++;
            }
        }
        // Normally the loop will finish naturally before the interaction manager aborts, but we
        // can't guarantee that so this allows aborting the loop as well
        AtomicBoolean abort = new AtomicBoolean(false);

        // Queue interactions
        for (int i = 0; i < maxOps; i++) {
            if (abort.get())
                break;
            // Select the recipe
            InteractionManager.pushCallbackEvent(() -> {
                mmt$iMerchantScreen.mmt$setOfferId(offerId);
                mmt$iMerchantScreen.mmt$syncOfferId();
                // Check abort condition
                if (!mmt$canBuy(offer)) {
                    abort.set(true);
                    InteractionManager.clear();
                }
                return InteractionManager.TICK_WAITER;
            });
            // Shift-click the result slot
            InteractionHelper.quickMove(containerId, resultSlotId);
        }
    }

    @Unique
    private boolean mmt$canBuy(MerchantOffer offer) {
        if (offer.isOutOfStock())
            return false;

        ItemCost costA = offer.getItemCostA();
        if (!ComparisonHelper.playerHas(costA.itemStack(), costA.count()))
            return false;

        Optional<ItemCost> costB = offer.getItemCostB();
        return costB.isEmpty()
                || ComparisonHelper.playerHas(costB.get().itemStack(), costB.get().count());
    }
}
