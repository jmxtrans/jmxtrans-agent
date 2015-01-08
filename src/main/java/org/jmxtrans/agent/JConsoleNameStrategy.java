package org.jmxtrans.agent;

import javax.management.ObjectName;
import java.util.Map;

public class JConsoleNameStrategy implements ResultNameStrategy {
    @Override
    public String getResultName(Query query, ObjectName objectName, String key, String attribute) {
        String result = objectName.getDomain();
        if(objectName.getKeyProperty("type") != null) {
            result += "." + objectName.getKeyProperty("type");
        }
        if(objectName.getKeyProperty("name") != null) {
            result += "." + objectName.getKeyProperty("name");
        }
        for (Map.Entry<String, String> entry : objectName.getKeyPropertyList().entrySet()) {
            if(!entry.getKey().equalsIgnoreCase("name") && !entry.getKey().equalsIgnoreCase("type")){
                result += "." + entry.getValue();
            }
        }

        result += "." + attribute;

        if(key != null && !key.isEmpty()){
            result = result + "." + key;
        }

        return result;
    }

    @Override
    public void postConstruct(Map<String, String> settings) {

    }
}
