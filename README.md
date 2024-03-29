The problem
------------
The `sftp` protocol is not supported by default by `java.net.URL`, you will run into a `MalformedURLException`. The `ftp` protocol is (still) supported in some Java versions but that is obsolete. 

The goal of this package is to allow you to change your application (configuration) from `ftp://username:password@host/path/file`  to  `sftp://username:password@host/path/file` or to use certificate authentication instead of a password: `sftp://username@host/path/file`

`sftp` is supported by the [Jsch library ](https://github.com/mwiede/jsch) but you cannot do `sftp` via URLs, which "blocks" any existing code that used `ftp://` URLs and grabbed input and output streams, without caring how the protocol is actually implemented. 

Jsch is also not so easy to work with, and a few of its issues (like "channel was closed") affect [apache vsf2](https://commons.apache.org/proper/commons-vfs/) hence (my) attempts to implement a reliable `sftp` URL handler using `vfs2` failed.

It is also hard to plug in new URL stream handler factories in a servlet container like Tomcat.  The JVM does not allow adding multiple such factories, and tomcat already defines one. Even if tomcat allows plugging in more, there are more complications, as Jsch starts a thread for each SFTP session, and new threads are not trivial to manage in reloadable web apps, as the original classloader is lost.

Solution
--------
To support `sftp://` URL connections for both `InputStream`s and `OutputStream`s in your JVM:

```
  java.net.URL.setURLStreamHandlerFactory(new org.makumba.SftpURLStreamHandlerFactory())
```

Tomcat use
----------
If you are in a Tomcat servlet/JSP environment, use a `TomcatSftpURLInstaller` to plug in this `sftp` URL handlder factory in the Tomcat JVM. It can be configured:
- in `conf/server.xml` inside an `<Engine>` tag
- in your webapp configuration `META-INF/context.xml`  inside a `<Context >` tag.

```
<Listener className="org.makumba.TomcatSftpURLInstaller" />
```

In both cases, it is recommended that the JAR dependencies (this package: `sftp-url.jar` and `jsch-0.2.x.jar`) are placed in the tomcat `lib/` folder beacause most Jsch objects are hanged in a thread (one for each SFTP Session) and, in case of webapp reload, that thread will lose the classes for the objects that it works with. Using a [servlet context listener](https://docs.oracle.com/javaee/6/api/javax/servlet/ServletContextListener.html) to stop that thread did not yield good results, but you may want to contribute a solution. That would allow deployment on tomcats where the `lib/` folder is not accessible.
