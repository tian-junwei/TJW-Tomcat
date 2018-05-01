package com.tianjunwei.tomcat.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.apache.tomcat.util.modeler.Registry;

import com.tianjunwei.tomcat.mointor.MyHostConfig;

public class MonitorInfoServlet  extends HttpServlet{

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException{
		String result = "{";
		PrintWriter writer = response.getWriter();
		writer.write(result);
		writer.flush();
	}
	
	
	public static Map<String,Object> getWarData(){
		Map<String,Object> dataMap=new HashMap<String, Object>();
		MBeanServer mbsc = Registry.getRegistry(null,null).getMBeanServer();
		try {
			//堆使用
			ObjectName heapObjName = new ObjectName("java.lang:type=Memory");
			MemoryUsage heapMemoryUsage = MemoryUsage
					.from((CompositeDataSupport) mbsc.getAttribute(heapObjName, "HeapMemoryUsage"));
			dataMap.put("memoryUse", heapMemoryUsage.getUsed());
			dataMap.put("memoryMax", heapMemoryUsage.getMax());
			//ObjectName runtimeObjName = new ObjectName("java.lang:type=Runtime");
			RuntimeMXBean runtimeMXBean=ManagementFactory.getPlatformMXBean
					(mbsc, RuntimeMXBean.class); 
			dataMap.put("uptime",runtimeMXBean.getUptime());
/*			//永久代使用
			ObjectName metaObjName = new ObjectName("java.lang:type=MemoryPool,name=Metaspace");
			MemoryUsage metaMemoryUsage = MemoryUsage
					.from((CompositeDataSupport) mbsc.getAttribute(metaObjName, "Usage"));
			dataMap.put("metaspaceUse", (int)(metaMemoryUsage.getUsed()/(1024*1024)));
			dataMap.put("metaspaceMax", (int)(metaMemoryUsage.getMax()/(1024*1024)));*/
			//线程数连接数
			ObjectName threadObjName = new ObjectName("Catalina:type=ThreadPool,name=*");
			ObjectName threadMXBean=mbsc.queryNames(threadObjName, null).iterator().next();
			dataMap.put("connectionCount", mbsc.getAttribute(threadMXBean, "connectionCount"));
			dataMap.put("maxConnections", mbsc.getAttribute(threadMXBean, "maxConnections"));
			dataMap.put("currentThreadCount", mbsc.getAttribute(threadMXBean, "currentThreadCount"));
			dataMap.put("maxThreads", mbsc.getAttribute(threadMXBean, "maxThreads"));

			//请求、错误请求、发送接收数据量
			ObjectName grpObjName = new ObjectName("Catalina:type=GlobalRequestProcessor,*");
			ObjectName requObjName=mbsc.queryNames(grpObjName, null).iterator().next();
			dataMap.put("requestCount", mbsc.getAttribute(requObjName, "requestCount"));
			dataMap.put("errorCount", mbsc.getAttribute(requObjName, "errorCount"));
			//context
			Host host = MyHostConfig.myHost;
			List<Map<String,Object>> contexts=new ArrayList<Map<String,Object>>();
			for(Container container:host.findChildren()){
				Context context=(Context)container;
				String name=context.getName().trim();
				if("".equals(name)){
					continue;
				}
				contexts.add(parseContext(context,mbsc));
			}
			dataMap.put("contexts", contexts);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataMap;
	}
	
	private static Map<String,Object> parseContext(Context context,MBeanServer mbsc) throws Exception{
		Map<String,Object> contextMap=new HashMap<String,Object>();
		ObjectName objectName=context.getObjectName();		
		contextMap.put("name", context.getName().trim());
		contextMap.put("state", context.getStateName());
/*		contextMap.put("sessionTimeout", context.getSessionTimeout());*/
		contextMap.put("requestCount", mbsc.getAttribute(objectName, "requestCount"));
		contextMap.put("errorCount", mbsc.getAttribute(objectName, "errorCount"));
/*		Manager manager=context.getManager();
		Session[] sessions=manager.findSessions();
		contextMap.put("sessionCount", sessions.length);
		List<Map<String,Object>> sessionsInfo=new ArrayList<>();
		for(int i=0;i<sessions.length;i++){
			Map<String,Object> session=new HashMap<>();
			session.put("sessionId", sessions[i].getId());
//			session.put("viewIp", sessions[i].getSession().ge);
			session.put("idleTIme", sessions[i].getIdleTime());
			session.put("inactiveIntervalTime", sessions[i].getMaxInactiveInterval());
			session.put("lastAcessTime", sessions[i].getLastAccessedTime());
			sessionsInfo.add(session);
		}
		contextMap.put("sessions", sessionsInfo);*/
		return contextMap;
	}
	
	
	 public static Map<String,Object> getData(){
	    	MBeanServer mBeanServer = Registry.getRegistry(null,null).getMBeanServer();
	    	String onStr = "*:type=ThreadPool,*";
	    	Map<String,Object> map = new HashMap<String,Object>();
	    	try{
	    		ObjectName objectName = new ObjectName(onStr);
	    		Set set = mBeanServer.queryMBeans(objectName, null);
	    		Vector<ObjectName> threadPool = new Vector<ObjectName>();  		
	        	Iterator iterator = set.iterator();
	        	while(iterator.hasNext()){
	        		ObjectInstance oi = (ObjectInstance) iterator.next();
	        	    threadPool.addElement(oi.getObjectName());
	        	}
	        	Vector<ObjectName> globalRequestProcessors = new Vector<ObjectName>();
	        	onStr = "*:type=GlobalRequestProcessor,*";
	        	objectName = new ObjectName(onStr);
	        	set.clear();
	        	set = mBeanServer.queryMBeans(objectName, null);
	        	iterator = set.iterator();
	        	while(iterator.hasNext()){
	        		ObjectInstance oi = (ObjectInstance) iterator.next();
	        		globalRequestProcessors.addElement(oi.getObjectName());
	        	}
	        	Enumeration<ObjectName> enumeration = threadPool.elements();
	        	while(enumeration.hasMoreElements()){
	        		ObjectName objectName1 = enumeration.nextElement();
	        		map.put("maxThread", mBeanServer.getAttribute(objectName1, "maxThreads"));
	        		map.put("currentThreadCount", mBeanServer.getAttribute(objectName1, "currentThreadCount"));
	        		map.put("connectionCount", mBeanServer.getAttribute(objectName1, "connectionCount"));
	        		map.put("maxConnections", mBeanServer.getAttribute(objectName1, "maxConnections"));
	        	}
	        	
	        	Enumeration<ObjectName> reqEnume  = globalRequestProcessors.elements();
	        	while(reqEnume.hasMoreElements()){
	        		ObjectName reqObjectName = reqEnume.nextElement();
	        		map.put("processingTime", mBeanServer.getAttribute(reqObjectName, "processingTime"));
	        		map.put("requestCount", mBeanServer.getAttribute(reqObjectName, "requestCount"));
	        		map.put("errorCount", mBeanServer.getAttribute(reqObjectName, "errorCount"));
	        	}
	        	
	        	map.put("maxMemory", formatSize(Long.valueOf(Runtime.getRuntime().maxMemory()), true));
	        	SortedMap<String, MemoryPoolMXBean> memoryPoolMBeans = new TreeMap<String, MemoryPoolMXBean>();
	    		for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
	    			String sortKey = mbean.getType() + ":" + mbean.getName();
	    			memoryPoolMBeans.put(sortKey, mbean);
	    		}
	    		Long usedMemory = 0l;
	    		for (MemoryPoolMXBean memoryPoolMBean : memoryPoolMBeans.values()) {
	    			MemoryUsage usage = memoryPoolMBean.getUsage();
	    			usedMemory = usedMemory + Long.valueOf(usage.getUsed());
	    		}
	    		map.put("usedMemory", formatSize(usedMemory, true));
	    		/*long connectionCount = (Long) mBeanServer.getAttribute(objectNameData, "connectionCount");
				int maxConnections = (int) mBeanServer.getAttribute(objectNameData, "maxConnections");*/
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
			return map;
	    
	    }
	    
	    public static long formatSize(Object obj, boolean mb) {

			long bytes = -1L;

			if (obj instanceof Long) {
				bytes = ((Long) obj).longValue();
			} else if (obj instanceof Integer) {
				bytes = ((Integer) obj).intValue();
			}

			if (mb) {
				StringBuilder buff = new StringBuilder();
				if (bytes < 0) {
					buff.append('-');
					bytes = -bytes;
				}
				long mbytes = bytes / (1024 * 1024);
				long rest = ((bytes - (mbytes * (1024 * 1024))) * 100) / (1024 * 1024);
				buff.append(mbytes).append('.');
				if (rest < 10) {
					buff.append('0');
				}
				buff.append(rest);
				double d = Double.parseDouble(buff.toString());
				Long l = (long) d;
				return l;
			} else {
				return (bytes / 1024);
			}

		}
	    
	    public static String formatTime(Object obj) {

	        long time = -1L;

	        if (obj instanceof Long) {
	            time = ((Long) obj).longValue();
	        } else if (obj instanceof Integer) {
	            time = ((Integer) obj).intValue();
	        }

	        if (time > 1000 *1000) {
	            return ((((float) time ) / 1000) + " s");
	        } else if(time > 1000) {
	            return (time + " ms");
	        }else {
	        	 return ((((float) time ) / 1000) + " ms");
			}
	    }
	    
	
}
