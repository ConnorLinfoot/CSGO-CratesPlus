package plus.crates.csgo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import plus.crates.Crates.Crate;
import plus.crates.Crates.KeyCrate;
import plus.crates.Crates.Winning;
import plus.crates.Opener.Opener;

import java.io.IOException;
import java.util.*;

public class CSGOOpener extends Opener {
    private HashMap<UUID, Inventory> guis = new HashMap<>();
    private int length = 10 * 10;
    private int slowSpeedTime;
    private int fastSpeedTime;
    private ItemStack redstoneTorchOff;
    private ItemStack redstoneTorchOn;

    public CSGOOpener(Plugin plugin, String name) {
        super(plugin, name);
    }

    @Override
    public void doSetup() {
        FileConfiguration config = getOpenerConfig();
        if (!config.isSet("Length")) {
            config.set("Length", 10);
            try {
                config.save(getOpenerConfigFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        length = config.getInt("Length") * 10; // Convert to "half ticks"
        slowSpeedTime = length / 20;
        fastSpeedTime = (length / 10) * 9;
        redstoneTorchOff = new ItemStack(Material.REDSTONE_TORCH_OFF);
        redstoneTorchOn = new ItemStack(Material.REDSTONE_TORCH_ON);
        ItemMeta torchMeta = redstoneTorchOff.getItemMeta();
        torchMeta.setDisplayName(ChatColor.RESET + " ");
        redstoneTorchOff.setItemMeta(torchMeta);
        redstoneTorchOn.setItemMeta(torchMeta);
    }

    @Override
    public void doReopen(Player player, Crate crate, Location blockLocation) {
        player.openInventory(guis.get(player.getUniqueId()));
    }

    private ArrayList<ItemStack> calculateItems(ArrayList<Winning> winnings, int current) {
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int winningsI = 0;

            switch (i) {
                case 0:
                    winningsI = current - 2;
                    break;
                case 1:
                    winningsI = current - 1;
                    break;
                case 3:
                    winningsI = current + 1;
                    break;
                case 4:
                    winningsI = current + 2;
                    break;
            }

            if (i == 2) {
                items.add(winnings.get(current).getPreviewItemStack());
            } else {
                if (winningsI < 0) {
                    // winningsI = -2
                    winningsI = winnings.size() - Math.abs(winningsI);
                } else if (winningsI > winnings.size() - 1) {
                    winningsI = winnings.size() + winningsI;
                }
                items.add(winnings.get(winningsI).getPreviewItemStack());
            }
        }
        return items;
    }

    @Override
    public void doOpen(final Player player, final Crate crate, Location blockLocation) {
        final ArrayList<Winning> winnings = crate.getWinningsExcludeAlways();
        if (winnings.size() < 5) {
            // TODO Handle when they have less than 5 winnings
            return;
        }
        Collections.shuffle(winnings);
        final Inventory gui = Bukkit.createInventory(null, 27, crate.getName(true));
        final Integer[] iTracker = {0, 0}; // 0 = current tick (10), 1 = current winning
        player.openInventory(gui);
        guis.put(player.getUniqueId(), gui);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    // Player is not online, cancel task and run key command to return the key
                    finish(player);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate key " + player.getName() + " " + crate.getName() + " 1");
                    this.cancel();
                    return;
                }

                if ((iTracker[0] > fastSpeedTime || iTracker[0] < slowSpeedTime) && (iTracker[0] & 1) == 0) {
                    iTracker[0]++;
                    return;
                }

                ArrayList<ItemStack> items = calculateItems(winnings, iTracker[1]);

                for (int i = 0; i < 27; i++) {
                    if (i == 4 || i == 22) {
                        gui.setItem(i, iTracker[0] == length ? redstoneTorchOn : redstoneTorchOff);
                    } else if (i >= 10 && i <= 16) {
                        gui.setItem(i, items.get(i - 10));
                    } else {
                        ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) new Random().nextInt(15));
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.RESET + " ");
                        itemStack.setItemMeta(itemMeta);
                        gui.setItem(i, itemStack);
                    }
                }

                if (iTracker[0] >= length) {
                    finish(player);
                    this.cancel();
                    return;
                }

                iTracker[1]++;
                if (iTracker[1] >= winnings.size() - 1) {
                    iTracker[1] = 0;
                }

                iTracker[0]++;
            }
        }.runTaskTimerAsynchronously(getPlugin(), 0L, 2L);
    }

    @Override
    public boolean doesSupport(Crate crate) {
        return crate instanceof KeyCrate;
    }

    @Override
    protected void finish(Player player) {
        super.finish(player);
        guis.remove(player.getUniqueId());
    }
}