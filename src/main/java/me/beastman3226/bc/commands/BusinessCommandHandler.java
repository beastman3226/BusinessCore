package me.beastman3226.bc.commands;

import me.beastman3226.bc.BusinessCore.Information;
import me.beastman3226.bc.business.Business;
import me.beastman3226.bc.business.BusinessManager;
import me.beastman3226.bc.errors.InsufficientFundsException;
import me.beastman3226.bc.event.business.BusinessBalanceChangeEvent;
import me.beastman3226.bc.event.business.BusinessFiredEmployeeEvent;
import me.beastman3226.bc.event.business.BusinessPostCreatedEvent;
import me.beastman3226.bc.event.business.BusinessPreCreatedEvent;
import me.beastman3226.bc.player.EmployeeManager;
import me.beastman3226.bc.player.Manager;
import me.beastman3226.bc.util.Prefixes;
import me.beastman3226.bc.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This class handles all business related commands
 * @author beastman3226
 */
public class BusinessCommandHandler implements CommandExecutor {

   private static BusinessCommandHandler instance = null;
   protected BusinessCommandHandler() {}
   public static BusinessCommandHandler getInstance() {
      if(instance == null) {
         instance = new BusinessCommandHandler();
      }
      return instance;
   }
   
    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String string, String[] args) {
        if(Information.eco == null) {
            Information.BusinessCore.setupEconomy();
        }
        if(sender.hasPermission(cmnd.getPermission())) {
            // <editor-fold defaultstate="collapsed" desc="Business Create">
            if(cmnd.getName().equalsIgnoreCase("b.create") && args.length > 0) {
                if(sender instanceof Player && !BusinessManager.isOwner(sender.getName())) {
                    BusinessPreCreatedEvent event = new BusinessPreCreatedEvent(sender, args);
                    Bukkit.getPluginManager().callEvent(event);
                    Business b = BusinessManager.createBusiness(new Business.Builder(BusinessManager.openID()).name(event.getName()).owner(event.getSender().getName()));
                    BusinessPostCreatedEvent event1 = new BusinessPostCreatedEvent(b);
                    Bukkit.getPluginManager().callEvent(event1);
                    if(!event1.isCancelled()) {
                        Bukkit.getServer().broadcastMessage(Prefixes.POSITIVE + event1.getBusiness().getOwnerName() + " has just created " + event1.getBusiness().getName());
                    }
                } else {
                    sender.sendMessage(Prefixes.ERROR + "I need a player name to create a business. You aren't a player. OR You already have a business");
                }
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Business Delete">
            } else if (cmnd.getName().equalsIgnoreCase("b.delete")) {
                if(sender instanceof Player && BusinessManager.isOwner(sender.getName())) {
                    BusinessManager.deleteBusiness(BusinessManager.getBusiness(sender.getName()));
                } else if (!(sender instanceof Player) && args.length > 0) {
                    int k = 0;
                    boolean caught = false;
                    try {
                        k = Integer.valueOf(args[0]);
                    } catch (NumberFormatException e) {
                        caught = true;
                    }
                    if(!caught) {
                        if(BusinessManager.isID(k)) {
                            BusinessManager.deleteBusiness(BusinessManager.getBusiness(k));
                        } else {
                            sender.sendMessage(Prefixes.ERROR + "That ID is not valid!");
                            return false;
                        }
                    } else if(BusinessManager.isOwner(args[0])){
                        BusinessManager.deleteBusiness(BusinessManager.getBusiness(args[0]));
                    } else {
                       sender.sendMessage(Prefixes.ERROR + "That owner is not valid!");
                       return false;
                    }
                } else {
                    sender.sendMessage(Prefixes.ERROR + "Valid business not found.");
                    return false;
                }
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Business withdraw">
            } else if(cmnd.getName().equalsIgnoreCase("b.withdraw") && args.length > 0) {
                if(sender instanceof Player && BusinessManager.isOwner(sender.getName())) {
                    boolean caught = false;
                    double amount = 0.0;
                    Business b = BusinessManager.getBusiness(sender.getName());
                    try {
                        amount = Double.parseDouble(args[0]);
                    } catch (NumberFormatException nfe) {
                        caught = true;
                    }
                    if(caught) {
                        sender.sendMessage(Prefixes.ERROR + "Your second argument must be an amount such as 0.0");
                        return false;
                    } else {
                        try {
                            BusinessBalanceChangeEvent event = new BusinessBalanceChangeEvent(b,-amount);
                            Bukkit.getServer().getPluginManager().callEvent(event);
                            if(!event.isCancelled()) {
                                event.getBusiness().withdraw(event.getAbsoluteAmount());
                                Business.businessList.remove(event.getBusiness());
                                Business.businessList.add(event.getBusiness());
                            }
                        } catch (InsufficientFundsException ex) {
                            sender.sendMessage(Prefixes.ERROR + "The amount must be less than the current balance.");
                            return false;
                        }
                        Information.eco.depositPlayer(sender.getName(), amount);
                        sender.sendMessage(Prefixes.NOMINAL + "Current balance in " + b.getName() + " is " + b.getBalance() + " " + Information.eco.currencyNamePlural());
                        return true;
                    }
                } else if(!(sender instanceof Player)) {
                    boolean caught = false;
                    int id = 0;
                    double amount = 0.0;
                    try {
                        id = Integer.parseInt(args[0]);
                        amount = Double.parseDouble(args[1]);
                    } catch (NumberFormatException nfe) {
                        caught = true;
                    }
                    if(caught) {
                        sender.sendMessage("Please do 'b.withdraw [business_id] [amount]', both must be numbers!");
                        return false;
                    } else {
                        try {
                            Business b = BusinessManager.getBusiness(id);
                            BusinessBalanceChangeEvent event = new BusinessBalanceChangeEvent(b,-amount);
                            Bukkit.getServer().getPluginManager().callEvent(event);
                            if(!event.isCancelled()) {
                                event.getBusiness().withdraw(event.getAbsoluteAmount());
                                Business.businessList.remove(event.getBusiness());
                                Business.businessList.add(event.getBusiness());
                            }
                        } catch (InsufficientFundsException ex) {
                            sender.sendMessage("The amount must be less than the balance!");
                            return false;
                        }
                    }
                } else {
                    sender.sendMessage(Prefixes.ERROR + "You are not an owner.");
                }
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Business Deposit">
            } else if(cmnd.getName().equalsIgnoreCase("b.deposit") && args.length > 0) {
                if(sender instanceof Player && (BusinessManager.isOwner(sender.getName()) || Manager.isManager(sender.getName()))){
                    boolean caught = false;
                    double amount = 0.0;
                    try {
                        amount = Double.valueOf(args[0]);
                    } catch (NumberFormatException nfe) {
                        caught = true;
                    }
                    if(caught) {
                        sender.sendMessage(Prefixes.ERROR + args[0] + " is not the proper format for a deposit");
                        return false;
                    } else {
                        if(BusinessManager.isOwner(sender.getName())) {
                        Business b = BusinessManager.getBusiness(sender.getName());
                        BusinessBalanceChangeEvent event = new BusinessBalanceChangeEvent(b,amount);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                        if(!event.isCancelled()) {
                             event.getBusiness().deposit(event.getAmount());
                             Information.
                                     eco.
                                     withdrawPlayer(sender.getName(),
                                     event
                                     .getAmount());
                             Business.businessList.remove(event.getBusiness());
                             Business.businessList.add(event.getBusiness());
                        }
                        sender.sendMessage(Prefixes.NOMINAL + "Current balance in " + b.getName() + " is " + b.getBalance() + " " +  Information.eco.currencyNamePlural());
                        return true;
                        } else {
                            Business b = Manager.getBusiness(sender.getName());
                            BusinessBalanceChangeEvent event = new BusinessBalanceChangeEvent(b,amount);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                        if(!event.isCancelled()) {
                             event.getBusiness().deposit(event.getAmount());
                             Information.
                                     eco.
                                     withdrawPlayer(sender.getName(),
                                     event
                                     .getAmount());
                             Business.businessList.remove(event.getBusiness());
                             Business.businessList.add(event.getBusiness());
                        }
                        sender.sendMessage(Prefixes.NOMINAL + "Current balance in " + b.getName() + " is " + b.getBalance() + " " +  Information.eco.currencyNamePlural());
                        return true;
                        }
                    }
                } else {
                   int id = 0;
                   double amount = 0.0;
                   boolean caught = false;
                   try {
                       id = Integer.parseInt(args[0]);
                       if(args[1] == null) {
                           sender.sendMessage(Prefixes.ERROR + "You need to specify an amount!");
                           return false;
                       } else {
                           amount = Double.parseDouble(args[1]);
                       }
                   } catch (NumberFormatException nfe) {
                       caught = true;
                   }
                   if(caught) {
                       sender.sendMessage("Please specify valid numbers as numbers ie. 1, 0.0");
                       return false;
                   } else {
                       Business b = BusinessManager.getBusiness(id);
                        BusinessBalanceChangeEvent event = new BusinessBalanceChangeEvent(b,amount);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                        if(!event.isCancelled()) {
                             event.getBusiness().deposit(event.getAmount());
                             Business.businessList.remove(event.getBusiness());
                             Business.businessList.add(event.getBusiness());
                        }
                       Player owner = Bukkit.getPlayerExact(b.getOwnerName());
                       if(owner.isOnline() | owner != null) {
                           owner.sendMessage(Prefixes.POSITIVE + "Server has just deposited " + amount + " into your business, your new balance is " + b.getBalance() + " " + Information.eco.currencyNamePlural());
                       }
                       return true;
                   }
                }
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Business balance">
            } else if(cmnd.getName().equalsIgnoreCase("b.balance")) {
                if(sender instanceof Player && (BusinessManager.isOwner(sender.getName()) || Manager.isManager(sender.getName()))) {
                    sender.sendMessage(Prefixes.NOMINAL + "Current balance in " + BusinessManager.getBusiness(sender.getName()).getName() + " is " + BusinessManager.getBusiness(sender.getName()).getBalance());
                    return true;
                } else if(!(sender instanceof Player) && args.length > 0) {
                    int id = 0;
                    String name = "";
                    boolean caught = false;
                    try {
                    id = Integer.valueOf(args[0]);
                    } catch (NumberFormatException nfe) {
                        caught = true;
                    }
                    if(caught) {
                        name = args[0];
                        if(BusinessManager.isOwner(name)) {
                            Business b = BusinessManager.getBusiness(name);
                            sender.sendMessage(Prefixes.NOMINAL + "Current balance in " + b.getName() + " is " + b.getBalance());
                            return true;
                        } else if(Manager.isManager(sender.getName())) {
                            Business b = Manager.getBusiness(sender.getName());
                            sender.sendMessage(Prefixes.NOMINAL + "Current balance in " + b.getName() + " is " + b.getBalance());
                            return true;
                        } else {
                            sender.sendMessage(Prefixes.ERROR + name + " is not an owner. Try again with valid name.");
                            return false;
                        }
                    } else {
                        if(BusinessManager.isID(id)) {
                            Business b = BusinessManager.getBusiness(id);
                            sender.sendMessage(Prefixes.NOMINAL + "Current balance in " + b.getName() + " is " + b.getBalance());
                            return true;
                        } else {
                            sender.sendMessage(Prefixes.ERROR + "That is not a valid id!");
                            return false;
                        }
                    }
                }
            // </editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Business info">
            } else if (cmnd.getName().equalsIgnoreCase("b.info")) {
                if(sender instanceof Player) {
                    if(BusinessManager.isOwner(sender.getName())) {
                        Business b = BusinessManager.getBusiness(sender.getName());
                        if(b == null) {
                            System.out.println("For some odd reason the business returned null");
                        }
                        sender.sendMessage(ChatColor.DARK_GREEN + "|==========Business Info==========|");
                        sender.sendMessage(ChatColor.GREEN + "  Name: " + b.getName());
                        sender.sendMessage(ChatColor.GREEN + "  ID: " + b.getID());
                        sender.sendMessage(ChatColor.GREEN + "  Balance: " + b.getBalance());
                        if(b.getEmployeeIDs().length == 0) {
                            sender.sendMessage(ChatColor.GREEN + "  Employees: N/A");
                        } else {
                            sender.sendMessage(ChatColor.GREEN + "  Employees: " + this.asString(b.getEmployeeIDs()));
                        }
                    } else if(Manager.isManager(sender.getName())) {
                        Business b = Manager.getBusiness(sender.getName());
                        if(b == null) {
                            System.out.println("For some odd reason the business returned null");
                        }
                        sender.sendMessage(ChatColor.DARK_GREEN + "|==========Business Info==========|");
                        sender.sendMessage(ChatColor.GREEN + "  Name: " + b.getName());
                        sender.sendMessage(ChatColor.GREEN + "  ID: " + b.getID());
                        sender.sendMessage(ChatColor.GREEN + "  Balance: " + b.getBalance());
                        if(b.getEmployeeIDs().length == 0) {
                            sender.sendMessage(ChatColor.GREEN + "  Employees: N/A");
                        } else {
                            sender.sendMessage(ChatColor.GREEN + "  Employees: " + this.asString(b.getEmployeeIDs()));
                        }
                    } else {
                        String[] info = new String[]{ChatColor.DARK_GREEN + "|=========Top Businesses==========|",
                                                          ChatColor.GREEN + "  1) " + ChatColor.WHITE + BusinessManager.getIndex(1),
                                                          ChatColor.GREEN + "  2) " + ChatColor.WHITE + BusinessManager.getIndex(2),
                                                          ChatColor.GREEN + "  3) " + ChatColor.WHITE + BusinessManager.getIndex(3),
                                                          ChatColor.GREEN + "  4) " + ChatColor.WHITE + BusinessManager.getIndex(4),
                                                          ChatColor.GREEN + "  5) " + ChatColor.WHITE + BusinessManager.getIndex(5)};
                        sender.sendMessage(info[0]);
                        sender.sendMessage(info[1]);
                        sender.sendMessage(info[2]);
                        sender.sendMessage(info[3]);
                        sender.sendMessage(info[4]);
                        sender.sendMessage(info[5]);
                    }
                } else if(!(sender instanceof Player) && args.length > 0) {
                    int id = 0;
                    try {
                        id = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("That is an invalid id!");
                        return false;
                    }
                    Business b= BusinessManager.getBusiness(id);
                    if(b == null) {
                        Information.BusinessCore.getLogger().warning("That is not a valid id!");
                        return false;
                    }
                    String[] info = new String[]{ChatColor.DARK_GREEN + "|==========Business Info==========|",
                                                          ChatColor.GREEN + "  Name: " + b.getName(),
                                                          ChatColor.GREEN + "  ID: " + b.getID(),
                                                          ChatColor.GREEN + "  Balance: " + b.getBalance(),
                                                          ChatColor.GREEN + "  Employees: " + b.getEmployeeIDs() == null || b.getEmployeeIDs().length == 0 ? "N/A" : this.asString(b.getEmployeeIDs())};
                    sender.sendMessage(info);
                } else if(!(sender instanceof Player)) {
                    String[] info = new String[]{ChatColor.DARK_GREEN + "|=========Top Businesses==========|",
                                                          ChatColor.GREEN + "  1) " + ChatColor.WHITE + BusinessManager.getIndex(1),
                                                          ChatColor.GREEN + "  2) " + ChatColor.WHITE + BusinessManager.getIndex(2),
                                                          ChatColor.GREEN + "  3) " + ChatColor.WHITE + BusinessManager.getIndex(3),
                                                          ChatColor.GREEN + "  4) " + ChatColor.WHITE + BusinessManager.getIndex(4),
                                                          ChatColor.GREEN + "  5) " + ChatColor.WHITE + BusinessManager.getIndex(5)};
                        sender.sendMessage(info[0]);
                        sender.sendMessage(info[1]);
                        sender.sendMessage(info[2]);
                        sender.sendMessage(info[3]);
                        sender.sendMessage(info[4]);
                        sender.sendMessage(info[5]);
                }
            //</editor-fold>
            //<editor-fold desc="Business top" defaultstate="collapsed">
            } else if (cmnd.getName().equalsIgnoreCase("b.top") && args.length >= 0) {
                if(args.length == 0) {
                String[] info = new String[]{ChatColor.DARK_GREEN + "|=========Top Businesses==========|",
                                                          ChatColor.GREEN + "  1) " + ChatColor.WHITE + BusinessManager.getIndex(1),
                                                          ChatColor.GREEN + "  2) " + ChatColor.WHITE + BusinessManager.getIndex(2),
                                                          ChatColor.GREEN + "  3) " + ChatColor.WHITE + BusinessManager.getIndex(3),
                                                          ChatColor.GREEN + "  4) " + ChatColor.WHITE + BusinessManager.getIndex(4),
                                                          ChatColor.GREEN + "  5) " + ChatColor.WHITE + BusinessManager.getIndex(5)};
                        sender.sendMessage(info[0]);
                        sender.sendMessage(info[1]);
                        sender.sendMessage(info[2]);
                        sender.sendMessage(info[3]);
                        sender.sendMessage(info[4]);
                        sender.sendMessage(info[5]);
                } else {
                    int index = Integer.parseInt(args[0]);
                    if(index == 2) {
                        String[] info = new String[]{ChatColor.DARK_GREEN + "|=========Top Businesses==========|",
                                                          ChatColor.GREEN + "  6) " + ChatColor.WHITE + BusinessManager.getIndex(6),
                                                          ChatColor.GREEN + "  7) " + ChatColor.WHITE + BusinessManager.getIndex(7),
                                                          ChatColor.GREEN + "  8) " + ChatColor.WHITE + BusinessManager.getIndex(8),
                                                          ChatColor.GREEN + "  9) " + ChatColor.WHITE + BusinessManager.getIndex(9),
                                                          ChatColor.GREEN + "  10) " + ChatColor.WHITE + BusinessManager.getIndex(10)};
                        sender.sendMessage(info[0]);
                        sender.sendMessage(info[1]);
                        sender.sendMessage(info[2]);
                        sender.sendMessage(info[3]);
                        sender.sendMessage(info[4]);
                        sender.sendMessage(info[5]);
                    } else {
                        String[] info = new String[]{ChatColor.DARK_GREEN + "|=========Top Businesses==========|",
                                                          ChatColor.GREEN + "  " + (((index*5) - 5) + 1) + ") "  + ChatColor.WHITE + BusinessManager.getIndex(((index*5) - 5) + 1),
                                                          ChatColor.GREEN + "  " + (((index*5) - 5) + 2) + ") " + ChatColor.WHITE + BusinessManager.getIndex(((index*5) - 5) + 2),
                                                          ChatColor.GREEN + "  " + (((index*5) - 5) + 3) + ") " + ChatColor.WHITE + BusinessManager.getIndex(((index*5) - 5) + 3),
                                                          ChatColor.GREEN + "  " + (((index*5) - 5) + 4) + ") " + ChatColor.WHITE + BusinessManager.getIndex(((index*5) - 5) + 4),
                                                          ChatColor.GREEN + "  " + (((index*5) - 5) + 5) + ") " + ChatColor.WHITE + BusinessManager.getIndex(((index*5) - 5) + 5)};
                        sender.sendMessage(info[0]);
                        sender.sendMessage(info[1]);
                        sender.sendMessage(info[2]);
                        sender.sendMessage(info[3]);
                        sender.sendMessage(info[4]);
                        sender.sendMessage(info[5]);
                    }
                }
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Hire">
            } else if(cmnd.getName().equalsIgnoreCase("b.hire") && args.length > 0) {
                if(sender instanceof Player && (BusinessManager.isOwner(sender.getName()) || EmployeeManager.isEmployee(sender.getName()))) {
                    String name = args[0];
                    Player player = Bukkit.getPlayer(name);
                    if(player != null & player.isOnline()) {
                        if(EmployeeManager.isEmployee(sender.getName())) {
                            player.sendMessage(Prefixes.POSITIVE + "You have been invited to join " + EmployeeManager.getEmployee(sender.getName()).getBusiness().getName() + " by " + sender.getName());
                            sender.sendMessage(Prefixes.NOMINAL + "Invite to the business has been sent.");
                            EmployeeManager.pending.put(player.getName(), EmployeeManager.getEmployee(sender.getName()).getBusiness().getID());
                            Scheduler.runAcceptance();
                        } else if(BusinessManager.isOwner(sender.getName())) {
                            player.sendMessage(Prefixes.POSITIVE + "You have been invited to join " + BusinessManager.getBusiness(sender.getName()).getName() + " by " + sender.getName());
                            sender.sendMessage(Prefixes.NOMINAL + "Invite to the business has been sent.");
                            EmployeeManager.pending.put(player.getName(), BusinessManager.getBusiness(sender.getName()).getID());
                            Scheduler.runAcceptance();
                        }
                    }
                }
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Fire">
            } else if(cmnd.getName().equalsIgnoreCase("b.fire") && args.length > 0) {
                if(sender instanceof Player && (BusinessManager.isOwner(sender.getName()) || Manager.isManager(sender.getName()))) {
                    boolean caught = false;
                    int id = 0;
                    String name = args[0];
                    try {
                        id = Integer.parseInt(name);
                    } catch (NumberFormatException e) {
                        caught = true;
                    }
                    if(BusinessManager.isOwner(sender.getName())) {
                    Business b = BusinessManager.getBusiness(sender.getName());
                    BusinessFiredEmployeeEvent event = new BusinessFiredEmployeeEvent(b, null);
                    if(!caught) {
                        event.setEmployee(id);
                        Bukkit.getPluginManager().callEvent(event);
                        if(!event.isCancelled()) {
                            b.removeEmployee(event.getEmployee().getID());
                            Business.businessList.remove(event.getBusiness());
                                Business.businessList.add(event.getBusiness());
                        }
                    } else {
                        String ename = Bukkit.getPlayer(name).getName();
                        event.setEmployee(EmployeeManager.getEmployee(ename));
                        Bukkit.getPluginManager().callEvent(event);
                        if(!event.isCancelled()) {
                            b.removeEmployee(event.getEmployee().getID());
                            Business.businessList.remove(event.getBusiness());
                            Business.businessList.add(event.getBusiness());
                        }
                    }
                    Bukkit.getPlayer(name).sendMessage(Prefixes.NOMINAL + "You have been fired from " + b.getName() + "!");
                } else if(Manager.isManager(sender.getName())) {
                    Business b = Manager.getBusiness(sender.getName());
                    BusinessFiredEmployeeEvent event = new BusinessFiredEmployeeEvent(b, null);
                    if(!caught) {
                        event.setEmployee(id);
                        Bukkit.getPluginManager().callEvent(event);
                        if(!event.isCancelled()) {
                            b.removeEmployee(event.getEmployee().getID());
                            Business.businessList.remove(event.getBusiness());
                                Business.businessList.add(event.getBusiness());
                        }
                    } else {
                        String ename = Bukkit.getPlayer(name).getName();
                        event.setEmployee(EmployeeManager.getEmployee(ename));
                        Bukkit.getPluginManager().callEvent(event);
                        if(!event.isCancelled()) {
                            b.removeEmployee(event.getEmployee().getID());
                            Business.businessList.remove(event.getBusiness());
                            Business.businessList.add(event.getBusiness());
                        }
                    }
                }
                } else if(!(sender instanceof Player)) {
                    sender.sendMessage(Prefixes.ERROR + "I am having issues finding the correct business and employee");
                }
            // </editor-fold>
            // <editor-fold desc="Business promote" defaultstate="collapsed">
            } else if(cmnd.getName().equalsIgnoreCase("b.promote") && args.length > 0) {
                Player p = Bukkit.getPlayer(args[0]);
                if(p != null && BusinessManager.isOwner(sender.getName())) {
                    Manager.addManager(p.getName(), BusinessManager.getBusiness(sender.getName()).getName());
                    sender.sendMessage(Prefixes.POSITIVE + "You have just promoted " + p.getName() + " to manager.");
                } else if(Manager.isManager(sender.getName()) && p != null) {
                    Manager.addManager(p.getName(), Manager.getBusiness(sender.getName()).getName());
                    sender.sendMessage(Prefixes.POSITIVE + "You have just promoted " + p.getName() + " to manager.");
                }
            // </editor-fold>
            // <editor-fold desc="Business demote" defaultstate="collapsed">
            } else if(cmnd.getName().equalsIgnoreCase("b.demote") && args.length > 0) {
                Player p = Bukkit.getPlayer(args[0]);
                if(p!= null && BusinessManager.isOwner(sender.getName())) {
                    if(Manager.isManager(p.getName(), BusinessManager.getBusiness(sender.getName()).getName())) {
                        Manager.removeManager(p.getName(), string);
                        sender.sendMessage(Prefixes.ERROR + "You have just fired " + p.getName() + "!");
                    }
                }
            // </editor-fold>
            // <editor-fold desc="Toggle" defaultstate="collapsed">
            } else if(cmnd.getName().equalsIgnoreCase("b.toggle")) {
                if(BusinessManager.isOwner(sender.getName())) {
                    sender.sendMessage(Prefixes.NOMINAL + "Current status of salary pay: " + BusinessManager.getBusiness(sender.getName()).toggleSalary());
                }
            // </editor-fold>
            } else if(cmnd.getName().equalsIgnoreCase("b.salary")) {
                if(BusinessManager.isOwner(sender.getName()) && args.length>0) {
                    double newsal = Double.parseDouble(args[0]);
                    BusinessManager.getBusiness(sender.getName()).setSalary(newsal);
                    sender.sendMessage(Prefixes.NOMINAL + "Current salary: " + newsal);
                }
            }
        } else {
            sender.sendMessage(Prefixes.ERROR + ChatColor.translateAlternateColorCodes('&', cmnd.getPermissionMessage()));
            return false;
        }
        return true;
    }

    private String asString(Object[] a) {
        String string = "";
        int i = 0;
        for(Object j : a) {
            if(i == 0) {
                string = j + "";
                i++;
                continue;
            }
            string = string.concat(", " + j);
        }
        return string;
    }
}
