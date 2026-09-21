package dile.ru.api.module.impl.visual;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;

public final class ItemReplacer extends Module {
    private static ItemReplacer instance;

    private static final String[] WEAPONS = {
            "Abominable Blade",
            "Abominable Great Saber",
            "Abominable Scythe",
            "Acidic Cleaver",
            "Amethyst Shuriken",
            "Ancient Royal Great Sword",
            "Aquatic Sacred Blade",
            "Arcanethyst",
            "Ashura's Blade",
            "Awakened Lichblade",
            "Blood Edge",
            "Bloody Death",
            "Bramblethorn",
            "Brimstone Claymore",
            "Carian Knight's Sword",
            "Chrono Blade",
            "Corrupted Mythic Blade",
            "Creation Splitter",
            "Crescent Rose",
            "Cyber Katana",
            "Cyber Mantis Blade",
            "Cyber Sword",
            "Cybernetic Chainsaw Blade",
            "Cybernetic Katana",
            "Cybernetic Knife",
            "Dainsleif",
            "Dark Blade",
            "Dark Cleaver",
            "Death Knight's Dagger",
            "Death Knight's Sword",
            "Demigod's Unholy Blade",
            "Demigod's Unholy Halberd",
            "Demon Lord's Great Axe",
            "Demon Lord's Sword",
            "Demonic Blade",
            "Demonic Cleaver",
            "Divine Axe Rhitta",
            "Divine Justice",
            "Divine Punisher",
            "Divine Reaper",
            "Dragon Slaying blade",
            "Edge Of The Astral Plane",
            "Emberblade",
            "Enigma",
            "Epic Sword",
            "Estoc",
            "Fallen God's Spear",
            "Fallen God's Sword",
            "Floral Longsword",
            "Floral Sabre",
            "Forest Guardian's Glaive",
            "Frost Axe",
            "Frost Blade",
            "Frost Scythe",
            "Hearthflame",
            "Hero Sword",
            "Holy Moonlight Sword",
            "Hornet's Needle",
            "Icewhisper",
            "Jade Halberd",
            "Katana",
            "Legendary Sword",
            "Longsword",
            "Magi Scythe",
            "Masamune",
            "Mjolnir",
            "Molten Blade",
            "Molten Sword",
            "Muramasa",
            "Mystical Spellblade",
            "Mythic Blade",
            "Ocean's Rage",
            "Partisan",
            "Pharaoh's Treasure",
            "Pheonix Grace",
            "Plague Longsword",
            "Power Fuse Hammer",
            "Power Fuse Sword",
            "Requiem of the Ninth Abyss",
            "Ribbon Cleaver",
            "Righteous Relic",
            "Rivers Of Blood",
            "Royal Chakram",
            "Royal Rapier",
            "Sabre",
            "Scissor Blade",
            "Sculk Cleaver",
            "Sculk Scythe",
            "Sculk Sword",
            "Sentinel's Will",
            "Silverine Blade",
            "Soul Claws",
            "Soul Collector",
            "Soul Devourer",
            "Soul Edge",
            "Soul Harvester",
            "Soul Stealer",
            "Soulrender",
            "Star's Edge",
            "Steel Sword",
            "Stop Sign",
            "Storm Bringer",
            "Storm's Edge",
            "Sunbreak",
            "Tengen's Blade",
            "Terra Blade",
            "Thousand Demon Daggers",
            "Thunder Bringer",
            "Thunderbrand",
            "True Excalibur",
            "Vampiric Needle",
            "Wakizashi",
            "Watcher Claymore",
            "Watching Warglaive",
            "Waxweaver",
            "Whisperwind",
            "Wickpiercer",
            "Wraith Scythe",
            "Yoru"
    };

    private final BooleanSetting selfOnly = register(new BooleanSetting("Self Only", "Only replace swords on yourself.", true));
    private final ModeSetting weaponModel = register(new ModeSetting("Weapon", "Selected weapon model.", WEAPONS[0], WEAPONS));

    public ItemReplacer() {
        super("Item Replacer", "Replaces sword models with custom weapons.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static ItemReplacer getInstance() {
        return instance;
    }

    public boolean isSelfOnly() {
        return selfOnly.getValue();
    }

    public String getSelectedWeapon() {
        return weaponModel.getValue();
    }
}
