package me.beastman3226.bc.listener;

import me.beastman3226.bc.BusinessCore;
import me.beastman3226.bc.business.Business;
import me.beastman3226.bc.business.BusinessManager;
import me.beastman3226.bc.data.file.FileData;
import me.beastman3226.bc.event.business.BusinessBalanceChangeEvent;
import me.beastman3226.bc.event.business.BusinessClosedEvent;
import me.beastman3226.bc.event.business.BusinessCreatedEvent;
import me.beastman3226.bc.event.business.BusinessFiredEmployeeEvent;
import me.beastman3226.bc.event.business.BusinessHiredEmployeeEvent;
import me.beastman3226.bc.player.EmployeeManager;
import me.beastman3226.bc.util.Message;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 *
 * @author beastman3226
 */
public class BusinessListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBalanceChange(BusinessBalanceChangeEvent e) {
        Economy eco = BusinessCore.getInstance().getEconomy();
        if (e.getSource() != null) {
            if (e.isWithdrawal()) {
                if (!e.isOverdraft()) {
                    if (e.getSource() instanceof Player) {
                        EconomyResponse er = eco.depositPlayer((Player) e.getSource(), e.getAbsoluteAmount());
                        if (!er.transactionSuccess()) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                    Message message = new Message("business.withdraw.success", e.getSource()).setBusiness(e.getBusiness()).setOther(e.getAbsoluteAmount());
                    message.sendMessage();
                    e.getBusiness().withdraw(e.getAbsoluteAmount());
                } else {
                    Message message = new Message("business.withdraw.overdraft", e.getSource()).setBusiness(e.getBusiness()).setOther(e.getAbsoluteAmount());
                    message.sendMessage();
                }
            } else {
                if (e.getSource() instanceof Player) {
                    EconomyResponse er = eco.withdrawPlayer((Player) e.getSource(), e.getAmount());
                    if (!er.transactionSuccess()) {
                        e.setCancelled(true);
                        return;
                    }
                }
                Message message = new Message("business.deposit", e.getSource()).setBusiness(e.getBusiness()).setOther(e.getAbsoluteAmount());
                message.sendMessage();
                e.getBusiness().deposit(e.getAbsoluteAmount());
            }
        } else {
            if(e.isWithdrawal()) {
                if(e.isOverdraft())
                    e.setCancelled(true);
                else
                    e.getBusiness().withdraw(e.getAbsoluteAmount());
            } else {
                e.getBusiness().deposit(e.getAbsoluteAmount());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClosed(BusinessClosedEvent e) {
        Business b = e.getBusiness();
        if (b.getOwner().isOnline()) {
            Message message = new Message("business.close", e.getSource()).setRecipient(b.getOwner()).setBusiness(e.getBusiness());
            message.sendMessage();
        }
        if(!e.getSource().equals(b.getOwner())) {
            Message message = new Message("business.close", e.getSource()).setBusiness(e.getBusiness());
            message.sendMessage();
        }
        BusinessManager.closeBusiness(e.getBusiness());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreated(BusinessCreatedEvent e) {
        Player owner = e.getBusiness().getOwner();
        Message message = new Message("business.start", owner, e.getBusiness());
        message.sendMessage();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFire(BusinessFiredEmployeeEvent e) {
        Message message = new Message("business.employee.fire", e.getBusiness().getOwner(), e.getBusiness()).setCause(Bukkit.getPlayer(e.getEmployee().getUniqueId()));
        message.sendMessage();
        EmployeeManager.getEmployeeList().remove(e.getEmployee());
        e.getBusiness().removeEmployee(e.getEmployee());
        BusinessCore.getInstance().getEmployeeFileManager()
                .edit(new FileData().add(e.getEmployee().getName() + "", null));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHire(BusinessHiredEmployeeEvent e) {
        Player owner = e.getBusiness().getOwner();
        Player employee = Bukkit.getPlayer(e.getEmployee().getUniqueId());
        if (owner.isOnline()) {
            Message message = new Message("business.employee.hire.offer_accepted_owner", owner, e.getBusiness()).setCause(employee).setEmployee(e.getEmployee());
            message.sendMessage();
        }
        if (employee.isOnline()) {
            Message message = new Message("business.employee.hire.offer_accepted_employee", employee, e.getBusiness()).setCause(owner).setEmployee(e.getEmployee());
            message.sendMessage();
        }
        e.getBusiness().addEmployee(e.getEmployee());
        EmployeeManager.getEmployeeList().add(e.getEmployee());
    }
}
