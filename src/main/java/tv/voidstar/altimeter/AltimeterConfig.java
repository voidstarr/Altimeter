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
import java.util.HashMap;
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
            checkIntervalUnit = TimeUnit.valueOf(checkInterval.getNode("unit").getString().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            checkIntervalValue = 5;
            checkIntervalUnit = TimeUnit.MINUTES;
            Altimeter.getLogger().error("Invalid checkInterval unit in config: {}. Setting checkInterval to default 5 MINUTES", checkIU, e);
            Altimeter.getLogger().error("Read https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/TimeUnit.html#enum_constant_detail for allowed values.");
        }

        CommentedConfigurationNode accountTTLNode = configs.getNode("altimeter", "ttl")
                .setComment("How long after an account is added to the queue is it removed.");
        getValOrSetDefault(accountTTLNode, "P30D");
        accountTTL = Duration.parse(accountTTLNode.getString());

        ConfigurationNode accountLimitNode = configs.getNode("altimeter", "accountLimit")
                .setComment("How many accounts from one IP can log in.");
        getValOrSetDefault(accountLimitNode, 5);
        accountLimit = accountLimitNode.getInt();

        ConfigurationNode limitOverridesNode = configs.getNode("altimeter", "limitOverrides")
                .setComment("Override account limit for specific IPs");
        if (!limitOverridesNode.isList()) {
            ConfigurationNode override = limitOverridesNode.appendListNode();
            override.getNode("ip").setValue("ex.am.pl.e");
            override.getNode("accountLimit").setValue(5);
        } else {
            for (ConfigurationNode overrideNode : limitOverridesNode.getChildrenList()) {
                String ip = overrideNode.getNode("ip").getString("in.va.li.d");
                if (InetAddresses.isInetAddress(ip)) {
                    accountLimitOverrides.put(ip, overrideNode.getNode("accountLimit").getInt(5));
                } else {
                    Altimeter.getLogger().error("Invalid IP in limitOverrides configuration {}", ip);
                }
            }
        }
        save();
    }

    public static void save() {
        try {
            loader.save(configs);
        } catch (IOException e) {
            Altimeter.getLogger().error("Unable to save config file.", e);
        }
    }

    public static int getAccountLimit() {
        return accountLimit;
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
}
