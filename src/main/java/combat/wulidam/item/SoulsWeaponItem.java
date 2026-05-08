package combat.wulidam.item;

import combat.wulidam.combat.WeaponData;
import combat.wulidam.combat.WeaponRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Base class for all souls-like combat weapons..
 * Sword settings: durability, enchantability, attack attributes, weapon component
 * are applied via ToolMaterial.applySwordSettings() in the Settings passed to the constructor.
 */
public class SoulsWeaponItem extends Item {
    private final Identifier weaponDataId;

    public SoulsWeaponItem(Settings settings, Identifier weaponDataId) {
        super(settings);
        this.weaponDataId = weaponDataId;
    }

    /**
     * @return the identifier used to look up this weapon's combat data in the WeaponRegistry.
     */
    public Identifier getWeaponDataId() {
        return weaponDataId;
    }

    /**
     * @return the WeaponData for this item, or null if not yet loaded.
     */
    public WeaponData getWeaponData() {
        return WeaponRegistry.getWeaponData(weaponDataId);
    }

    @Override
    public void appendTooltip(
            ItemStack stack, Item.TooltipContext context,
            net.minecraft.component.type.TooltipDisplayComponent displayComponent,
            Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);

        WeaponData data = getWeaponData();
        if (data != null) {
            textConsumer.accept(Text.empty());
            textConsumer.accept(Text.translatable("tooltip.soulslikecombat.combat_stats")
                    .formatted(Formatting.GOLD, Formatting.BOLD));
            textConsumer.accept(Text.translatable("tooltip.soulslikecombat.damage",
                    String.format("%.1f", data.baseDamage())).formatted(Formatting.RED));
            textConsumer.accept(Text.translatable("tooltip.soulslikecombat.range",
                    String.format("%.1f", data.attackRange())).formatted(Formatting.YELLOW));
            textConsumer.accept(Text.translatable("tooltip.soulslikecombat.combo",
                    data.comboCount()).formatted(Formatting.GREEN));
            textConsumer.accept(Text.translatable("tooltip.soulslikecombat.parry_window",
                    data.parryWindowTicks()).formatted(Formatting.AQUA));
        }
    }
}
