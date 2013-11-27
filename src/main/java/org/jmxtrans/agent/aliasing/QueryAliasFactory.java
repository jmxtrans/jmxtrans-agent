package org.jmxtrans.agent.aliasing;

import javax.management.ObjectName;

import org.jmxtrans.agent.Query;

public interface QueryAliasFactory {

	String valueOf(Query query, ObjectName objectName, String key);
	
}
