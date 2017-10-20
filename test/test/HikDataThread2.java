package test;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.tomcat.util.modeler.Registry;

/**
 * @author tianjunwei
 * @time 2017  2017年9月16日 上午11:13:28
 */
public class HikDataThread2 extends Thread{

	  protected MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
	
	  protected final Vector<ObjectName> threadPools = new Vector<>();
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
		while(true){
			Enumeration<ObjectName> enumeration = threadPools.elements();
            while (enumeration.hasMoreElements()) {
                ObjectName objectNameData = enumeration.nextElement();
               try {
            	   mBeanServer.getAttribute(objectNameData, "maxThreads");
            	   mBeanServer.getAttribute(objectNameData, "currentThreadCount");
            	   mBeanServer.getAttribute(objectNameData, "currentThreadsBusy");
                   Object value = mBeanServer.getAttribute(objectNameData, "keepAliveCount");
                  
				} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e1) {
					e1.printStackTrace();
				}
              
            }
            try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
		}
	}

}
