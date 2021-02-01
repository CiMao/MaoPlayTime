package cn.cimao.www.MaoPlayTime;

import cn.cimao.www.MaoPlayTime.tools.Tool;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainClass extends JavaPlugin implements Listener {
    public static YamlConfiguration PlayerData = new YamlConfiguration();
    public File playerdata = new File("plugins" + File.separator + getName() + File.separator + "PlayerData.yml");
    public static YamlConfiguration Message = new YamlConfiguration();
    public File message = new File("plugins" + File.separator + getName() + File.separator + "Message.yml");
    public static Economy econ = null;
    public static Permission perms = null;
    public static Chat chat = null;

    @Override
    public void onEnable() {

        File configFile = new File(getDataFolder(),"config.yml");
        if(!configFile.exists()){
            saveDefaultConfig();
        }

        if (!playerdata.exists()) {
            try {
                playerdata.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            PlayerData.load(playerdata);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if(!message.exists()){
            try {
                message.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Message.load(message);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
            Message.set("NeedVault","§f§l[§4§l服务器§f§l]§d你的金币不足");
            Message.set("NeedPlayerPoints","§f§l[§4§l服务器§f§l]§d你的点券不足");
            Message.set("IsNever","§f§l[§4§l服务器§f§l]§d您是尊贵的永久玩家无需购买时长");
            Message.set("NeedRemove","§f§l[§4§l服务器§f§l]§d请先取消永久权限再进行增减");
            Message.set("AddSuccess","§f§l[§4§l服务器§f§l]§d为玩家playername增加游戏时长成功");
            Message.set("DelSuccess","§f§l[§4§l服务器§f§l]§d为玩家playername减少游戏时长成功");
            Message.set("NeverSuccess","§f§l[§4§l服务器§f§l]§d为玩家playername设置游戏时长为永久成功");
            Message.set("DelNeverSuccess","§f§l[§4§l服务器§f§l]§d为玩家playername取消游戏时长为永久成功");
            Message.set("Me","§f§l[§4§l服务器§f§l]§d您的游戏时长剩余time分钟");
            try {
                Message.save(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Message.load(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            setupEconomy();
            setupPermissions();
            setupChat();
            getLogger().info("插件依赖Vault加载成功");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            hookPlayerPoints();
            getLogger().info("插件依赖PlayerPoints加载成功");
        }

        getServer().getPluginManager().registerEvents(this,this);
        //每分钟减少在线玩家的时长
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            //要(延迟/循环)执行的内容
            public void run() {
                ArrayList<Player>players = new ArrayList<Player>();
                players.addAll(Bukkit.getServer().getOnlinePlayers());
                if (players.size()>0){
                    for (int i=0;i<players.size();i++) {
                        Player player = players.get(i);
                        if (PlayerData.getInt(player.getName())>0) {
                            PlayerData.set(player.getName(),PlayerData.getInt(player.getName())-1);
                            try {
                                PlayerData.save(playerdata);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        runnable.runTaskTimerAsynchronously(this,0,1200);
    }

    private void setupEconomy()
    {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        econ = (Economy)rsp.getProvider();
    }

    private void setupChat()
    {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = (Chat)rsp.getProvider();
    }

    private void setupPermissions()
    {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = (Permission)rsp.getProvider();
    }

    private PlayerPoints playerPoints;
    private boolean hookPlayerPoints() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("PlayerPoints");
        playerPoints = PlayerPoints.class.cast(plugin);
        return playerPoints != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //playtime never [playername]
        //设置玩家为永久玩家
        if (args.length==2&&args[0].equalsIgnoreCase("never")&&sender.isOp()) {
            PlayerData.set(args[1],-1);
            try {
                PlayerData.save(playerdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sender.sendMessage(Tool.rlore(Message.getString("NeverSuccess").replaceAll("playername",args[1])));
            return true;
        }
        //playtime delnever [playername]
        //取消永久玩家
        if (args.length==2&&args[0].equalsIgnoreCase("delnever")&&sender.isOp()) {
            PlayerData.set(args[1],0);
            try {
                PlayerData.save(playerdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sender.sendMessage(Tool.rlore(Message.getString("DelNeverSuccess").replaceAll("playername",args[1])));
            return true;
        }
        //playtime open
        //打开购买界面
        if (args.length==1&&args[0].equalsIgnoreCase("open")) {
            Player player = (Player)sender;
            if (PlayerData.getInt(player.getName())==-1) {
                sender.sendMessage(Message.getString("IsNever"));
                return true;
            }
            Inventory inv = Bukkit.createInventory(null, 27, "BuyTimeGui");
            player.openInventory(inv);
            ItemStack gui = new ItemStack(getConfig().getInt("Gui.ID"));
            ItemMeta meta = gui.getItemMeta();
            meta.setDisplayName(getConfig().getString("Gui.DisplayName"));
            meta.setLore(getConfig().getStringList("Gui.Lore"));
            gui.setItemMeta(meta);
            //先全部设置挡板
            for (int i=0;i<27;i++) {
                inv.setItem(i,gui);
            }
            //得到购买的设置列表
            List<String> keys = new ArrayList<>();
            keys.addAll(getConfig().getConfigurationSection("Buy").getKeys(false));
            if (keys.size()>0) {
                //放置物品进gui
                for (int i=0;i<keys.size();i++) {
                    ItemStack item = new ItemStack(getConfig().getInt("Buy."+keys.get(i)+".ID"));
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.setDisplayName(getConfig().getString("Buy."+keys.get(i)+".DisplayName"));
                    itemMeta.setLore(getConfig().getStringList("Buy."+keys.get(i)+".Lore"));
                    item.setItemMeta(itemMeta);
                    inv.setItem(getConfig().getInt("Buy."+keys.get(i)+".Index"),item);
                }
            }
            return true;
        }
        //playtime add [playername] [time]
        //为玩家增加时间
        if (args.length==3&&args[0].equalsIgnoreCase("add")&&sender.isOp()) {
            if (PlayerData.getInt(args[1])==-1) {
                sender.sendMessage(Tool.rlore(Message.getString("NeedRemove")));
            }
            PlayerData.set(args[1],PlayerData.getInt(args[1])+Integer.parseInt(args[2]));
            try {
                PlayerData.save(playerdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sender.sendMessage(Tool.rlore(Message.getString("AddSuccess").replaceAll("playername",args[1])));
            return true;
        }
        //playtime del [playername] [time]
        //为玩家减少时间
        if (args.length==3&&args[0].equalsIgnoreCase("del")&&sender.isOp()) {
            if (PlayerData.getInt(args[1])==-1) {
                sender.sendMessage(Tool.rlore(Message.getString("NeedRemove")));
            }
            if (PlayerData.getInt(args[1])-Integer.parseInt(args[2])>0) {
                PlayerData.set(args[1], PlayerData.getInt(args[1]) - Integer.parseInt(args[2]));
            }else {
                PlayerData.set(args[1], 0);
            }
            try {
                PlayerData.save(playerdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sender.sendMessage(Tool.rlore(Message.getString("DelSuccess").replaceAll("playername",args[1])));
            return true;
        }
        //playtime me
        //查询我的时长
        if (args.length==1&&args[0].equalsIgnoreCase("me")) {
            sender.sendMessage(Tool.rlore(Message.getString("Me").replaceAll("time",PlayerData.getInt(sender.getName())+"")));
            return true;
        }
        //reload
        if (args.length==1&&args[0].equalsIgnoreCase("reload")&&sender.isOp()) {
            reloadConfig();
            try {
                PlayerData.load(playerdata);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
            try {
                Message.load(message);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
            sender.sendMessage("重载成功");
            return true;
        }
        //showmessage
        if (label.equalsIgnoreCase("playtime")) {
            sender.sendMessage("playtime me");
            sender.sendMessage("playtime open");
            if (sender.isOp()) {
                sender.sendMessage("playtime never [playername]");
                sender.sendMessage("playtime delnever [playername]");
                sender.sendMessage("playtime add [playername] [time]");
                sender.sendMessage("playtime del [playername] [time]");
                sender.sendMessage("playtime reload");
            }
            return true;
        }
        return false;
    }
    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent e){
        if (getConfig().get(e.getPlayer().getName())==null) {
            //当玩家第一次登陆给予基础时长
            PlayerData.set(e.getPlayer().getName(),getConfig().getInt("DefaultTime"));
            try {
                PlayerData.save(playerdata);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    @EventHandler
    public void PlayerMoveEvent(PlayerMoveEvent e){
        //若时长为0打开购买界面
        if (PlayerData.getInt(e.getPlayer().getName())==0) {
            e.setCancelled(true);
            Inventory inv = Bukkit.createInventory(null, 27, "BuyTimeGui");
            e.getPlayer().openInventory(inv);
            ItemStack gui = new ItemStack(getConfig().getInt("Gui.ID"));
            ItemMeta meta = gui.getItemMeta();
            meta.setDisplayName(getConfig().getString("Gui.DisplayName"));
            meta.setLore(getConfig().getStringList("Gui.Lore"));
            gui.setItemMeta(meta);
            //先全部设置挡板
            for (int i=0;i<27;i++) {
                inv.setItem(i,gui);
            }
            //得到购买的设置列表
            List<String> keys = new ArrayList<>();
            keys.addAll(getConfig().getConfigurationSection("Buy").getKeys(false));
            if (keys.size()>0) {
                //放置物品进gui
                for (int i=0;i<keys.size();i++) {
                    ItemStack item = new ItemStack(getConfig().getInt("Buy."+keys.get(i)+".ID"));
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.setDisplayName(getConfig().getString("Buy."+keys.get(i)+".DisplayName"));
                    itemMeta.setLore(getConfig().getStringList("Buy."+keys.get(i)+".Lore"));
                    item.setItemMeta(itemMeta);
                    inv.setItem(getConfig().getInt("Buy."+keys.get(i)+".Index"),item);
                }
            }
        }
    }
    @EventHandler
    public void onClickInventory (InventoryClickEvent e){
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot<0) {
            return;
        }
        ItemStack item = e.getInventory().getItem(slot);
        //如果是挡板则取消
        if (item.getItemMeta().getDisplayName().equals(getConfig().getString("Gui.DisplayName"))) {
            e.setCancelled(true);
            return;
        }
        //是商品则继续
        List<String> keys = new ArrayList<>();
        keys.addAll(getConfig().getConfigurationSection("Buy").getKeys(false));
        e.setCancelled(true);
        if (keys.size()>0) {
            for (int i=0;i<keys.size();i++) {
                if (item.getItemMeta().getDisplayName().equals(getConfig().getString("Buy."+keys.get(i)+".DisplayName"))) {
                    //判断到是这个商品
                    if (getConfig().getString("Buy."+keys.get(i)+".Type").equals("Vault")) {
                        //服务器未加载Vault
                        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                            return;
                        }else {
                            double hasmoney = econ.getBalance(player.getName());
                            if (hasmoney >= getConfig().getInt("Buy."+keys.get(i)+".Price")) {
                                EconomyResponse takemoney = econ.withdrawPlayer(player, getConfig().getInt("Buy."+keys.get(i)+".Price"));
                                PlayerData.set(player.getName(),PlayerData.getInt(player.getName())+getConfig().getInt("Buy."+keys.get(i)+".Time"));
                            } else {
                                player.sendMessage(Message.getString("NeedVault"));
                            }
                        }
                    }
                    if (getConfig().getString("Buy."+keys.get(i)+".Type").equals("PlayerPoints")) {
                        //服务器未加载PlayerPoints
                        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
                            return;
                        }else {
                            if (playerPoints.getAPI().take(player.getName(), getConfig().getInt("Buy."+keys.get(i)+".Price"))) {
                                PlayerData.set(player.getName(),PlayerData.getInt(player.getName())+getConfig().getInt("Buy."+keys.get(i)+".Time"));
                            } else {
                                player.sendMessage(Message.getString("NeedPlayerPoints"));
                                return;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
}
