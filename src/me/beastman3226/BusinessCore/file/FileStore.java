package me.beastman3226.BusinessCore.file;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.beastman3226.BusinessCore.BusinessMain;
import me.beastman3226.BusinessCore.business.Business;
import me.beastman3226.BusinessCore.business.BusinessManager;
import me.beastman3226.BusinessCore.job.Job;
import me.beastman3226.BusinessCore.player.Employee;

/**
 *
 * @author beastman3226
 */
public class FileStore {

    /**
     * Updates information in the business file.
     * @param path the path to be updated
     * @param updateWith the path to be updated
     */
    public static void update(String path, Object updateWith) {
        if(updateWith == null) deleteBusiness(path);
        if(BusinessMain.flatfile.contains(path)) {
            String business = BusinessMain.flatfile.getString(path);
            BusinessMain.logger.log(Level.INFO, "Found {0}...", path);
            BusinessMain.logger.info(("Updating..."));
            BusinessMain.flatfile.set(path, updateWith);
            BusinessMain.logger.log(Level.INFO, "Updated {0} with {1} from {2}", new Object[]{path, updateWith, business});
        } else {
            BusinessMain.logger.log(Level.INFO, "Could not find path: {0}", path);
            BusinessMain.logger.info(("Creating path..."));
            BusinessMain.flatfile.set(path, updateWith);
            BusinessMain.logger.log(Level.INFO, "File reads: {0}: ''{1}''", new Object[]{path, updateWith});
        }
    }

    /**
     * Saves all the data to the disk
     */
    public static void save() {
        for(Business b : Business.businessList) {
            BusinessMain.flatfile.set(b.getOwnerName() + ".business", b.getName());
            BusinessMain.flatfile.set(b.getOwnerName() + ".employees", b.getEmployeeList().toArray(new String[]{}));
            BusinessMain.flatfile.set(b.getOwnerName() + ".id", b.getIndex());
            BusinessMain.flatfile.set(b.getOwnerName() + ".jobs",  b.getJobList().toArray());
            BusinessMain.flatfile.set(b.getOwnerName() + ".ownerName", b.getOwnerName());
            BusinessMain.flatfile.set(b.getOwnerName() + ".worth", b.getWorth());
            BusinessMain.flatfile.saveConfig();
        }
        HashSet<String> employeeNames = new HashSet<>();
        for(Employee e : Employee.employeeList) {
            BusinessMain.employeeFile.set(e.getEmployeeName() + ".name", e.getEmployeeName());
            BusinessMain.employeeFile.set(e.getEmployeeName() + ".completed", e.getCompletedJobs());
            BusinessMain.employeeFile.set(e.getEmployeeName() + ".scouted", e.getScoutedJobs());
            BusinessMain.employeeFile.set(e.getEmployeeName() + ".current", e.getJob().getId());
            try {
                BusinessMain.employeeFile.save(BusinessMain.employee);
            } catch (IOException ex) {
                Logger.getLogger(FileStore.class.getName()).log(Level.SEVERE, null, ex);
            }
            employeeNames.add(e.getEmployeeName());
        }
        BusinessMain.employeeFile.set("employees", employeeNames.toArray(new String[]{}));
        try {
                BusinessMain.employeeFile.save(BusinessMain.employee);
            } catch (IOException ex) {
                Logger.getLogger(FileStore.class.getName()).log(Level.SEVERE, null, ex);
            }
        HashSet<Integer> jobIds = new HashSet<>();
        for(Job j : Job.jobList) {
            //TODO: Job save
           jobIds.add(j.getId());
        }
        BusinessMain.jobFile.set("jobids", jobIds.toArray());
    }

    /**
     * Loads all the data from the disk
     */
    public static void load() {
        for(Object id : BusinessMain.jobFile.getList("jobIds")) {
            //TODO: id's into jobs
        }
        for(String name : BusinessMain.employeeFile.getStringList("employees")) {
            //TODO: names into employees
            //FIXME: If job has reference to employee, do here
        }
        for(String owner : BusinessMain.flatfile.getStringList("ownernames")) {
            BusinessManager.createBusiness(BusinessMain.flatfile.getString(owner + ".business"), owner);
            BusinessManager.deposit(owner, BusinessMain.flatfile.getDouble(owner + ".worth"));
            Vector<String> employees = new Vector<>();
            employees.addAll(BusinessMain.flatfile.getStringList(owner + ".employees"));
            BusinessManager.getBusiness(owner).setEmployeeList(employees);
            Vector<Integer> jobs = new Vector<>();
            for(Object o : BusinessMain.flatfile.getList(owner + ".jobs")) {
                int k = (int) o;
                jobs.add(k);
            }
            BusinessManager.getBusiness(owner).setJobList(toJob(jobs));
        }
    }

    private static void deleteBusiness(String path) {
        BusinessMain.flatfile.set(path, null);
    }

    public static Iterator<Business> getAll() {
        Set<String> keys = BusinessMain.flatfile.getKeys();
        Map<String, Object> values = BusinessMain.flatfile.getConfig().getValues(true);
        Set<Business> businesses = new HashSet<>();
        for(Object value : values.values()) {
            businesses.add((Business) value);
        }
        return businesses.iterator();
    }

    private static Vector<Job> toJob(Vector<Integer> jobs) {
        //TODO: method logice for grabbing jobs
        return null;
    }

}
