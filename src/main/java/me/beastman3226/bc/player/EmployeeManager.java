package me.beastman3226.bc.player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.beastman3226.bc.BusinessCore;
import me.beastman3226.bc.BusinessCore.Information;
import me.beastman3226.bc.data.Data;
import me.beastman3226.bc.data.EmployeeHandler;
import me.beastman3226.bc.data.file.EmployeeFileManager;
import me.beastman3226.bc.data.file.FileData;
import me.beastman3226.bc.db.Table;
import me.beastman3226.bc.errors.OpenJobException;

/**
 *
 * @author beastman3226
 */
public class EmployeeManager {

    public static HashMap<String, Integer> pending = new HashMap<String, Integer>(50);

    public static void loadEmployees() {
        if(Information.database) {
            try {
                ResultSet rs = BusinessCore.Information.connection.createStatement().executeQuery("SELECT * FROM " + Table.EMPLOYEE);
                while(rs.next()) {
                    String name = rs.getString("EmployeeName");
                    int id = rs.getInt("EmployeeID");
                    int bID = rs.getInt("BusinessID");
                    Employee e = null;
                    try {
                        e = new Employee(name, id).setBusiness(bID).setCompletedJobs(rs.getInt("CompletedJobs")).startJob(rs.getInt("JobID"));
                    } catch (OpenJobException ex) {
                        continue;
                    }
                    if(e != null) {
                        Employee.employeeList.add(e);
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(EmployeeManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            for(String key : Information.employeeYml.getKeys(false)) {
                    int id = Information.employeeYml.getInt(key + ".id");
                    String name = key;
                    int bID = Information.employeeYml.getInt(key + ".business");
                    int completed = Information.employeeYml.getInt(key + ".completed");
                    int jID = Information.employeeYml.getInt(key + ".job");
                try {
                    Employee e = new Employee(name, id).setBusiness(bID).setCompletedJobs(completed).startJob(jID);
                    Employee.employeeList.add(e);
                } catch (OpenJobException ex) {
                   continue;
                }
            }
        }
    }

    public static Employee addEmployee(String name) {
        Employee employee = new Employee(name, 1000 + Employee.employeeList.size() + 1);
        Employee.employeeList.add(employee);
        if(Information.database) {
            EmployeeHandler.add(Data.EMPLOYEE.add("EmployeeID", employee.getID())
                                             .add("EmployeeName", name)
                                             .add("BusinessID", employee.getBusiness().getID())
                                             .add("CompletedJobs", employee.getCompletedJobs())
                                             .add("JobID", employee.getCurrentJob()));
        } else {
            EmployeeFileManager.editConfig(new FileData().add(name + ".name", name)
                    .add(name + ".id", employee.getID())
                    .add(name + ".business", employee.getBusiness().getID())
                    .add(name + ".completed", employee.getCompletedJobs())
                    .add(name + ".job", employee.getCurrentJob()));

        }
        return employee;
    }

    public static Employee getEmployee(String name) {
        Employee employee = null;
        for(Employee e : Employee.employeeList) {
            if(e.getName().equalsIgnoreCase(name)) {
                employee = e;
                break;
            }
        }
        return employee;
    }

    public static Employee getEmployee(int id) {
        Employee employee = null;
        for(Employee e : Employee.employeeList) {
            if(e.getID() == id) {
                employee = e;
                break;
            }
        }
        return employee;
    }

    public static boolean isEmployee(String name) {
        return getEmployee(name) != null;
    }
}
