package com.ims;

import com.ims.handlers.*;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;

public class ImsApiServer {
    private final Logger LOGGER = LoggerFactory.getLogger(ImsApiServer.class);
    private Undertow server;

    public static void main(final String[] args) {
        //Settings.getInstance().setSettingsPath(args[0]);
    	Settings.getInstance().setSettingsPath("ims.properties");
        ImsApiServer imsApiServer = new ImsApiServer();
        imsApiServer.buildAndStartServer(Integer.parseInt(Settings.getInstance().getProperty(Settings.PROPERTY_PORT)),
                Settings.getInstance().getProperty(Settings.PROPERTY_HOST));
    }


    public void buildAndStartServer(int port, String host) {
        server = Undertow.builder()
                .addListener(port, host)
                .setHandler(getWebSocketHandler())
                .build();
        server.start();
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    private PathHandler getWebSocketHandler() {
        return path()
                .addPrefixPath("/", resource(new ClassPathResourceManager(ImsApiServer.class.getClassLoader(), ImsApiServer.class.getPackage())).addWelcomeFiles("index.html"))
                //.addPrefixPath("/", resource(new FileResourceManager(new File("c:\\Java\\imsapi\\imsapi\\src\\main\\resources\\com\\ims\\"), 1)))
                //.addPrefixPath("/", resource(new FileResourceManager(new File("C:\\Users\\matthewWork\\workspace\\imsapi\\imsapi\\src\\main\\resources\\com\\ims\\"), 1)))

                .addPrefixPath("/api/v1/updates", new UpdatesHandler())
                .addPrefixPath("/api/v1/auth", new AuthHandler())
                .addPrefixPath("/api/v1/register", new RegistrationHandler())
                .addPrefixPath("/api/v1/files", new TokenCheckHandler(new FilesHandler()))
                .addPrefixPath("/api/v1/files/share", new TokenCheckHandler(new ShareHandler()))
                .addPrefixPath("/api/v1/user", new TokenCheckHandler(new UserHandler()))
                .addPrefixPath("/api/v1/graph", new TokenCheckHandler(new GraphHandler()))
                .addPrefixPath("/api/v1/statistics", new TokenCheckHandler(new StatisticsHandler()))
                ;
    }
}
