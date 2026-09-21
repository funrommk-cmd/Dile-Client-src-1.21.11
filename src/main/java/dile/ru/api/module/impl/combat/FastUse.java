package dile.ru.api.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.MultiModeSetting;

public class FastUse extends Module {
    private final MultiModeSetting items = register(new MultiModeSetting(
            "Предметы", "Какие предметы автоматически использовать после полного натяжения.",
            new String[]{"Арбалет", "Лук", "Трезубец"}, "Арбалет", "Лук", "Трезубец"
    ));

    private boolean wasUsing;

    public FastUse() {
        super("FastUse", "Автоматически стреляет после полного натяжения.", ModuleCategory.COMBAT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            wasUsing = false;
            return;
        }

        if (!client.player.isUsingItem() || client.player.getUseItem().isEmpty()) {
            wasUsing = false;
            return;
        }

        int ticks = client.player.getTicksUsingItem();

        if (ticks > 0) {
            wasUsing = true;
        } else if (wasUsing) {
            wasUsing = false;
            return;
        }

        if (!wasUsing || ticks <= 0) {
            return;
        }

        if (isFullCharge(client, ticks)) {
            client.gameMode.releaseUsingItem(client.player);
            client.player.stopUsingItem();
            wasUsing = false;
        }
    }

    private boolean isFullCharge(Minecraft client, int ticks) {
        var stack = client.player.getUseItem();
        var item = stack.getItem();

        if (item instanceof CrossbowItem && items.isSelected("Арбалет")) {
            return CrossbowItem.isCharged(stack) || ticks >= CrossbowItem.getChargeDuration(stack, client.player);
        }
        if (item instanceof BowItem && items.isSelected("Лук")) {
            return BowItem.getPowerForTime(ticks) >= 1.0F;
        }
        if (item instanceof TridentItem && items.isSelected("Трезубец")) {
            return ticks >= 10;
        }

        return false;
    }

    @Override
    protected void onEnable() {
        wasUsing = false;
    }

    @Override
    protected void onDisable() {
        wasUsing = false;
    }
}
