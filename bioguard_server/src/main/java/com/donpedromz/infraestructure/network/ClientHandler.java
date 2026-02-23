package com.donpedromz.infraestructure.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import com.donpedromz.infraestructure.network.data.Response;
import com.donpedromz.infraestructure.network.routing.MessageRouter;

/**
 * @author juanp
 * @version 1.0
 * Maneja la comunicación con un cliente TCP, aplicando una política de
 * procesamiento a los mensajes recibidos y enviando respuestas.
 */
public class ClientHandler implements Runnable {
    /**
     * Socket del cliente conectado. Se utiliza para leer mensajes y enviar respuestas.
     */
    private final Socket clientSocket;
    /**
     * Política de procesamiento que se aplica a los mensajes recibidos del cliente.
     */
    private final MessageRouter router;

    /**
     * Crea un nuevo manejador de cliente con el socket y la política de procesamiento especificados.
     * @param clientSocket El socket del cliente conectado. No debe ser null.
     * @param router El router de mensajes que se aplicará a los mensajes recibidos. No debe ser null.
     */
    public ClientHandler(Socket clientSocket, MessageRouter router) {
        this.clientSocket = clientSocket;
        this.router = router;
    }

    /**
     * Ejecuta el manejador de cliente, leyendo un mensaje del cliente, aplicando la política de procesamiento
     */
    @Override
    public void run() {
        try (
        DataInputStream input = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())){
            String receivedMessage = input.readUTF();
            System.out.println("[TCP] Received from client: " + receivedMessage);
            Response response;
            try {
                response = router.dispatchMessage(receivedMessage);
                if (response == null) {
                    response = new Response.Builder()
                            .statusCode(500)
                            .message("[TCP][500][InternalError] Error interno del servidor")
                            .build();
                }
            } catch (Exception e) {
                System.out.println("[SERVER][UnhandledException] " + e.getMessage());
                response = new Response.Builder()
                        .statusCode(500)
                        .message("[TCP][500][InternalError] Error interno del servidor")
                        .build();
            }
            out.writeUTF(response.toString());
            out.flush();
        } catch (EOFException | SocketException e) {
            System.out.println("[TCP] Client connection closed");
        } catch (IOException e) {
            System.out.println("[TCP] Error handling client connection");
            throw new RuntimeException(e);
        }
    }
}
