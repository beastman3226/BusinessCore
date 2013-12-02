package me.beastman3226.bc.data;

import java.util.HashMap;

/**
 *
 * @author beastman3226
 */
public enum Data {

    BUSINESS(),
    EMPLOYEE(),
    JOB();

    Data() {

    }
    private HashMap<String, Object> dataColumn = new HashMap<>();

    public Data add(String column, Object data) {
        dataColumn.put(column, data);
        return this;
    }

    public HashMap getData() {
        return this.dataColumn;
    }
}
