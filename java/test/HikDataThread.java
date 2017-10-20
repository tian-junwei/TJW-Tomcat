package test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.tomcat.util.buf.StringUtils;
import org.apache.tomcat.util.modeler.Registry;

import test.bean.AppData;

/**
 * @author tianjunwei
 * @time 2017/9/16 11:13:28
 */
public class HikDataThread extends Thread {

	private Queue<Integer> queue = new LinkedList<Integer>();

	private final int dataSize = 24 * 60;

	private static Map<String, Object> monitorData = new HashMap<String, Object>();
	
	private static Map<String, AppData> monitorAppData = new HashMap<String, AppData>();

	protected MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();

	protected final Vector<ObjectName> threadPools = new Vector<>();
	protected final Vector<ObjectName> globalRequestProcessors = new Vector<>();
	public static volatile boolean flag = false;

	@Override
	public void run() {
		synchronized (mBeanServer) {
			flag = true;
		}
		String onStr = "*:type=ThreadPool,*";
		ObjectName objectName = null;
		try {
			objectName = new ObjectName(onStr);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		}
		Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
		Iterator<ObjectInstance> iterator = set.iterator();
		while (iterator.hasNext()) {
			ObjectInstance oi = iterator.next();
			threadPools.addElement(oi.getObjectName());
		}
		onStr = "*:type=GlobalRequestProcessor,*";
		try {
			objectName = new ObjectName(onStr);
		} catch (MalformedObjectNameException e2) {
			e2.printStackTrace();
		}
		set = mBeanServer.queryMBeans(objectName, null);
		iterator = set.iterator();
		while (iterator.hasNext()) {
			ObjectInstance oi = iterator.next();
			globalRequestProcessors.addElement(oi.getObjectName());
		}
		while (true) {
			try {
				initAppData();
				initData();
			} catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException
					| MBeanException | ReflectionException e1) {
			}
			try {
				Thread.sleep(1000 * 60);
			} catch (InterruptedException e) {

			}
		}

	}

	public void initData() {

		monitorData.put("maxMemory", formatSize(Long.valueOf(Runtime.getRuntime().maxMemory()), true));

		SortedMap<String, MemoryPoolMXBean> memoryPoolMBeans = new TreeMap<>();
		for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
			String sortKey = mbean.getType() + ":" + mbean.getName();
			memoryPoolMBeans.put(sortKey, mbean);
		}
		Long usedMemory = 0l;
		for (MemoryPoolMXBean memoryPoolMBean : memoryPoolMBeans.values()) {
			MemoryUsage usage = memoryPoolMBean.getUsage();
			usedMemory = usedMemory + Long.valueOf(usage.getUsed());
		}
		monitorData.put("usedMemory", formatSize(usedMemory, true));
		Enumeration<ObjectName> enumeration = threadPools.elements();
		while (enumeration.hasMoreElements()) {
			ObjectName objectNameData = enumeration.nextElement();
			try {
				int maxThreads = (int) mBeanServer.getAttribute(objectNameData, "maxThreads");
				monitorData.put("maxThreads", maxThreads);
				int currentThreadCount = (int) mBeanServer.getAttribute(objectNameData, "currentThreadCount");
				monitorData.put("currentThreadCount", currentThreadCount);
				// int currentThreadsBusy = (int)
				// mBeanServer.getAttribute(objectNameData,
				// "currentThreadsBusy");
				// int keepAliveCount = (int)
				// mBeanServer.getAttribute(objectNameData, "keepAliveCount");
				// monitorData.put("keepAliveCount", keepAliveCount);
				// monitorData.put("currentThreadsBusy", currentThreadsBusy);

				// int acceptCount = (int)
				// mBeanServer.getAttribute(objectNameData, "acceptCount");
				long connectionCount = (Long) mBeanServer.getAttribute(objectNameData, "connectionCount");
				int maxConnections = (int) mBeanServer.getAttribute(objectNameData, "maxConnections");
				
				if(monitorData.get("maxCurrentConnectionCount") == null || (long) monitorData.get("maxCurrentConnectionCount") < connectionCount){
					monitorData.put("maxCurrentConnectionCount", connectionCount);
				}
				
				monitorData.put("connectionCount", connectionCount);
				monitorData.put("maxConnections", maxConnections);

				String name = objectNameData.getKeyProperty("name");
				ObjectName grpName = null;

				Enumeration<ObjectName> globalRequestProcessorsenumeration = globalRequestProcessors.elements();
				while (globalRequestProcessorsenumeration.hasMoreElements()) {
					ObjectName globalRequestProcessorsobjectName = globalRequestProcessorsenumeration.nextElement();
					if (name.equals(globalRequestProcessorsobjectName.getKeyProperty("name"))) {
						grpName = globalRequestProcessorsobjectName;
					}
				}

				mBeanServer.getAttribute(grpName, "processingTime");
				// int requestCount = (int) mBeanServer.getAttribute(grpName,"requestCount");
				int errorCount = (int) mBeanServer.getAttribute(grpName, "errorCount");
				int oldErrorCount = 0;
				if (queue.size() >= dataSize) {
					oldErrorCount = queue.poll();
				}
				queue.offer(errorCount);
				int tempErrorCount;
				if((tempErrorCount = errorCount - oldErrorCount) > 0){
					monitorData.put("errorCount", tempErrorCount);
				}else {
					monitorData.put("errorCount", errorCount);
				}
				
			} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e1) {
				e1.printStackTrace();
			}

		}
	}

	
	public void initAppData() throws MalformedObjectNameException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException{
		
		ObjectName queryHosts = new ObjectName("*:j2eeType=WebModule,*");
        Set<ObjectName> hostsON = mBeanServer.queryNames(queryHosts, null);
        Iterator<ObjectName> iterator = hostsON.iterator();
        iterator = hostsON.iterator();
        while (iterator.hasNext()) {
        	ObjectName objectName = iterator.next();
        	String webModuleName = objectName.getKeyProperty("name");
        	AppData appData = new AppData();
        	String name = webModuleName.replaceFirst("//localhost/", "");
        	if(name == null  || name.length()<1){
        		name="ROOT";
        	}
        	appData.setApplicationName(name);
        	String onStr = objectName.getDomain() + ":j2eeType=Servlet,WebModule=" + webModuleName + ",*";
            ObjectName servletObjectName = new ObjectName(onStr);
            Set<ObjectInstance> set = mBeanServer.queryMBeans(servletObjectName, null);
            Iterator<ObjectInstance> moduleIterator = set.iterator();
            Long processingTime = 0l;
            int requestCount= 0;
            while (moduleIterator.hasNext()) {
            	ObjectInstance oi = moduleIterator.next();
            	ObjectName moduleObjectName = oi.getObjectName();
            	//执行总时间
            	processingTime = processingTime + (Long)mBeanServer.getAttribute(moduleObjectName, "processingTime");
		        //请求次数
            	requestCount = requestCount + (int) mBeanServer.getAttribute(moduleObjectName, "requestCount");
            }
            if(requestCount > 0){
            	processingTime = processingTime*1000 / requestCount;
            }
            appData.setAverTime(formatTime(processingTime));
            monitorAppData.put(name, appData);
        }
	}
	
	
	public static Map<String, Object> getMonitorData() {
		return monitorData;
	}

	public static String formatSize(Object obj, boolean mb) {

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
			buff.append(rest).append(" MB");
			return buff.toString();
		} else {
			return ((bytes / 1024) + " KB");
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
