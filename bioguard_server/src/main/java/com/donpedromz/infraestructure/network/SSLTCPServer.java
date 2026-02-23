package com.donpedromz.infraestructure.network;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import com.donpedromz.infraestructure.network.routing.MessageRouter;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

/**
 * Servidor TCP que utiliza SSL para cifrar las comunicaciones.
 * Escucha en un puerto configurado, acepta conexiones entrantes
 */
public class SSLTCPServer implements INetworkService{
    /**
     * Configuración del servidor SSL, que incluye la ruta al keystore, la contraseña y el puerto de escucha.
     */
    private final ISSLConfig config;
    /**
     * Política de procesamiento que se aplica a los mensajes recibidos de los clientes.
     * Se utiliza para generar respuestas basadas en el contenido de los mensajes.
     */
    private final MessageRouter router;
    /**
     * Crea un nuevo servidor TCP con SSL utilizando la configuración y la política de procesamiento especificadas.
     * @param config Configuración del servidor SSL,
     *               que incluye la ruta al keystore, la contraseña y el puerto de escucha. No debe ser null.
     * @param router Router de mensajes que se aplicará a los mensajes recibidos de los clientes.
     *               No debe ser null.
     */
    public SSLTCPServer(ISSLConfig config, MessageRouter router) {
        this.config = config;
        this.router = router;
    }

    /**
     * Crea una fábrica de sockets SSL configurada con el keystore especificado en la configuración.
     * @return Una fábrica de sockets SSL lista para crear sockets de servidor SSL.
     * @throws Exception si ocurre un error al cargar el keystore o configurar el contexto SSL.
     */
    private SSLServerSocketFactory createSSLFactory() throws Exception{
        char[] pwd = config.getKeyStorePassword().toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(config.getKeyStorePath())){
            if(is == null){
                throw new Exception("KeyStore file not found: %s".formatted(config.getKeyStorePath()));
            }
            ks.load(is, pwd);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, pwd);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);
            return ctx.getServerSocketFactory();
        }
    }

    /**
     * Inicia el servidor TCP SSL, escuchando en el puerto configurado. Para cada conexión entrante,
     * se crea un nuevo hilo
     */
    @Override
    public void start(){
        try(ServerSocket serverSocket = createSSLFactory().createServerSocket(config.getPort())){
            System.out.println("[TCP] Server running listening on port: %s".formatted(config.getPort()));
            while(true){
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new ClientHandler(clientSocket, router));
                clientThread.start();
            }
        } catch (Exception e) {
            System.out.println("[TCP] Internal Critical Error");
            throw new RuntimeException(e);
        }
    }
}
