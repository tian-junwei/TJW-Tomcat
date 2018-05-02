package com.tianjunwei.tomcat.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.ServerInfo;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.modeler.Registry;

import com.alibaba.fastjson.JSON;
import com.tianjunwei.tomcat.mointor.MyHostConfig;

public class MonitorInfoServlet  extends HttpServlet implements NotificationListener {

	private static final long serialVersionUID = 1L;

	protected MBeanServer mBeanServer = null;
	
	protected final Vector<ObjectName> protocolHandlers = new Vector<>();
	
	protected final Vector<ObjectName> threadPools = new Vector<>();
	
    protected final Vector<ObjectName> globalRequestProcessors = new Vector<>();
    
    protected final Vector<ObjectName> requestProcessors = new Vector<>();
	
	@Override
	public void init() throws ServletException {

		// Retrieve the MBean server
		mBeanServer = Registry.getRegistry(null, null).getMBeanServer();

		try {

			// Query protocol handlers
			String onStr = "*:type=ProtocolHandler,*";
			ObjectName objectName = new ObjectName(onStr);
			Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
			Iterator<ObjectInstance> iterator = set.iterator();
			while (iterator.hasNext()) {
				ObjectInstance oi = iterator.next();
				protocolHandlers.addElement(oi.getObjectName());
			}

			// Query Thread Pools
			onStr = "*:type=ThreadPool,*";
			objectName = new ObjectName(onStr);
			set = mBeanServer.queryMBeans(objectName, null);
			iterator = set.iterator();
			while (iterator.hasNext()) {
				ObjectInstance oi = iterator.next();
				threadPools.addElement(oi.getObjectName());
			}

			// Query Global Request Processors
			onStr = "*:type=GlobalRequestProcessor,*";
			objectName = new ObjectName(onStr);
			set = mBeanServer.queryMBeans(objectName, null);
			iterator = set.iterator();
			while (iterator.hasNext()) {
				ObjectInstance oi = iterator.next();
				globalRequestProcessors.addElement(oi.getObjectName());
			}

			// Query Request Processors
			onStr = "*:type=RequestProcessor,*";
			objectName = new ObjectName(onStr);
			set = mBeanServer.queryMBeans(objectName, null);
			iterator = set.iterator();
			while (iterator.hasNext()) {
				ObjectInstance oi = iterator.next();
				requestProcessors.addElement(oi.getObjectName());
			}

			// Register with MBean server
			onStr = "JMImplementation:type=MBeanServerDelegate";
			objectName = new ObjectName(onStr);
			mBeanServer.addNotificationListener(objectName, this, null, null);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException{
		PrintWriter writer = response.getWriter();
		writer.write(JSON.toJSONString(getWarData()));
		writer.flush();
	}
	
	/**
	 * 获取操作系统及jvm相关的信息
	 */
	public static Map<String, Object> getServerData(){
		
		Map<String, Object> serverMap =new HashMap<String, Object>();
		serverMap.put("tomcat_version", ServerInfo.getServerInfo());
		serverMap.put("jvm_version", System.getProperty("java.runtime.version"));
		serverMap.put("jvm_vendor", System.getProperty("java.vm.vendor"));
		serverMap.put("os_name", System.getProperty("os.name"));
		serverMap.put("os_version", System.getProperty("os.version"));
		serverMap.put("os_arch", System.getProperty("os.arch"));
		
        try {
            InetAddress address = InetAddress.getLocalHost();
            serverMap.put("hostName", address.getHostName());
            serverMap.put("hostAddress", address.getHostAddress());
         } catch (UnknownHostException e) {
        	 serverMap.put("hostName", "-");
             serverMap.put("hostAddress", "-");
        }
		return serverMap;
	}
	
	/**
	 * 
	 * 获取操作系统相关信息
	 */
	public static Map<String, Object> getOsData(){
		Map<String, Object> osMap = new HashMap<String, Object>();
		long[] result = new long[16];
	    boolean ok = false;
	    try {
	    	String methodName = "info";
	    	Class<?> paramTypes[] = new Class[1];
	    	paramTypes[0] = result.getClass();
	    	Object paramValues[] = new Object[1];
	    	paramValues[0] = result;
	    	Method method = Class.forName("org.apache.tomcat.jni.OS")
	                .getMethod(methodName, paramTypes);
	    	method.invoke(null, paramValues);
	    	ok = true;
	    } catch (Throwable t) {
	    	t = ExceptionUtils.unwrapInvocationTargetException(t);
	    	ExceptionUtils.handleThrowable(t);
	    }

	    if (ok) {
	    	osMap.put(" Physical memory",formatSize(Long.valueOf(result[0]), true));
	    	osMap.put(" Available memory: ",formatSize(Long.valueOf(result[1]), true));
	    	osMap.put(" Total page file: ",formatSize(Long.valueOf(result[2]), true));
	    	osMap.put(" Free page file: ",formatSize(Long.valueOf(result[3]), true));
	    	osMap.put(" Memory load: ",Long.valueOf(result[6]));
	    	osMap.put(" Process kernel time: ",formatTime(Long.valueOf(result[11] / 1000), true));
	    	osMap.put(" Process user time: ",formatTime(Long.valueOf(result[12] / 1000), true));
	    }
		return osMap;
	}
	
	public static Map<String, Object> getJVMData(){
		Map<String, Object> jvmMap = new HashMap<String,Object>();
		SortedMap<String, MemoryPoolMXBean> memoryPoolMBeans = new TreeMap<>();
        for (MemoryPoolMXBean mbean: ManagementFactory.getMemoryPoolMXBeans()) {
            String sortKey = mbean.getType() + ":" + mbean.getName();
            memoryPoolMBeans.put(sortKey, mbean);
        }
        jvmMap.put("free_memory",Runtime.getRuntime().freeMemory());
        jvmMap.put("total_memory",Runtime.getRuntime().totalMemory());
        jvmMap.put("max_memory" , Runtime.getRuntime().maxMemory());

        List<Map<String, Object>> memory_list = new ArrayList<Map<String, Object>>();
        jvmMap.put("memory_pool", memory_list);
        for (MemoryPoolMXBean memoryPoolMBean : memoryPoolMBeans.values()) {
            MemoryUsage usage = memoryPoolMBean.getUsage();
            Map<String, Object> useMap = new HashMap<String, Object>();
            useMap.put("name",memoryPoolMBean.getName());
            useMap.put("type",memoryPoolMBean.getType());
            useMap.put("usageInit",usage.getInit());
            useMap.put("usageCommitted",usage.getCommitted());
            useMap.put("usageMax",usage.getMax());
            useMap.put("usageUsed",usage.getUsed());
            memory_list.add(useMap);
        }
        return jvmMap;
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
			ObjectName runtimeObjName = new ObjectName("java.lang:type=Runtime");
			RuntimeMXBean runtimeMXBean=ManagementFactory.getPlatformMXBean
					(mbsc, RuntimeMXBean.class); 
			dataMap.put("uptime",runtimeMXBean.getUptime());
			//永久代使用
			ObjectName metaObjName = new ObjectName("java.lang:type=MemoryPool,name=Metaspace");
			MemoryUsage metaMemoryUsage = MemoryUsage
					.from((CompositeDataSupport) mbsc.getAttribute(metaObjName, "Usage"));
			dataMap.put("metaspaceUse", (int)(metaMemoryUsage.getUsed()/(1024*1024)));
			dataMap.put("metaspaceMax", (int)(metaMemoryUsage.getMax()/(1024*1024)));
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
		Manager manager=context.getManager();
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
		contextMap.put("sessions", sessionsInfo);
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
	    public static String formatTime(Object obj, boolean seconds) {

	        long time = -1L;

	        if (obj instanceof Long) {
	            time = ((Long) obj).longValue();
	        } else if (obj instanceof Integer) {
	            time = ((Integer) obj).intValue();
	        }

	        if (seconds) {
	            return ((((float) time ) / 1000) + " s");
	        } else {
	            return (time + " ms");
	        }
	    }

	    @Override
	    public void handleNotification(Notification notification,
	                                   java.lang.Object handback) {

	        if (notification instanceof MBeanServerNotification) {
	            ObjectName objectName =
	                ((MBeanServerNotification) notification).getMBeanName();
	            if (notification.getType().equals
	                (MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
	                String type = objectName.getKeyProperty("type");
	                if (type != null) {
	                    if (type.equals("ProtocolHandler")) {
	                        protocolHandlers.addElement(objectName);
	                    } else if (type.equals("ThreadPool")) {
	                        threadPools.addElement(objectName);
	                    } else if (type.equals("GlobalRequestProcessor")) {
	                        globalRequestProcessors.addElement(objectName);
	                    } else if (type.equals("RequestProcessor")) {
	                        requestProcessors.addElement(objectName);
	                    }
	                }
	            } else if (notification.getType().equals
	                       (MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
	                String type = objectName.getKeyProperty("type");
	                if (type != null) {
	                    if (type.equals("ProtocolHandler")) {
	                        protocolHandlers.removeElement(objectName);
	                    } else if (type.equals("ThreadPool")) {
	                        threadPools.removeElement(objectName);
	                    } else if (type.equals("GlobalRequestProcessor")) {
	                        globalRequestProcessors.removeElement(objectName);
	                    } else if (type.equals("RequestProcessor")) {
	                        requestProcessors.removeElement(objectName);
	                    }
	                }
	                String j2eeType = objectName.getKeyProperty("j2eeType");
	                if (j2eeType != null) {

	                }
	            }
	        }

	    }
	    
	
}
