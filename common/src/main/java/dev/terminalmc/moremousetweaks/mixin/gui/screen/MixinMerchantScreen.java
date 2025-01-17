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

package dev.terminalmc.moremousetweaks.mixin.gui.screen;

import dev.terminalmc.moremousetweaks.util.inject.IMerchantScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Quick-trading helper for merchant screens.
 */
@Mixin(MerchantScreen.class)
public abstract class MixinMerchantScreen implements IMerchantScreen {
    @Shadow 
    private int shopItem;

    @Shadow
    protected abstract void postButtonClick();

    @Shadow
    int scrollOff;

    @Override
    public void mmt$setRecipeId(int id) {
        shopItem = id;
    }

    @Override
    public void mmt$syncRecipeId() {
        postButtonClick();
    }

    @Override
    public int mmt$getRecipeIdOffset() {
        return scrollOff;
    }
}
