The problem
------------
The sftp protocol is not supported by default by java.net.URL, you will run into a MalformedURLException. The ftp protocol is supported but that is obsolete.

sftp is supported by the [Jsch library ](https://github.com/mwiede/jsch) but you cannot to sftp via URLs as you could do with ftp:// URLs and grabbing input and output streams, without caring how the protocol is actually implemented. 

Jsch is also not so easy to work with, and a few of its issues (like "channel was closed") affect [apache vsf2](https://commons.apache.org/proper/commons-vfs/)

It is also hard to plug in new URL handlers in a servlet container like Tomcat.  The JVM does not allow adding multiple factories, and tomcat already defines one. Besides Jsch starts a thread for each SFTP session, and new threads are not trivial to manage in reloadable web apps, as the original classloader is lost.

Solution
--------
To support sftp:// URL connections for both InputStreams and OutputStreams in your JVM:

```
  java.net.URL.setURLStreamHandlerFactory(new org.makumba.SftpURLStreamHandlerFactory())
```

Tomcat use
----------
If you are in a Tomcat servlet/JSP environment, use a `TomcatSftpURLInstaller` to install this handlder factory in the Tomcat JVM. It can be configured:
- in `conf/server.xml` inside an `<Engine>` tag
- in your webapp configuration `META-INF/context.xml`  inside a `<Context >` tag.

```
<Listener className="org.makumba.TomcatSftpURLInstaller" />
```

In both cases, it is recommended that the JAR dependencies (sftp-uril.jar and jsch.jar) are placed in the tomcat `lib/` folder beacause most Jsch objects are hanged in a thread (one for each SFTP Session) and, in case of webapp reload, that thread will lose the classes for the objects that it works with.
