package org.jmxtrans.agent.aliasing;

import javax.management.ObjectName;

import org.jmxtrans.agent.Query;

import java.util.Map;

public interface QueryAliasFactory {

	void postConstruct(Map<String, String> settings);

	String valueOf(Query query, ObjectName objectName, String key);

}
