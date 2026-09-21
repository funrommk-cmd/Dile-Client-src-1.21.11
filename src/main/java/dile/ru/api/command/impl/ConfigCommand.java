package dile.ru.api.command.impl;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import dile.ru.api.command.Command;
import dile.ru.api.command.CommandManager;
import dile.ru.api.config.ConfigManager;
import dile.ru.manager.Manager;
import dile.ru.utils.repository.RepositoryStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Configuration management", "cfg");
    }

    @Override
    public void execute(String label, String[] args) {
        String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "save";
        switch (action) {
            case "save" -> {
                if (args.length < 2) {
                    if (Manager.getInstance() != null && Manager.getInstance().getConfigManager() != null) {
                        Manager.getInstance().getConfigManager().saveAll();
                    }
                    logDirect("Config saved.", ChatFormatting.GREEN);
                } else {
                    saveNamed(args[1]);
                }
            }
            case "load" -> {
                if (args.length < 2) {
                    logDirect("Usage: cfg load <name>", ChatFormatting.RED);
                } else {
                    loadNamed(args[1]);
                }
            }
            case "list" -> listConfigs();
            case "delete" -> {
                if (args.length < 2) {
                    logDirect("Usage: cfg delete <name>", ChatFormatting.RED);
                } else {
                    deleteNamed(args[1]);
                }
            }
            case "dir" -> {
                try {
                    openDirectory(ConfigManager.userConfigDirectory());
                    logDirect("Config folder opened.", ChatFormatting.GREEN);
                } catch (IOException exception) {
                    logDirect("Failed to open config folder: " + exception.getMessage(), ChatFormatting.RED);
                }
            }
            case "system" -> {
                try {
                    openDirectory(RepositoryStorage.root());
                    logDirect("System folder opened.", ChatFormatting.GREEN);
                } catch (IOException exception) {
                    logDirect("Failed to open system folder: " + exception.getMessage(), ChatFormatting.RED);
                }
            }
            default -> {
                logDirect("Usage: cfg save [name] | cfg load <name> | cfg delete <name> | cfg list | cfg dir | cfg system");
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return Stream.of("save", "load", "list", "delete", "dir", "system").filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)));
        }
        if (args.length == 2 && ("save".equals(args[0].toLowerCase(Locale.ROOT)) || "load".equals(args[0].toLowerCase(Locale.ROOT)) || "delete".equals(args[0].toLowerCase(Locale.ROOT)))) {
            return listConfigNames().filter(value -> value.startsWith(args[1].toLowerCase(Locale.ROOT)));
        }
        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Configuration management.", "Usage:", "> cfg save [name]", "> cfg load <name>", "> cfg delete <name>", "> cfg list", "> cfg dir", "> cfg system");
    }

    private void saveNamed(String name) {
        try {
            Path dir = ConfigManager.userConfigDirectory();
            Files.createDirectories(dir);
            Path file = dir.resolve(name + ConfigManager.CONFIG_EXTENSION);
            if (Manager.getInstance() != null && Manager.getInstance().getConfigManager() != null) {
                Manager.getInstance().getConfigManager().saveAll();
            }
            Path systemModules = ConfigManager.systemDirectory().resolve("modules" + ConfigManager.CONFIG_EXTENSION);
            if (Files.exists(systemModules)) {
                Files.copy(systemModules, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            logDirect("Config saved as: " + name, ChatFormatting.GREEN);
        } catch (IOException exception) {
            logDirect("Failed to save config: " + exception.getMessage(), ChatFormatting.RED);
        }
    }

    private void loadNamed(String name) {
        try {
            Path file = ConfigManager.userConfigDirectory().resolve(name + ConfigManager.CONFIG_EXTENSION);
            if (!Files.exists(file)) {
                logDirect("Config not found: " + name, ChatFormatting.RED);
                return;
            }
            if (Manager.getInstance() != null && Manager.getInstance().getConfigManager() != null) {
                Manager.getInstance().getConfigManager().saveAll();
            }
            Path systemModules = ConfigManager.systemDirectory().resolve("modules" + ConfigManager.CONFIG_EXTENSION);
            Files.copy(file, systemModules, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (Manager.getInstance() != null && Manager.getInstance().getConfigManager() != null) {
                Manager.getInstance().getConfigManager().loadAll();
            }
            logDirect("Config loaded: " + name, ChatFormatting.GREEN);
        } catch (IOException exception) {
            logDirect("Failed to load config: " + exception.getMessage(), ChatFormatting.RED);
        }
    }

    private void deleteNamed(String name) {
        try {
            Path file = ConfigManager.userConfigDirectory().resolve(name + ConfigManager.CONFIG_EXTENSION);
            if (!Files.exists(file)) {
                logDirect("Config not found: " + name, ChatFormatting.RED);
                return;
            }
            Files.delete(file);
            logDirect("Config deleted: " + name, ChatFormatting.GREEN);
        } catch (IOException exception) {
            logDirect("Failed to delete config: " + exception.getMessage(), ChatFormatting.RED);
        }
    }

    private void listConfigs() {
        String prefix = CommandManager.getInstance().getPrefix();
        List<String> names = listConfigNames().toList();
        if (names.isEmpty()) {
            logDirect("No configs found in: " + ConfigManager.userConfigDirectory(), ChatFormatting.GRAY);
            return;
        }
        logDirect("Configs (" + names.size() + "):", ChatFormatting.GREEN);
        for (String name : names) {
            MutableComponent line = Component.literal("  ").withStyle(ChatFormatting.WHITE);
            line.append(Component.literal(name).withStyle(ChatFormatting.WHITE));

            MutableComponent loadHover = Component.literal("Load config: " + name).withStyle(ChatFormatting.GREEN);
            line.append(Component.literal(" "));
            line.append(Component.literal("[")
                    .withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.literal("Load").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("]").withStyle(ChatFormatting.DARK_GREEN))
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(loadHover))
                            .withClickEvent(new ClickEvent.RunCommand(prefix + "cfg load " + name))));

            MutableComponent deleteHover = Component.literal("Delete config: " + name).withStyle(ChatFormatting.RED);
            line.append(Component.literal(" "));
            line.append(Component.literal("[")
                    .withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal("Delete").withStyle(ChatFormatting.RED))
                    .append(Component.literal("]").withStyle(ChatFormatting.DARK_RED))
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(deleteHover))
                            .withClickEvent(new ClickEvent.RunCommand(prefix + "cfg delete " + name))));

            logDirectRaw(line);
        }
    }

    private Stream<String> listConfigNames() {
        Path dir = ConfigManager.userConfigDirectory();
        if (!Files.exists(dir)) {
            return Stream.empty();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.toString().endsWith(ConfigManager.CONFIG_EXTENSION))
                    .map(p -> p.getFileName().toString())
                    .map(n -> n.substring(0, n.length() - ConfigManager.CONFIG_EXTENSION.length()))
                    .sorted()
                    .toList()
                    .stream();
        } catch (IOException exception) {
            return Stream.empty();
        }
    }

    private void openDirectory(Path directory) throws IOException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        ProcessBuilder builder = os.contains("win")
                ? new ProcessBuilder("explorer.exe", directory.toAbsolutePath().toString())
                : os.contains("mac")
                ? new ProcessBuilder("open", directory.toAbsolutePath().toString())
                : new ProcessBuilder("xdg-open", directory.toAbsolutePath().toString());
        builder.start();
    }
}
