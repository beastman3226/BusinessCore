package me.beastman3226.BusinessCore.data;

import code.husky.mysql.MySQL;
import java.sql.Connection;
import java.sql.Statement;
import org.bukkit.plugin.Plugin;

/**A data object
 *
 * @author beastman3226
 */
public class Data {

    public static MySQL MySQL;
    public static Connection c = null;
    protected static Statement s;


    public static final void startup(Plugin plugin, String ip, String port, String tableName, String user, String pass ) {
        MySQL = new MySQL(plugin, ip, port, tableName, user, pass);
        c = MySQL.openConnection();
    }

}
