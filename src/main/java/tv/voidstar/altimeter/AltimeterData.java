package tv.voidstar.altimeter;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class AltimeterData {
    private static final Map<String, HashMap<UUID, Instant>> ipAccountMap = Collections.synchronizedMap(new HashMap<>());

    private static File ipAccountListsFile;
    private static HoconConfigurationLoader loader;

    private static ConfigurationNode ipAccountsMap;

    public static void init(File rootDir) throws IOException {
        ipAccountListsFile = new File(rootDir, "accounts.conf");
        if (!ipAccountListsFile.exists())
            ipAccountListsFile.createNewFile();

        loader = HoconConfigurationLoader.builder().setFile(ipAccountListsFile).build();
        ipAccountsMap = loader.load();
    }

    public static void load() {
        ipAccountMap.clear();
        for (ConfigurationNode ipNode : ipAccountsMap.getNode("ips").getChildrenList()) {
            String ip = ipNode.getNode("ip").getString();
            HashMap<UUID, Instant> accountsForIP = new HashMap<>();
            for (ConfigurationNode accountEntry : ipNode.getNode("accounts").getChildrenList()) {
                UUID uuid = UUID.fromString(accountEntry.getNode("uuid").getString());
                Instant ttl = Instant.ofEpochMilli(accountEntry.getNode("ttl").getLong());
                accountsForIP.put(uuid, ttl);
            }
            ipAccountMap.put(ip, accountsForIP);
        }
    }

    public static void save() {
        ipAccountsMap.removeChild("ips");
        ConfigurationNode list = ipAccountsMap.getNode("ips");
        for (String ip : ipAccountMap.keySet()) {
            ConfigurationNode ipNode = list.appendListNode();
            ipNode.getNode("ip").setValue(ip);
            ConfigurationNode accountList = ipNode.getNode("accounts");
            HashMap<UUID, Instant> accountsForIP = ipAccountMap.get(ip);
            for (UUID uuid : accountsForIP.keySet()) {
                ConfigurationNode accountForIP = accountList.appendListNode();
                accountForIP.getNode("uuid").setValue(uuid.toString());
                accountForIP.getNode("ttl").setValue(accountsForIP.get(uuid).toEpochMilli());
            }
        }
        try {
            loader.save(ipAccountsMap);
        } catch (IOException e) {
            Altimeter.getLogger().error("Could not save Account/IP lists to disk", e);
        }
    }

    public static void reload() {
        ipAccountMap.clear();
        load();
    }

    public static boolean canLogIn(UUID player, String ip) {
        if (ipAccountMap.containsKey(ip)) {
            HashMap<UUID, Instant> accounts = ipAccountMap.get(ip);
            Altimeter.getLogger().info("IP has {} accounts logged.", accounts.size());
            if (!accounts.containsKey(player)) {
                if (accounts.size() == AltimeterConfig.getAccountLimit()) {
                    Altimeter.getLogger().info("\tDenied login. too many accounts from this ip");
                    return false;
                } else {
                    Altimeter.getLogger().info("\tAllowed login and logged account for IP");
                    accounts.put(player, Instant.now().plus(AltimeterConfig.getAccountTTL()));
                    save();
                    return true;
                }
            }
        } else {
            Altimeter.getLogger().info("\tnew IP encountered");
            Altimeter.getLogger().info("\tAllowed login and logged account for IP");
            HashMap<UUID, Instant> newIPMap = new HashMap<>();
            newIPMap.put(player, Instant.now().plus(AltimeterConfig.getAccountTTL()));
            ipAccountMap.put(ip, newIPMap);
            save();
            return true;
        }
        return false;
    }

    public static void checkAndClearAccounts(Instant now) {
        Iterator<Map.Entry<String, HashMap<UUID, Instant>>> ipIterator = ipAccountMap.entrySet().iterator();
        while (ipIterator.hasNext()) {
            Map.Entry<String, HashMap<UUID, Instant>> ipEntry = ipIterator.next();
            ipEntry.getValue().entrySet().removeIf(accountEntry -> accountEntry.getValue().isAfter(now));
            if (ipEntry.getValue().isEmpty())
                ipIterator.remove();
        }
    }
}
