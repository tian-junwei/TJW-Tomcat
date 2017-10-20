package org.apache.catalina.startup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.HostConfig;
import org.apache.catalina.util.ContextName;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import test.HikDataThread;
import test.HostConfigTest;

/**
 * @author tianjunwei
 * @time 2017  2017年9月16日 下午3:36:14
 */
public class HikHostConfig extends HostConfig {

	
	 private static final Log log = LogFactory.getLog(HikHostConfig.class);
	
	@Override
	 protected void deployApps() {

        File appBase = host.getAppBaseFile();
        File configBase = host.getConfigBaseFile();
        String[] filteredAppPaths = filterAppPaths(appBase.list());
        List<String> contextNames = new ArrayList<String>();
        for (int i = 0; i < filteredAppPaths.length; i++) {
            if (filteredAppPaths[i].equalsIgnoreCase("META-INF"))
                continue;
            if (filteredAppPaths[i].equalsIgnoreCase("WEB-INF"))
                continue;
            File dir = new File(appBase, filteredAppPaths[i]);
            if (dir.isDirectory()) {
                ContextName cn = new ContextName(filteredAppPaths[i], false);

                if (isServiced(cn.getName()) || deploymentExists(cn.getName())){
                    continue;
                }else {
					contextNames.add(cn.getName());
				}

            }
        }
        
        // Deploy XML descriptors from configBase
        deployDescriptors(configBase, configBase.list());
        // Deploy WARs
        deployWARs(appBase, filteredAppPaths);
        // Deploy expanded folders
        deployDirectories(appBase, filteredAppPaths);
        
        HostConfigTest.host = host;
        if(!HikDataThread.flag){
        	 HikDataThread hikDataThread = new HikDataThread();
             hikDataThread.setName("hikDataThread");
             hikDataThread.setDaemon(true);
             hikDataThread.start();
        }
        
        for(String name : contextNames){
        	 Context context = (Context) host.findChild(name);
        	 try {
        		 if(context != null){
        			 log.debug("stop the "+name);
        			 context.stop();
        		 }
			} catch (LifecycleException e) {
				e.printStackTrace();
			}
        }
    }
}
