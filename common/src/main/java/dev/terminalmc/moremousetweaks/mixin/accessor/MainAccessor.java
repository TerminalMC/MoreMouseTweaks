package dev.terminalmc.moremousetweaks.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yalter.mousetweaks.IGuiScreenHandler;
import yalter.mousetweaks.Main;

@Mixin(Main.class)
public interface MainAccessor {
    @Accessor("handler")
    static IGuiScreenHandler getHandler() {
        throw new AssertionError();
    }
}
