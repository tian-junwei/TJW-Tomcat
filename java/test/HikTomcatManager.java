package test;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;

/**
 * @author tianjunwei
 * @time 2017  2017年9月19日 下午6:43:55
 */
public class HikTomcatManager {

	private static Host host =  null;
	
	public static boolean startService(String serviceName){

		if(host == null){
			host =  HostConfigTest.host;
		}
		
		boolean flag = false;
		
		if(serviceName == null || serviceName.length() < 1 || host == null){
			return flag;
		}
		if(!serviceName.startsWith("/")){
			serviceName = "/"+serviceName;
		}
		Context context = (Context) host.findChild(serviceName);
		if(context == null){
			return flag;
		}
		try {
			context.start();
			flag = true;
		} catch (LifecycleException e) {
			
		}
		return flag;
		
	}
	
	public static boolean stopService(String serviceName){
		boolean flag = false;
		
		if(host == null){
			host =  HostConfigTest.host;
		}
		
		if(serviceName == null || serviceName.length() < 1 || host == null){
			return flag;
		}
		if(!serviceName.startsWith("/")){
			serviceName = "/"+serviceName;
		}
		Context context = (Context) host.findChild(serviceName);
		if(context == null){
			return flag;
		}
		try {
			context.stop();
			flag = true;
		} catch (LifecycleException e) {
			
		}
		return flag;
	}
}
