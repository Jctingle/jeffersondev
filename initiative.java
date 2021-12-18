package jeffersondev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import jeffersondev.App;

public class initiative implements CommandExecutor,Listener {   
    private App app;
    public initiative(App app){
        this.app = app;
    }
    Integer orderIndex = 0;
    ArrayList<Player> activeUsers = new ArrayList<>();
    ArrayList<String> unitList = new ArrayList<>();
    ArrayList<Integer> allRolls = new ArrayList<>();
    Map<String, Player> unitOwners = new Hashtable<>();
    Map<String, Integer> playerRoll = new Hashtable<>();
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    final Scoreboard board = manager.getNewScoreboard();
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(sender instanceof Player){
            Player p = ((Player) sender).getPlayer();
            if (args[0].equals("start") && !activeUsers.contains(p)) {
                final Objective objecteye = board.registerNewObjective("initiative", "dummy", "initiative", RenderType.INTEGER);
                objecteye.setDisplaySlot(DisplaySlot.SIDEBAR);
                p.sendMessage("Starting New Combat Round");
                Team activeUnit = board.registerNewTeam("active");
                activeUnit.setColor(ChatColor.GREEN);
                Team deadUnit = board.registerNewTeam("dead");
                deadUnit.setColor(ChatColor.BLACK);
                
            } else if (args[0].equals("end")) {
                    final Objective objecteye = board.getObjective("initiative");
                    objecteye.unregister();
                    ItemStack item = new ItemStack(Material.EMERALD, 1);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("End Turn");
                    item.setItemMeta(meta);
                    for(Player online : Bukkit.getOnlinePlayers()){
                        online.setScoreboard(manager.getNewScoreboard());
                        online.getInventory().removeItem(item);
                        }
                    p.sendMessage("Combat over, clearing display");
                    unitOwners.clear();
                    playerRoll.clear();
                    allRolls.clear();
                    unitList.clear();
                    Team activeUnit = board.getTeam("active");
                    activeUnit.unregister();
                    Team deadUnit = board.getTeam("dead");
                    deadUnit.unregister();
                    orderIndex = 0;

            }
            else if (args[0].equals("join")) {
                //need to change board visual state tied into if they are an active player in this scoreboard
                int i = 1;
                if (args[1].equals("late")){
                    i++;
                }
                String playerName = args[i].toString();
                Integer iniInt = Integer.valueOf(args[i+1]);
                final Objective objecteye = board.getObjective("initiative");
                Score score = objecteye.getScore(playerName);
                score.setScore(iniInt);
                unitOwners.put(playerName, p);
                playerRoll.put(playerName, iniInt);
                allRolls.add(iniInt);
                unitList.add(playerName);
                if (i==2){
                    Team activeUnit = board.getTeam("active");
                    Set<String> pturns = activeUnit.getEntries();
                    Collections.sort(allRolls);
                    Collections.reverse(allRolls);
                    if (pturns.size() >= 1) {
                        String name = pturns.iterator().next();
                        //okay so now that I have the unit name I can reference the two hashmaps, grab the value of 
                        // Allrolls index of name.value
                        Integer current = allRolls.indexOf(playerRoll.get(name));
                        Integer newcomer = allRolls.indexOf(playerRoll.get(playerName));
                        if (current == 0){
                            p.sendMessage("Welcome to the game");
                            if ( newcomer == 0){
                                activeUnit.addEntry(playerName);
                            }
                        }
                        if (current > newcomer){
                            orderIndex++;
                        }
                        else{
                            orderIndex--;
                        }
                    }
                }
                for(Player online : Bukkit.getOnlinePlayers()){
                    online.setScoreboard(board);
                    }
                }
            else if (args[0].equals("begin")) {
                //sort the initiative dicerolls
                Collections.sort(allRolls);
                Collections.reverse(allRolls);
                //load team and load current turn integer
                Team activeUnit = board.getTeam("active");
                Integer currentTurn = allRolls.get(orderIndex);

                //End Turn Item code
                ItemStack item = new ItemStack(Material.EMERALD, 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("End Turn");
                item.setItemMeta(meta);

                //this for each goes through every entry in the scoreboard system and finds the matching integer, multiple matches allowed
                for (Map.Entry<String, Integer> entry : playerRoll.entrySet()) {
                        String keynote = entry.getKey();
                        Integer valnote = entry.getValue();
                        if (valnote == currentTurn){
                            activeUnit.addEntry(keynote);
                            Player pturn = unitOwners.get(keynote);
                            pturn.sendTitle("Your Turn", "Please make your move.", 1, 20, 1);
                            pturn.getInventory().addItem(item);
                        }
                }

                }
            else if (args[0].equals("kill")){
                //redundant item initialize code
                ItemStack item = new ItemStack(Material.EMERALD, 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("End Turn");
                item.setItemMeta(meta);

                String playerName = args[1].toString();
                Team deadUnit = board.getTeam("dead");
                Team activeUnit = board.getTeam("active");
                Player unitOwner = unitOwners.get(playerName);
                //check if they are on the current turn.
                if (activeUnit.hasEntry(playerName)){
                    activeUnit.removeEntry(playerName);
                    deadUnit.addEntry(playerName);
                    unitOwner.getInventory().remove(item);
                    //run next turn cycle

                    orderIndex++;
                    Integer resetTurn = allRolls.size();
                    if (orderIndex.equals(resetTurn)){
                        orderIndex = 0;
                    }
                    Integer currentTurn = allRolls.get(orderIndex);
                    //this is the setting active state
                    //important to note though this is going to get fucky when you tie in a player owning multiple unites
                    for (Map.Entry<String, Integer> entry : playerRoll.entrySet()) {
                        String keynote = entry.getKey();
                        Integer valNote = entry.getValue();
                        if (valNote == currentTurn){
                            Player pturn = unitOwners.get(keynote);
                            if(deadUnit.hasEntry(keynote)){
                                currentTurn = allRolls.get(orderIndex);
                                skipTurn(unitOwner);
                            }
                            else if(pturn.equals(unitOwner) && !deadUnit.hasEntry(keynote)){
                                unitOwner.getInventory().addItem(item);
                                activeUnit.addEntry(keynote);
                            }
                            else{
                                pturn.getInventory().addItem(item);
                                activeUnit.addEntry(keynote);
                                pturn.sendTitle("Your Turn", "Please make your move.", 1, 20, 1);
                            }
                        }
                        }
                }
                else{
                    deadUnit.addEntry(playerName);
                }
            }
                return true;
        }
        else{
            System.out.println("Cannot execute this command on the command line");
            return false;
            }
    }
    public void skipTurn(Player player){
        ItemStack item = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("End Turn");
        item.setItemMeta(meta);
        String lastUnit = "";
        Team activeUnit = board.getTeam("active");
        orderIndex++;
        Integer resetTurn = allRolls.size();
        if (orderIndex.equals(resetTurn)){
            orderIndex = 0;
        }
        Integer currentTurn = allRolls.get(orderIndex);
        for (Map.Entry<String, Integer> entry : playerRoll.entrySet()) {
            String keynote = entry.getKey();
            Integer valNote = entry.getValue();
            if (valNote != currentTurn){
                activeUnit.removeEntry(keynote);
                lastUnit = new String(keynote);
            }
        }
        //this is the setting active state
        for (Map.Entry<String, Integer> entry : playerRoll.entrySet()) {
            String keynote = entry.getKey();
            Integer valNote = entry.getValue();
            if (valNote == currentTurn){
                Team deadUnit = board.getTeam("dead");
                if (!deadUnit.hasEntry(keynote)){
                    activeUnit.addEntry(keynote);
                }
                Player pturn = unitOwners.get(keynote);
                //check for dead, though I may need to do this before the for loop even begins
                if(deadUnit.hasEntry(keynote)){
                    skipTurn(player);
                    //addcomment test
                }
                else{
                    pturn.getInventory().addItem(item);
                    pturn.sendTitle("Your Turn", "Please make your move.", 1, 20, 1);
                }
            }
        }    
    }
    @EventHandler
    public void onRightClick(PlayerInteractEvent event){
        //fatal crash happening here
        Server server = Bukkit.getServer();
        Player player = event.getPlayer();  
        ItemStack item = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("End Turn");
        item.setItemMeta(meta);
        String lastTurnUnit = "";
            if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
                if(player.getInventory().getItemInMainHand().getItemMeta().equals(meta)){  
                    event.setCancelled(true);
                    Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + ChatColor.DARK_RED + " Has finished their Turn");
                    player.getInventory().remove(item);

                    Team activeUnit = board.getTeam("active");
                    orderIndex++;
                    Integer resetTurn = allRolls.size();
                    if (orderIndex.equals(resetTurn)){
                        orderIndex = 0;
                    }
                    Integer currentTurn = allRolls.get(orderIndex);
                    for (Map.Entry<String, Integer> entry : playerRoll.entrySet()) {
                        String keynote = entry.getKey();
                        Integer valNote = entry.getValue();
                        if (valNote != currentTurn){
                            activeUnit.removeEntry(keynote);
                        }
                    }
                    //this is the setting active state
                    for (Map.Entry<String, Integer> entry : playerRoll.entrySet()) { 
                        server.getLogger().warning("Inside the turn set loop");
                        String keynote = entry.getKey();
                        Integer valNote = entry.getValue();
                        if (valNote == currentTurn){
                            Team deadUnit = board.getTeam("dead");
                            if (!deadUnit.hasEntry(keynote)){
                                activeUnit.addEntry(keynote);
                            }
                            Player pturn = unitOwners.get(keynote);
                            //check for dead, though I may need to do this before the for loop even begins
                            server.getLogger().warning("right before the if logic");
                            if(deadUnit.hasEntry(keynote)){
                                //skip turn if unit is dead
                                skipTurn(player);
                            }
                            else if(pturn.getInventory().contains(item)){
                                pturn.sendMessage("Someone in your bracket finished their turn");
                            }
                            // else if(pturn.equals(player)){
                            //     //this is supposed to be for if a player owns more than one unit and they share an initiative
                            //     pturn.sendMessage("Turn Ended");
                            // }
                            else{
                                pturn.getInventory().addItem(item);
                                pturn.sendTitle("Your Turn", "Please make your move.", 1, 20, 1);
                            }
                        }
                        }
                    }
                }
            }
}
