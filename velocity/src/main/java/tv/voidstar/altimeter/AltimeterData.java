package tv.voidstar.altimeter;

import com.j256.ormlite.table.TableUtils;
import io.github.eufranio.storage.Persistable;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import tv.voidstar.altimeter.data.AccountData;
import tv.voidstar.altimeter.data.IPData;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class AltimeterData {
    private static final Map<String, HashMap<UUID, Instant>> ipAccountMap = Collections.synchronizedMap(new HashMap<>());

    private static File ipAccountListsFile;
    private static HoconConfigurationLoader loader;

    private static ConfigurationNode ipAccountsMap;

    static Persistable<IPData, String> ips;
    static Persistable<AccountData, Integer> accounts;

    public static void init(File rootDir) throws IOException {
        if (AltimeterConfig.isDatabaseEnabled()) {
            Altimeter.getLogger().info("Initializing database storage");
            ips = Persistable.create(IPData.class, AltimeterConfig.getDatabaseUrl());
            ips.idFieldName = "ip";

            accounts = Persistable.create(AccountData.class, AltimeterConfig.getDatabaseUrl());
        } else {
            Altimeter.getLogger().info("Initializing flat file storage");
            ipAccountListsFile = new File(rootDir, "accounts.conf");
            if (!ipAccountListsFile.exists())
                ipAccountListsFile.createNewFile();

            loader = HoconConfigurationLoader.builder().setFile(ipAccountListsFile).build();
            ipAccountsMap = loader.load();
        }
    }

    public static void load() {
        if (AltimeterConfig.isDatabaseEnabled())
            return;

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
        if (AltimeterConfig.isDatabaseEnabled())
            return;

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
        if (AltimeterConfig.isDatabaseEnabled())
            return;

        ipAccountMap.clear();
        load();
    }

    public static boolean canLogIn(UUID player, String ip) {
        if (AltimeterConfig.isDatabaseEnabled()) {
            IPData ipData = ips.getOrCreate(ip);
            try { ipData.refresh(); } catch (Exception e) { e.printStackTrace(); }

            if (ipData.accounts.stream().anyMatch(acc -> acc.uuid.equals(player)))
                return true;

            if (ipData.accounts.size() < AltimeterConfig.getAccountLimit(ip)) {
                Altimeter.getLogger().info("\tAllowed login and logged account for IP");
                AccountData data = new AccountData() {{
                    ip = ipData;
                    uuid = player;
                    ttl = Instant.now().plus(AltimeterConfig.getAccountTTL());
                }};
                accounts.save(data);
                return true;
            }

            Altimeter.getLogger().info("\tDenied login. too many accounts from this ip");
            return false;
        } else {
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
    }

    public static void checkAndClearAccounts(Instant now) {
        if (AltimeterConfig.isDatabaseEnabled()) {
            try {
                for (AccountData data : accounts.objDao) {
                    if (now.isAfter(data.ttl))
                        accounts.delete(data);
                }
            } catch (Exception e) {
                Altimeter.getLogger().error("Error clearing IPs");
                e.printStackTrace();
            }
        } else {
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
    }

    static boolean trimAllAccountLists() {
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
        if (AltimeterConfig.isDatabaseEnabled()) {
            IPData ipData = ips.get(ip);
            if (ipData == null)
                return false;

            try { ipData.refresh(); } catch (Exception e) { e.printStackTrace(); }

            if (ipData.accounts.isEmpty() || AltimeterConfig.getAccountLimit(ip) >= ipData.accounts.size())
                return false;

            int elementsToRemove = ipData.accounts.size() - AltimeterConfig.getAccountLimit(ip);
            Comparator<AccountData> comparator = Comparator.comparingLong(acc -> acc.ttl.toEpochMilli());
            ipData.accounts.stream()
                    .sorted(comparator.reversed())
                    .limit(elementsToRemove)
                    .forEach(acc -> accounts.delete(acc));
        } else {
            HashMap<UUID, Instant> accounts = ipAccountMap.get(ip);
            if (accounts == null || AltimeterConfig.getAccountLimit(ip) > accounts.size())
                return false;

            int elementsToRemove = accounts.size() - AltimeterConfig.getAccountLimit(ip);
            accounts.entrySet().stream()
                    .sorted(Comparator.comparingLong(e -> ((Map.Entry<UUID, Instant>) e).getValue().toEpochMilli()).reversed())
                    .limit(elementsToRemove)
                    .forEach(e -> accounts.remove(e.getKey()));
        }
        return true;
    }

    public static boolean clear(String target) {
        if (AltimeterConfig.isDatabaseEnabled()) {
            if (target.equals("all")) {
                try {
                    TableUtils.clearTable(ips.objDao.getConnectionSource(), IPData.class);
                    return true;
                } catch (Exception e) {
                    Altimeter.getLogger().error("Error clearing the database:");
                    e.printStackTrace();
                    return false;
                }
            } else {
                IPData ipData = ips.get(target);
                if (ipData == null)
                    return false;

                try { ipData.refresh(); } catch (Exception e) { e.printStackTrace(); }
                ipData.accounts.clear();
                ips.delete(ipData);
                return true;
            }
        } else {
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
}
