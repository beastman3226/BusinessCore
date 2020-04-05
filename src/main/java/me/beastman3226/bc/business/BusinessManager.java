package me.beastman3226.bc.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import me.beastman3226.bc.BusinessCore;
import me.beastman3226.bc.data.file.FileData;
import me.beastman3226.bc.errors.NoOpenIDException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author beastman3226
 */
public class BusinessManager {

    public static ArrayList<Business> businessList = new ArrayList<Business>();

    /**
     * Creates all businesses from file.
     */
    public static void createBusinesses() {
        BusinessCore.getInstance().getBusinessFileManager().reload();
        FileConfiguration businessYml = BusinessCore.getInstance().getBusinessFileManager().getFileConfiguration();
        int id;
        String name, owner;
        boolean salary = false;
        double pay = 0, balance;
        for(String s : businessYml.getKeys(false)) {
            List<String> list = businessYml.getStringList(s + ".employeeIDs");
            id = businessYml.getInt(s + ".id");
            name = s;
            owner = Bukkit.getOfflinePlayer(UUID.fromString(businessYml.getString(s + ".ownerUUID"))).getName();
            if(owner == null) owner = Bukkit.getPlayer(UUID.fromString(businessYml.getString(s + ".ownerUUID"))).getName();
            balance = businessYml.getDouble(s + ".balance");
            if(!businessYml.contains(s + ".pay") || !businessYml.contains(s + ".salary")) {
                pay = businessYml.getDouble(s + ".pay");
                salary = businessYml.getBoolean(s + ".salary");
            }
            if(!list.isEmpty()) {
                Business b = createBusiness(new Business.Builder(id)
                        .name(name)
                        .owner(owner)
                        .balance(balance)
                        .ids(list.toArray(new String[]{}))
                        .salary(salary)
                        .pay(pay));
               BusinessCore.getInstance().getLogger().log(Level.INFO, "Loaded business " + b.getName() + " from file");
            } else {
                createBusiness(new Business.Builder(id)
                        .name(s)
                        .owner(owner)
                        .balance(balance));
                /*if(Information.debug) {
                    Information.BusinessCore.getLogger().log(Level.INFO, "Loaded business {0} with owner as {1}!", new Object[]{b.getName(), b.getOwnerName()});
                }*/
            }
          }
    }

    /**
     * Base method for creating a new business
     * @param build
     * @return a new business
     */
    public static Business createBusiness(Business.Builder build) {
        Business b = build.build();
        businessList.add(b);
        /*if(Information.debug) {
            Information.log.log(Level.INFO, "Created a business with name {0}", build.getName());
        }*/
        return b;
    }


    /**
     * Gets a business based on id
     * @param id The id of the business
     * @return The business
     */
    public static Business getBusiness(int id) {
        Business b = null;
        Business[] array = businessList.toArray(new Business[]{});
        for (Business business : array) {
            if(business.getID() == id) {
                b = business;
                break;
            }
        }
        return b;
    }

    /**
     * Finds a business based on the name of the current owner,
     * 100% match is not guaranteed (ownership change)
     * @param name Name of the owner
     * @return The business
     */
    public static Business getBusiness(String name) {
        for(Business b : businessList) {
            if(b.getOwnerName().equalsIgnoreCase(name)) {
                return b;
            } else {
                continue;
            }
        }
        return null;
    }
    
    public static int getID(String bname) {
        int id = -1;
        for(Business b : businessList) {
            if(b.getName().equalsIgnoreCase(bname)) {
                id = b.getID();
                break;
            }
        }
        return id;
    }
    
    /**
     * Finds an open business ID for a newly created business
     * @return The open id
     * @throws NoOpenIDException
     */
    public static int openID() throws NoOpenIDException {
        int id = 1000;
        Random r = new Random();
        id = (r.nextInt(1000) + 1000);
        for(Business b : businessList) {
            if(b.getID() == id) {
                id = (r.nextInt(5000) + 5000);
                for(Business b1 : businessList) {
                    if(b1.getID() == id) {
                        id = (r.nextInt(10000) + 10000);
                        for(Business b2 : businessList) {
                            if(b2.getID() == id) {
                                throw new NoOpenIDException(id);
                            }
                        }
                    }
                }
            }
        }
        return id;
    }

    /**
     * Deletes a business from storage and in memory
     * @param business The business to be deleted
     */
    public static void deleteBusiness(Business business) {
        try {
            businessList.remove(business);
            
            BusinessCore.getInstance().getBusinessFileManager().editConfig(new FileData().add(business.getName(), null));
            
            BusinessCore.getInstance().getLogger().log(Level.WARNING, business.getOwnerName() + " has just deleted business " + business.getName());
            Bukkit.getServer().getPlayer(business.getOwnerName()).sendMessage(BusinessCore.getPrefix(BusinessCore.NOMINAL) + "Your business has been deleted");
        } catch (Exception ex) {
            //
        }
    }

    /**
     * Checks if the player is an owner via a null check
     * using the getBusiness(name) method
     * @param name The name of the player
     * @return True if name has a business, false if not.
     */
    public static boolean isOwner(String name) {
        for(Business b : businessList) {
            if(b.getOwnerName().equalsIgnoreCase(name)) {
                return true;
            } else {
                continue;
            }
        }
        return false;
    }

    /**
     * Checks if the player is an owner via
     * null check using getbusiness(id) method.
     * @param id The id in question
     * @return True if the id is attached to a business, false if not
     */
    public static boolean isID(int id) {
        return getBusiness(id) != null;
    }


    public static ArrayList<Business> sortById() {
        businessList.sort((Business b1, Business b2) -> Integer.compare(b1.getID(), b2.getID()));
        return businessList;
    }

    public static ArrayList<Business> sortByBalance() {
        businessList.sort((Business b1, Business b2) -> Double.compare(b1.getBalance(), b2.getBalance()));
        return businessList;
    }

    public static ArrayList<Business> sortByName() {
        businessList.sort((Business b1, Business b2) -> b1.getName().compareTo(b2.getName()));
        return businessList;
    }
}
