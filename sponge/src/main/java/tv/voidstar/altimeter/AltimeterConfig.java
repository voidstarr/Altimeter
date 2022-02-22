package tv.voidstar.altimeter;

import com.google.common.net.InetAddresses;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AltimeterConfig {

    private static final HashMap<String, Integer> accountLimitOverrides = new HashMap<>();
    private static File configFile;
    private static @NonNull HoconConfigurationLoader loader;
    private static CommentedConfigurationNode configs;
    private static Duration accountTTL;
    private static int accountLimit;
    private static long checkIntervalValue;
    private static TimeUnit checkIntervalUnit;

    public static void init(File rootDir) {
        configFile = new File(rootDir, "altimeter.conf");
        loader = HoconConfigurationLoader.builder().setFile(configFile).build();
        load();

    }

    public static void load() {
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
            configs = loader.load();
        } catch (IOException e) {
            Altimeter.getLogger().error("Unable to load config file.", e);
        }

        // defaults
        configs.getNode("altimeter").setComment("General Altimeter configurations.");

        CommentedConfigurationNode checkInterval = configs.getNode("altimeter", "checkInterval")
                .setComment("How often accounts should be checked and cleared from IP lists.");
        getValOrSetDefault(checkInterval.getNode("value"), 5);
        getValOrSetDefault(checkInterval.getNode("unit").setComment("DAYS, HOURS, MINUTES, or SECONDS"), "MINUTES");

        checkIntervalValue = checkInterval.getNode("value").getLong();

        String checkIU = checkInterval.getNode("unit").getString();
        try {
            checkIntervalUnit = TimeUnit.valueOf(checkIU.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            checkIntervalValue = 5;
            checkIntervalUnit = TimeUnit.MINUTES;
            Altimeter.getLogger().error("Invalid checkInterval unit in config: {}. Setting checkInterval to default 5 MINUTES", checkIU, e);
            Altimeter.getLogger().error("Read https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/TimeUnit.html#enum_constant_detail for allowed values.");
        }

        CommentedConfigurationNode accountTTLNode = configs.getNode("altimeter", "ttl")
                .setComment("How long after an account is added to the queue is it removed.");
        getValOrSetDefault(accountTTLNode.getNode("value"), 30);
        getValOrSetDefault(accountTTLNode.getNode("unit").setComment("DAYS, HOURS, MINUTES, or SECONDS"), "DAYS");

        long accountTTLValue = accountTTLNode.getNode("value").getLong();
        ChronoUnit accountTTLUnit;

        try {
            accountTTLUnit = ChronoUnit.valueOf(accountTTLNode.getNode("unit").getString().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            accountTTLValue = 30;
            accountTTLUnit = ChronoUnit.DAYS;
            Altimeter.getLogger().error("Invalid TTL unit in config: {}. Setting ttl to default 30 DAYS", checkIU, e);
            Altimeter.getLogger().error("Read https://docs.oracle.com/javase/8/docs/api/java/time/temporal/ChronoUnit.html#enum.constant.summary for allowed values.");
        }

        accountTTL = Duration.of(accountTTLValue, accountTTLUnit);

        ConfigurationNode accountLimitNode = configs.getNode("altimeter", "accountLimit")
                .setComment("How many accounts from one IP can log in.");
        getValOrSetDefault(accountLimitNode, 5);
        accountLimit = accountLimitNode.getInt();

        Altimeter.getLogger().info("load limit overrides data");
        ConfigurationNode limitOverridesNode = configs.getNode("altimeter", "limitOverrides")
                .setComment("Override account limit for specific IPs");
        if (!limitOverridesNode.isList()) {
            ConfigurationNode override = limitOverridesNode.appendListNode();
            override.getNode("ip").setValue("127.0.0.1");
            override.getNode("limit").setValue(50);
        } else {
            Altimeter.getLogger().info("load limit overrides data");
            for (ConfigurationNode overrideNode : limitOverridesNode.getChildrenList()) {
                String ip = overrideNode.getNode("ip").getString("in.va.li.d");
                int limit = overrideNode.getNode("limit").getInt(accountLimit);
                if (InetAddresses.isInetAddress(ip)) {
                    accountLimitOverrides.put(ip, limit);
                    Altimeter.getLogger().info("{} has {} account limit", ip, limit);
                } else {
                    Altimeter.getLogger().error("Invalid IP in limitOverrides configuration {}", ip);
                }
            }
        }

        save();
    }

    public static void save() {
        configs.getNode("altimeter").removeChild("limitOverrides");
        ConfigurationNode limitOverridesNode = configs.getNode("altimeter", "limitOverrides");
        for (Map.Entry<String, Integer> entry : accountLimitOverrides.entrySet()) {
            ConfigurationNode limitOverrideElement = limitOverridesNode.appendListNode();
            limitOverrideElement.getNode("ip").setValue(entry.getKey());
            limitOverrideElement.getNode("limit").setValue(entry.getValue());
        }
        try {
            loader.save(configs);
        } catch (IOException e) {
            Altimeter.getLogger().error("Unable to save config file.", e);
        }
    }

    public static int getAccountLimit(String ip) {
        int limit = accountLimitOverrides.getOrDefault(ip, accountLimit);
        Altimeter.getLogger().info("account limit for {} is {}", ip, limit);
        return limit;
    }

    public static void setAccountLimit(int accountLimit) {
        AltimeterConfig.accountLimit = accountLimit;
    }

    public static Duration getAccountTTL() {
        return accountTTL;
    }

    public static void setAccountTTL(String accountTTL) {
        try {
            AltimeterConfig.accountTTL = Duration.parse(accountTTL);
        } catch (DateTimeParseException e) {
            Altimeter.getLogger().error("Invalid Duration.parse() string: {}", accountTTL, e);
            Altimeter.getLogger().error("Consider reading https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-");
        }
    }

    private static void getValOrSetDefault(ConfigurationNode node, boolean def) {
        if (!(node.getValue() instanceof Boolean))
            node.setValue(def);
    }

    private static void getValOrSetDefault(ConfigurationNode node, String def) {
        if (node.getString() == null)
            node.setValue(def);
    }

    private static void getValOrSetDefault(ConfigurationNode node, Number def) {
        if (!(node.getValue() instanceof Number))
            node.setValue(def);
    }

    public static TimeUnit getCheckIntervalUnit() {
        return checkIntervalUnit;
    }

    public static void setCheckIntervalUnit(TimeUnit checkIntervalUnit) {
        AltimeterConfig.checkIntervalUnit = checkIntervalUnit;
    }

    public static long getCheckIntervalValue() {
        return checkIntervalValue;
    }

    public static void setCheckIntervalValue(long checkIntervalValue) {
        AltimeterConfig.checkIntervalValue = checkIntervalValue;
    }

    public static boolean setOverride(String ip, int limit) {
        boolean replace = accountLimitOverrides.putIfAbsent(ip, limit) != null;
        if (replace) {
            accountLimitOverrides.replace(ip, limit);
        }
        AltimeterData.trimAccountList(ip);
        save();
        return replace;
    }
}
