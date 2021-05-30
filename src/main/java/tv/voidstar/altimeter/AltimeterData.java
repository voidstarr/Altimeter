package tv.voidstar.altimeter;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.entity.living.player.Player;

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
        if(!ipAccountListsFile.exists())
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
        for(String ip : ipAccountMap.keySet()) {
            ConfigurationNode ipNode = list.appendListNode();
            ipNode.getNode("ip").setValue(ip);
            HashMap<UUID, Instant> accountsForIP = ipAccountMap.get(ip);
            for(UUID uuid : accountsForIP.keySet()) {
                ipNode.getNode("uuid").setValue(uuid.toString());
                ipNode.getNode("ttl").setValue(accountsForIP.get(uuid).toEpochMilli());
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

    public static boolean canLogIn(Player player) {
        String playerIP = String.valueOf(player.getConnection().getAddress().getAddress());
        UUID playerUUID = player.getUniqueId();
        if(ipAccountMap.containsKey(playerIP)) {
            HashMap<UUID, Instant> accounts = ipAccountMap.get(playerIP);
            if (!accounts.containsKey(playerUUID)) {
                if(accounts.size() == AltimeterConfig.getAccountLimit()) {
                    return false;
                } else {
                    accounts.put(playerUUID, Instant.now().plus(AltimeterConfig.getAccountTTL()));
                    save();
                    return true;
                }
            }
        } else {
            HashMap<UUID, Instant> newIPMap = new HashMap<>();
            newIPMap.put(playerUUID, Instant.now().plus(AltimeterConfig.getAccountTTL()));
            ipAccountMap.put(playerIP, newIPMap);
            save();
            return true;
        }
        return false;
    }

    public static void checkAndClearAccounts(Instant now) {
        Iterator<Map.Entry<String, HashMap<UUID, Instant>>> ipIterator = ipAccountMap.entrySet().iterator();
        while(ipIterator.hasNext()) {
            Map.Entry<String, HashMap<UUID, Instant>> ipEntry = ipIterator.next();
            ipEntry.getValue().entrySet().removeIf(accountEntry -> accountEntry.getValue().isAfter(now));
            if(ipEntry.getValue().isEmpty())
                ipIterator.remove();
        }
    }
}
