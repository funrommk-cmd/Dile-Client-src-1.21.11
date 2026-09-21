package dile.ru.api.module.impl.player.autobuy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AutoBuyCatalog {
    private AutoBuyCatalog() {
    }

    public record Entry(String header, String itemId, String displayName) {
        public String search() {
            return displayName;
        }

        public ItemStack stack() {
            Identifier id = itemId.indexOf(':') >= 0
                    ? Identifier.tryParse(itemId)
                    : Identifier.withDefaultNamespace(itemId);
            if (id == null) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
        }
    }

    public record Group(String header, List<Entry> items) {
    }

    private static final List<Entry> ENTRIES = buildEntries();
    private static final List<Group> GROUPS = buildGroups(ENTRIES);

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static List<Group> groups() {
        return GROUPS;
    }

    private static List<Entry> buildEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry("ПвП предметы", "netherite_scrap", "Трапка"));
        entries.add(new Entry("ПвП предметы", "dried_kelp", "Пласт"));
        entries.add(new Entry("ПвП предметы", "ender_pearl", "Дезориентация"));
        entries.add(new Entry("ПвП предметы", "sugar", "Явная пыль"));
        entries.add(new Entry("ПвП предметы", "fire_charge", "Огненный смерч"));
        entries.add(new Entry("ПвП предметы", "snowball", "Снежок заморозка"));
        entries.add(new Entry("ПвП предметы", "phantom_membrane", "Божья Аура"));
        entries.add(new Entry("Сферы и талисманы", "nether_star", "Сфера"));
        entries.add(new Entry("Сферы и талисманы", "golden_apple", "Талисман"));
        entries.add(new Entry("Отмычки и ключи", "shears", "Отмычка"));
        entries.add(new Entry("Отмычки и ключи", "tripwire_hook", "Ключ"));
        entries.add(new Entry("Отмычки и ключи", "chest", "Кейс"));
        entries.add(new Entry("Книги и руны", "enchanted_book", "Книга зачарований"));
        entries.add(new Entry("Книги и руны", "netherite_ingot", "Руна"));
        entries.add(new Entry("Тотемы", "totem_of_undying", "Тотем бессмертия"));
        entries.add(new Entry("Незеритовая броня", "netherite_helmet", "Незеритовый шлем"));
        entries.add(new Entry("Незеритовая броня", "netherite_chestplate", "Незеритовый нагрудник"));
        entries.add(new Entry("Незеритовая броня", "netherite_leggings", "Незеритовые поножи"));
        entries.add(new Entry("Незеритовая броня", "netherite_boots", "Незеритовые ботинки"));
        entries.add(new Entry("Алмазная броня", "diamond_helmet", "Алмазный шлем"));
        entries.add(new Entry("Алмазная броня", "diamond_chestplate", "Алмазный нагрудник"));
        entries.add(new Entry("Алмазная броня", "diamond_leggings", "Алмазные поножи"));
        entries.add(new Entry("Алмазная броня", "diamond_boots", "Алмазные ботинки"));
        entries.add(new Entry("Оружие", "netherite_sword", "Незеритовый меч"));
        entries.add(new Entry("Оружие", "netherite_axe", "Незеритовый топор"));
        entries.add(new Entry("Оружие", "diamond_sword", "Алмазный меч"));
        entries.add(new Entry("Оружие", "bow", "Лук"));
        entries.add(new Entry("Оружие", "crossbow", "Арбалет"));
        entries.add(new Entry("Инструменты", "netherite_pickaxe", "Незеритовая кирка"));
        entries.add(new Entry("Инструменты", "diamond_pickaxe", "Алмазная кирка"));
        entries.add(new Entry("Еда", "enchanted_golden_apple", "Зачарованное золотое яблоко"));
        entries.add(new Entry("Еда", "golden_apple", "Золотое яблоко"));
        entries.add(new Entry("Еда", "cooked_beef", "Жареная говядина"));
        entries.add(new Entry("Еда", "cooked_porkchop", "Жареная свинина"));
        entries.add(new Entry("Расходники", "ender_pearl", "Жемчуг Края"));
        entries.add(new Entry("Расходники", "experience_bottle", "Бутылочка опыта"));
        entries.add(new Entry("Расходники", "firework_rocket", "Фейерверк-ракета"));
        entries.add(new Entry("Ценности", "nether_star", "Звезда Нижнего мира"));
        entries.add(new Entry("Ценности", "netherite_ingot", "Незеритовый слиток"));
        entries.add(new Entry("Ценности", "diamond", "Алмаз"));
        entries.add(new Entry("Ценности", "emerald", "Изумруд"));
        entries.add(new Entry("Ценности", "iron_ingot", "Серебро"));
        entries.add(new Entry("Прочее", "elytra", "Элитры"));
        entries.add(new Entry("Прочее", "shulker_box", "Шалкер-бокс"));
        return entries;
    }

    private static List<Group> buildGroups(List<Entry> entries) {
        Map<String, List<Entry>> byHeader = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byHeader.computeIfAbsent(entry.header(), ignored -> new ArrayList<>()).add(entry);
        }
        List<Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<Entry>> group : byHeader.entrySet()) {
            groups.add(new Group(group.getKey(), group.getValue()));
        }
        return groups;
    }
}
