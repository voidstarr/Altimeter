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
        HashMap<UUID, Instant> accounts = ipAccountMap.computeIfAbsent(ip, k -> new HashMap<>());
        if (accounts.containsKey(player)) {
            return true;
        } else if (accounts.size() < AltimeterConfig.getAccountLimit(ip)) {
            Altimeter.getLogger().info("\tAllowed login and logged account for IP");
            accounts.put(player, Instant.now().plus(AltimeterConfig.getAccountTTL()));
            save();
            return true;
        } else {
            Altimeter.getLogger().info("\tDenied login. too many accounts from this ip");
            return false;
        }

    }

    public static void checkAndClearAccounts(Instant now) {
        Iterator<Map.Entry<String, HashMap<UUID, Instant>>> ipIterator = ipAccountMap.entrySet().iterator();
        boolean dataChanged = false;
        while (ipIterator.hasNext()) {
            Map.Entry<String, HashMap<UUID, Instant>> ipEntry = ipIterator.next();
            HashMap<UUID, Instant> accounts = ipEntry.getValue();
            accounts.forEach((key, value) -> {
                if (now.isAfter(value)) {
                    accounts.remove(key);
                }
            });
            dataChanged = true;
            if (accounts.isEmpty())
                ipIterator.remove();
        }
        boolean trimmed = trimAllAccountLists();
        if (dataChanged || trimmed)
            save();
    }

    public static boolean trimAllAccountLists() {
        boolean trimmed = false;
        Iterator<Map.Entry<String, HashMap<UUID, Instant>>> ipIterator = ipAccountMap.entrySet().iterator();
        while (ipIterator.hasNext()) {
            Map.Entry<String, HashMap<UUID, Instant>> ipEntry = ipIterator.next();
            if (trimAccountList(ipEntry.getKey())) {
                trimmed = true;
            }
            if (ipEntry.getValue().isEmpty())
                ipIterator.remove();
        }
        return trimmed;
    }

    public static boolean trimAccountList(String ip) {
        HashMap<UUID, Instant> accounts = ipAccountMap.get(ip);
        int elementsToRemove = accounts.size() - AltimeterConfig.getAccountLimit(ip);
        if (elementsToRemove <= 0) {
            return false;
        }
        accounts.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> ((Map.Entry<UUID, Instant>) e).getValue().toEpochMilli()).reversed())
                .limit(elementsToRemove)
                .forEach(e -> accounts.remove(e.getKey()));
        return true;
    }

    public static boolean clear(String target) {
        boolean success;
        if (target.equals("all")) {
            ipAccountMap.clear();
            success = true;
        } else {
            success = ipAccountMap.remove(target) != null;
            if (success) {
                trimAccountList(target);
            }
        }
        save();
        return success;
    }
}
