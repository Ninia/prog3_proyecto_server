package ud.binmonkey.prog3_proyecto_server.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.apache.ftpserver.ftplet.FtpException;
import org.w3c.dom.Element;
import ud.binmonkey.prog3_proyecto_server.common.DocumentReader;
import ud.binmonkey.prog3_proyecto_server.common.exceptions.AdminEditException;
import ud.binmonkey.prog3_proyecto_server.common.exceptions.InvalidNameException;
import ud.binmonkey.prog3_proyecto_server.common.exceptions.UserNotFoundException;
import ud.binmonkey.prog3_proyecto_server.common.network.URI;
import ud.binmonkey.prog3_proyecto_server.http.handlers.DefaultHandler;
import ud.binmonkey.prog3_proyecto_server.http.handlers.LoginHandler;
import ud.binmonkey.prog3_proyecto_server.http.handlers.WebHandlers;
import ud.binmonkey.prog3_proyecto_server.users.UserManager;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class HTTPSServer {

    private HttpsServer httpsServer;
    private HashMap<String, HttpHandler> contexts = new HashMap<String, HttpHandler>() {{
        put("/", new WebHandlers.IndexHandler());
        put("/login", new LoginHandler());

        /* extras */
        put("/antigravity", new WebHandlers.AntigravityHandler());
        put("/favicon.ico", new WebHandlers.FavIcoHandler());
        put("/images/", new WebHandlers.WebFileHandler());
        put("/index", new WebHandlers.IndexHandler());
        put("/js/", new WebHandlers.WebFileHandler());
        put("/test", new DefaultHandler());
        put("/vendor/", new WebHandlers.WebFileHandler());
    }};


    public HTTPSServer() throws IOException {

        try {

            /* obtain keyword from config xml*/
            Element settings = (Element) DocumentReader.getDoc(
                    "conf/Network.xml").getElementsByTagName("http-server").item(0);
            String keyword = settings.getElementsByTagName("keyword").item(0).getTextContent();

            /* initialize ssl context */
            SSLContext sslContext = SSLContext.getInstance("TLS");

            /* password from keyword */
            char[] passwd = keyword.toCharArray();

            /* configure key store */
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream("src/test/resources/keys/keystore.jks"), passwd);

            /* configure key management factory */
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, passwd);

            /* configure trust manager factory */
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            /* setup SSL context */
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            /* create http server */
            this.httpsServer = HttpsServer.create(new InetSocketAddress(URI.getHost("http-server"),
                    URI.getPort("http-server")), 0);

            System.out.println("Creating HTTPS server in " + URI.getURI("http-server"));

            /* set http configurator */
            this.httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters parameters) {
                    try {

                        /* initialize ssl context */
                        SSLContext sslC = SSLContext.getDefault();
                        SSLEngine sslEngine = sslC.createSSLEngine();
                        parameters.setNeedClientAuth(false);
                        parameters.setCipherSuites(sslEngine.getEnabledCipherSuites());
                        parameters.setProtocols(sslEngine.getEnabledProtocols());

                        /* set default parameters */
                        parameters.setSSLParameters(sslC.getDefaultSSLParameters());


                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            });

            for (String context : contexts.keySet()) {
                httpsServer.createContext(context, contexts.get(context));
            }

            /* set multithreaded executor */
            this.httpsServer.setExecutor(new ThreadPoolExecutor(4, 8, 30,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(100)));

        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException |
                CertificateException | KeyManagementException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        boolean createUsers = true;

        if (createUsers) {
            try {
                UserManager.main(null);
            } catch (UserNotFoundException | FtpException | InvalidNameException | IOException | AdminEditException e) {
                e.printStackTrace();
            }
        }

        try {
            HTTPSServer server = new HTTPSServer();
            server.httpsServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("Failed to create HTTPS server.");
        }

    }
}
