package org.makumba;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
  To support sftp:// URLs for both InputStreams and OutputStreams, pass an instance of this class to 
  java.net.URL.setURLStreamHandlerFactory
  If you are in a tomcat environment, use a TomcatListener to install it in the server JVM.
 */
public class SftpURLStreamHandlerFactory implements URLStreamHandlerFactory{
    HashMap<String, Session> sessions;
    static final int TIMEOUT = 60000;
    JSch jsch = new JSch();
    {
        // JSch.setLogger(new MyLoggr());
    }

    @Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
	if (protocol.equals("sftp")) {
	    return new URLStreamHandler() {
		// jsch.setKnownHosts("./.ssh/known_hosts");
		@Override
		    protected void parseURL(URL u, String spec, int start, int limit) {
		    try {
			URI x = new URI(spec);
			setURL(u, protocol, x.getHost(), x.getPort(), x.getAuthority(), x.getUserInfo(),
			       x.getPath(), x.getQuery(), null);
		    } catch (URISyntaxException e) {
			e.printStackTrace();
		    }
		}
		
		Session connectTo(String userInfo, String host) throws IOException {
		    try {
			String user= userInfo.substring(0, userInfo.indexOf(':'));
			System.out.println("connecting to " + user + "@" + host);
			Session s = jsch.getSession(user, host);
			// jschSession.setConfig("kex", "none");
			// 10 seconds session timeout

			// authenticate using private key
			// jsch.addIdentity("/home/mkyong/.ssh/id_rsa");
			// authenticate using password
			s.setPassword(userInfo.substring(userInfo.indexOf(':') + 1));
			s.setConfig("StrictHostKeyChecking", "no");
			s.connect(TIMEOUT);
			sessions.put(userInfo + "@" + host, s);
			return s;
		    } catch (JSchException e1) {
			throw new IOException(e1);
		    }
		}
		
		ChannelSftp getChannel(String userInfo, String host) throws IOException {
		    if (sessions == null)
			sessions = new HashMap<String, Session>();
		    Session s = sessions.get(userInfo + "@" + host);
		    
		    if (s == null)
			s = connectTo(userInfo, host);
		    
		    JSchException last = null;
		    for (int i = 0; i < 5; i++) {
			Channel sftp;
			try {
			    sftp = s.openChannel("sftp");
			    sftp.connect(TIMEOUT);
			} catch (JSchException e1) {
			    s.disconnect();
			    last = e1;
			    s = connectTo(userInfo, host);
			    continue;
			}
			
			ChannelSftp channelSftp = (ChannelSftp) sftp;
			
			return channelSftp;
		    }
		    throw new IOException(last);
		}

		@Override
		    protected URLConnection openConnection(URL u) throws IOException {
		    
		    ChannelSftp channelSftp = getChannel(u.getUserInfo(), u.getHost());
		    
		    return new URLConnection(u) {
			String getFileName(URL u){
			    String f= u.getFile();
			    int x= f.indexOf(';');
			    if(x!=-1)
				f=f.substring(0, x);
			    return f.substring(1);
			}
			
			// channelSftp.exit(); should be called, it is probably called by JSch
			
			@Override
			    public Object getContent() throws IOException {
			    InputStreamReader isReader = new InputStreamReader(this.getInputStream());
			    // Creating a BufferedReader object
			    BufferedReader reader = new BufferedReader(isReader);
			    StringBuffer sb = new StringBuffer();
			    String str;
			    while ((str = reader.readLine()) != null) {
				sb.append(str);
			    }
			    return sb.toString();
			}
			
			@Override
			    public InputStream getInputStream() throws IOException {
			    try {
				return channelSftp.get(getFileName(u));
			    } catch (SftpException e) {
				throw new IOException(e);
			    }
			}
			
			@Override
			    public OutputStream getOutputStream() throws IOException {
			    try {
				return channelSftp.put(getFileName(u));
			    } catch (SftpException e) {
				throw new IOException(e);
			    }
			}
			
			@Override
			    public void connect() throws IOException {
			    System.out.println("connecting");
			    
			}
			
		    };
		}
		
	    };
	}
	return null;
    }
    
};



class MyLoggr implements com.jcraft.jsch.Logger {
    static java.util.HashMap<Integer, String> name = new HashMap<Integer, String>();
    static {
        name.put(new Integer(DEBUG), "DEBUG: ");
        name.put(new Integer(INFO), "INFO: ");
        name.put(new Integer(WARN), "WARN: ");
        name.put(new Integer(ERROR), "ERROR: ");
        name.put(new Integer(FATAL), "FATAL: ");
    }

    public boolean isEnabled(int level) {
        return true;
    }

    public void log(int level, String message) {
        System.err.print(name.get(new Integer(level)));
        System.err.println(message);
    }
}

