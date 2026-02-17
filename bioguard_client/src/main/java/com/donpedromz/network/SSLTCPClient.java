package com.donpedromz.network;

import com.donpedromz.config.ISSLConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Objects;


/**
 * @version 1.0
 * @author juanp
 * Establece conexion SSL con el servidor TCP de BioGuard utilizando un trust-store configurado.
 */
public class SSLTCPClient implements TCPClient {
    /**
     * Configuración SSL que proporciona los detalles necesarios para establecer la conexión segura,
     */
    private final ISSLConfig config;
    /**
     * Fábrica de sockets SSL configurada con el trust-store especificado en la configuración,
     */
    private final SSLSocketFactory socketFactory;

    /**
     * Constructor que inicializa el cliente SSL TCP con la configuración proporcionada y prepara la fábrica de sockets SSL.
     * @param config configuración SSL que incluye host, puerto, ruta al trust-store y contraseña del trust-store,
     */
    public SSLTCPClient(ISSLConfig config) {
        this.config = config;
        this.socketFactory = createSocketFactory();
    }

    /**
     * {@inheritDoc}
     * @param payload message to transmit
     * @return
     */
    @Override
    public String send(String payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        try (SSLSocket socket = (SSLSocket) socketFactory.createSocket(config.getHost(), config.getPort());
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {

            outputStream.writeUTF(payload);
            outputStream.flush();
            return inputStream.readUTF();
        } catch (IOException e) {
            throw new TCPClientException("Error communicating with TCP server", e);
        }
    }

    /**
     * Crea una fábrica de sockets SSL configurada con el trust-store especificado en la configuración.
     * @return SSLSocketFactory configurada para establecer conexiones SSL con el servidor TCP de BioGuard.
     */
    private SSLSocketFactory createSocketFactory() {
        try {
            char[] password = config.getTrustStorePassword().toCharArray();
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream inputStream = openTrustStoreStream()) {
                trustStore.load(inputStream, password);
            }
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new TCPClientException("Unable to initialize SSL context", e);
        }
    }

    /**
     * Abre un InputStream para el trust-store especificado en la configuración,
     * primero intentando cargarlo como un recurso de clase y, si no se encuentra,
     * intentándolo como un archivo del sistema.
     * @return InputStream para el trust-store, listo para ser cargado en el KeyStore.
     * @throws IOException si el trust-store no se encuentra en ninguna de las
     * ubicaciones esperadas o si ocurre un error al abrirlo.
     */
    private InputStream openTrustStoreStream() throws IOException {
        InputStream classpathResource = getClass()
                .getClassLoader()
                .getResourceAsStream(config.getTrustStorePath());
        if (classpathResource != null) {
            return classpathResource;
        }
        Path fileSystemPath = Paths.get(config.getTrustStorePath()).toAbsolutePath();
        if (!Files.exists(fileSystemPath)) {
            throw new IOException("TrustStore file not found at path: " + fileSystemPath);
        }
        return Files.newInputStream(fileSystemPath);
    }
}
