package au.com.addstar.attributioner;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttributionerCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("regions", "debug", "info", "reload", "timed", "apply", "remove");
    private static final String TIMED_USAGE =
            "<red>Usage: /attributioner timed \\<player> \\<attribute> \\<amount> \\<operation> \\<seconds>";
    private static final String APPLY_USAGE =
            "<red>Usage: /attributioner apply \\<player> \\<attribute> \\<amount> \\<operation>";
    private static final String REMOVE_USAGE =
            "<red>Usage: /attributioner remove \\<player> \\<attribute|all>";
    private static final List<String> HELP = List.of(
            "<yellow>Attributioner commands:",
            "<gold>/attributioner regions</gold> <gray>- List configured region attributes",
            "<gold>/attributioner info \\<player></gold> <gray>- Show a player's Attributioner modifiers",
            "<gold>/attributioner apply \\<player> \\<attribute> \\<amount> \\<operation></gold> <gray>- Apply a modifier until removed",
            "<gold>/attributioner timed \\<player> \\<attribute> \\<amount> \\<operation> \\<seconds></gold> <gray>- Apply a modifier for a number of seconds",
            "<gold>/attributioner remove \\<player> \\<attribute|all></gold> <gray>- Remove manual and timed modifiers",
            "<gold>/attributioner reload</gold> <gray>- Reload the config and clear all Attributioner modifiers",
            "<gold>/attributioner debug</gold> <gray>- Toggle debug logging");

    private final Attributioner plugin;
    private final AttributeManager manager;

    public AttributionerCommand(Attributioner plugin, AttributeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("attributioner.admin")) {
            sender.sendMessage("\u00a7cYou do not have permission to use this command.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("regions")) {
            if (plugin.getRegionModifiers().isEmpty()) {
                sender.sendMessage("\u00a7eNo region attributes configured.");
                return true;
            }

            // List all regions with their attribute modifiers
            for (Map.Entry<String, Map<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier>> entry : plugin.getRegionModifiers().entrySet()) {
                sender.sendMessage("\u00a7aRegion \u00a7f" + entry.getKey());
                for (org.bukkit.attribute.Attribute attr : entry.getValue().keySet()) {
                    sender.sendMessage("  - " + attr);
                }
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            // Toggle debug logging
            plugin.setebugMode(!plugin.isDebugMode());
            sender.sendMessage(plugin.isDebugMode() ? "\u00a7aDebug logging enabled." : "\u00a7eDebug logging disabled.");
        } else if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                sender.sendMessage("\u00a7cUsage: /attributioner info <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("\u00a7cPlayer not found.");
                return true;
            }

            sender.sendMessage("\u00a7aCustom modifiers for \u00a7f" + target.getName() + ":");
            boolean found = false;
            for (Attribute attr : AttributeManager.attributeRegistry()) {
                AttributeInstance inst = target.getAttribute(attr);
                if (inst == null) continue;
                for (AttributeModifier mod : inst.getModifiers()) {
                    if (mod.getKey() != null && mod.getKey().getNamespace().startsWith("attributioner-")) {
                        sender.sendMessage("  " + attr + " -> " + mod.getAmount() + " " + mod.getOperation() + " (" + mod.getKey() + ")");
                        found = true;
                    }
                }
            }
            if (!found) {
                sender.sendMessage("  None");
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("timed")) {
            handleTimed(sender, args);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("apply")) {
            handleApply(sender, args);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("remove")) {
            handleRemove(sender, args);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Reload the plugin configuration
            plugin.reloadConfig();
            plugin.loadConfig();
            sender.sendMessage("\u00a7aAttributioner config reloaded.");

            // Clear all attribute modifiers for online players
            manager.clearAllTimed();
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.clearAttrModifiers(player);
            }
        } else {
            HELP.forEach(sender::sendRichMessage);
        }
        return true;
    }

    private void handleTimed(CommandSender sender, String[] args) {
        if (args.length != 6) {
            sender.sendRichMessage(TIMED_USAGE);
            return;
        }

        Player target = parsePlayer(sender, args[1]);
        if (target == null) return;
        Attribute attribute = parseAttribute(sender, args[2]);
        if (attribute == null) return;
        Double amount = parseAmount(sender, args[3]);
        if (amount == null) return;
        AttributeModifier.Operation operation = parseOperation(sender, args[4]);
        if (operation == null) return;

        int seconds;
        try {
            seconds = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            seconds = 0;
        }
        if (seconds <= 0) {
            sender.sendRichMessage("<red>Seconds must be a whole number greater than 0: <value>",
                    Placeholder.unparsed("value", args[5]));
            return;
        }

        if (!manager.applyTimedModifier(target, attribute, amount, operation, seconds)) {
            sendMissingAttribute(sender, target, attribute);
            return;
        }

        sender.sendRichMessage("<green>Applied <attribute> <amount> <operation> to <aqua><name></aqua> for <seconds>s.",
                Placeholder.unparsed("attribute", attribute.getKey().asString()),
                Placeholder.unparsed("amount", Double.toString(amount)),
                Placeholder.unparsed("operation", operation.name()),
                Placeholder.unparsed("name", target.getName()),
                Placeholder.unparsed("seconds", Integer.toString(seconds)));
    }

    private void handleApply(CommandSender sender, String[] args) {
        if (args.length != 5) {
            sender.sendRichMessage(APPLY_USAGE);
            return;
        }

        Player target = parsePlayer(sender, args[1]);
        if (target == null) return;
        Attribute attribute = parseAttribute(sender, args[2]);
        if (attribute == null) return;
        Double amount = parseAmount(sender, args[3]);
        if (amount == null) return;
        AttributeModifier.Operation operation = parseOperation(sender, args[4]);
        if (operation == null) return;

        if (!manager.applyManualModifier(target, attribute, amount, operation)) {
            sendMissingAttribute(sender, target, attribute);
            return;
        }

        sender.sendRichMessage("<green>Applied <attribute> <amount> <operation> to <aqua><name></aqua> until removed.",
                Placeholder.unparsed("attribute", attribute.getKey().asString()),
                Placeholder.unparsed("amount", Double.toString(amount)),
                Placeholder.unparsed("operation", operation.name()),
                Placeholder.unparsed("name", target.getName()));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendRichMessage(REMOVE_USAGE);
            return;
        }

        Player target = parsePlayer(sender, args[1]);
        if (target == null) return;

        int removed;
        String what;
        if (args[2].equalsIgnoreCase("all")) {
            removed = manager.removeCommandModifiers(target);
            what = "manual or timed modifiers";
        } else {
            Attribute attribute = parseAttribute(sender, args[2]);
            if (attribute == null) return;
            removed = manager.removeCommandModifiers(target, attribute);
            what = "manual or timed " + attribute.getKey().asString() + " modifiers";
        }

        if (removed == 0) {
            sender.sendRichMessage("<yellow><aqua><name></aqua> has no <what>.",
                    Placeholder.unparsed("name", target.getName()),
                    Placeholder.unparsed("what", what));
        } else {
            sender.sendRichMessage("<green>Removed <count> modifier(s) from <aqua><name></aqua>.",
                    Placeholder.unparsed("count", Integer.toString(removed)),
                    Placeholder.unparsed("name", target.getName()));
        }
    }

    private static Player parsePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendRichMessage("<red>Player <aqua><name></aqua> is not online.",
                    Placeholder.unparsed("name", name));
        }
        return target;
    }

    private static Attribute parseAttribute(CommandSender sender, String name) {
        Attribute attribute = AttributeManager.lookupAttribute(name);
        if (attribute == null) {
            sender.sendRichMessage("<red>Unknown attribute: <value>",
                    Placeholder.unparsed("value", name));
        }
        return attribute;
    }

    private static Double parseAmount(CommandSender sender, String value) {
        double amount;
        try {
            amount = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            amount = Double.NaN;
        }
        if (!Double.isFinite(amount)) {
            sender.sendRichMessage("<red>Amount must be a number: <value>",
                    Placeholder.unparsed("value", value));
            return null;
        }
        return amount;
    }

    private static AttributeModifier.Operation parseOperation(CommandSender sender, String value) {
        try {
            return AttributeModifier.Operation.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendRichMessage("<red>Unknown operation: <value>. Valid operations: <valid>",
                    Placeholder.unparsed("value", value),
                    Placeholder.unparsed("valid", String.join(", ", operationNames())));
            return null;
        }
    }

    private static void sendMissingAttribute(CommandSender sender, Player target, Attribute attribute) {
        sender.sendRichMessage("<red><aqua><name></aqua> does not have the attribute <attribute>.",
                Placeholder.unparsed("name", target.getName()),
                Placeholder.unparsed("attribute", attribute.getKey().asString()));
    }

    private static String shortName(NamespacedKey key) {
        return key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.asString();
    }

    private static List<String> operationNames() {
        return Arrays.stream(AttributeModifier.Operation.values()).map(Enum::name).toList();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("attributioner.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("info", "timed", "apply", "remove").contains(sub)) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[1], names, new ArrayList<>());
        }
        if (sub.equals("timed") || sub.equals("apply")) {
            if (args.length == 3) {
                List<String> keys = AttributeManager.attributeRegistry().keyStream()
                        .map(AttributionerCommand::shortName)
                        .sorted()
                        .toList();
                return StringUtil.copyPartialMatches(args[2], keys, new ArrayList<>());
            }
            if (args.length == 5) {
                return StringUtil.copyPartialMatches(args[4], operationNames(), new ArrayList<>());
            }
        }
        if (sub.equals("remove") && args.length == 3) {
            // Only suggest attributes the player actually has manual or timed modifiers on
            List<String> options = new ArrayList<>();
            options.add("all");
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                for (Attribute attribute : AttributeManager.attributeRegistry()) {
                    AttributeInstance instance = target.getAttribute(attribute);
                    if (instance != null && (instance.getModifier(AttributeManager.manualKey(attribute)) != null
                            || instance.getModifier(AttributeManager.timedKey(attribute)) != null)) {
                        options.add(shortName(attribute.getKey()));
                    }
                }
            }
            return StringUtil.copyPartialMatches(args[2], options, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}

