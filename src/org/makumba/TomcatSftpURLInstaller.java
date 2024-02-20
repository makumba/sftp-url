package org.makumba;

import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;

/**
   To install the sftp stream handler factory in a tomcat environment, you need to use the tomcat factory, 
   because the JVM does not allow adding multiple factories.
   This lifecycle listener can be installed in the tomcat configuration (conf/server.xml) or in your context configuration (META-INF/context.xml)
<Listener className="org.makumba.TomcatSftpURLInstaller" />
   In both cases, it is recommended that the dependencies (sftp-uril.jar and jsch.jar) are placed in the tomcat lib/ folder 
   beacause most Jsch objects are hanged in a thread (one for each SFTP Session). In case of webapp reload the thread will not find the classes for the objects it needs.
 */
public class TomcatSftpURLInstaller implements LifecycleListener{
    boolean installed;

    @Override
    public void lifecycleEvent(LifecycleEvent evt) {
        if (!installed)
            if (TomcatURLStreamHandlerFactory.getInstance().isRegistered()) {
                TomcatURLStreamHandlerFactory.getInstance().addUserFactory(new SftpURLStreamHandlerFactory());
		installed = true;
		System.out.println("added sftp URL factory");
            }
    }
}



