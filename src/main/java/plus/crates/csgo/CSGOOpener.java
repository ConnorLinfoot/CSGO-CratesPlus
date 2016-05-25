package plus.crates.csgo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import plus.crates.CratesPlus;
import plus.crates.Opener.Opener;
import plus.crates.Winning;

import java.io.IOException;
import java.util.Random;

public class CSGOOpener extends Opener {
	private CratesPlus cratesPlus;
	private Inventory winGUI;
	private BukkitTask task;
	private Integer timer = 0;
	private Integer currentItem = 0;

	public CSGOOpener(Plugin plugin, String name) {
		super(plugin, name);
		cratesPlus = CratesPlus.getOpenHandler().getCratesPlus();
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
	}

	@Override
	public void doReopen() {
		getPlayer().openInventory(winGUI);
	}

	@Override
	protected void doOpen() {
		Random random = new Random();
		int max = crate.getWinnings().size() - 1;
		int min = 0;
		currentItem = random.nextInt((max - min) + 1) + min;
		winGUI = Bukkit.createInventory(null, 45, crate.getColor() + crate.getName() + " Win");
		player.openInventory(winGUI);
		int maxTime = getOpenerConfig().getInt("Length");
		final int maxTimeTicks = maxTime * 10;
		task = Bukkit.getScheduler().runTaskTimerAsynchronously(cratesPlus, new Runnable() {
			public void run() {
				if (!player.isOnline()) { // TODO, Try and handle DC for players?
					task.cancel();
					return;
				}
				Integer i = 0;
				while (i < 45) {

					if (i >= 18 && i <= 26 && i != 22) {
						// TODO handle csgo "Row"
//						winGUI.setItem(i, getWinning().getPreviewItemStack());
						i++;
						continue;
					}

					if (i == 22) {
						i++;
						if (crate.getWinnings().size() == currentItem)
							currentItem = 0;
						final Winning winning;
						if (timer == maxTimeTicks) {
							winning = getWinning();
						} else {
							winning = crate.getWinnings().get(currentItem);
						}

						final ItemStack currentItemStack = winning.getPreviewItemStack();
						if (timer == maxTimeTicks) {
							winning.runWin(player);
						}
						winGUI.setItem(22, currentItemStack);

						currentItem++;
						continue;
					}
					ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) cratesPlus.getCrateHandler().randInt(0, 15));
					ItemMeta itemMeta = itemStack.getItemMeta();
					if (timer == maxTimeTicks) {
						itemMeta.setDisplayName(ChatColor.RESET + "Winner!");
					} else {
						Sound sound;
						try {
							sound = Sound.valueOf("NOTE_PIANO");
						} catch (Exception e) {
							try {
								sound = Sound.valueOf("BLOCK_NOTE_HARP");
							} catch (Exception ee) {
								return; // This should never happen!
							}
						}
						final Sound finalSound = sound;
						Bukkit.getScheduler().runTask(cratesPlus, new Runnable() {
							@Override
							public void run() {
								if (player.getOpenInventory().getTitle() != null && player.getOpenInventory().getTitle().contains(" Win"))
									player.playSound(player.getLocation(), finalSound, (float) 0.2, 2);
							}
						});
						itemMeta.setDisplayName(ChatColor.RESET + "Rolling...");
					}
					itemStack.setItemMeta(itemMeta);
					winGUI.setItem(i, itemStack);
					i++;
				}
				if (timer == maxTimeTicks) {
					cratesPlus.getCrateHandler().removeOpening(player.getUniqueId());
					task.cancel();
					return;
				}
				timer++;
			}
		}, 0L, 2L);
	}


}
