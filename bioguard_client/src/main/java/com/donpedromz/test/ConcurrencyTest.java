package com.donpedromz.test;

import com.donpedromz.common.IConfigReader;
import com.donpedromz.common.PropertiesManager;
import com.donpedromz.network.RequestBuilder;
import com.donpedromz.network.config.SSLConfig;
import com.donpedromz.network.config.ISSLConfig;
import com.donpedromz.network.SSLTCPClient;
import com.donpedromz.network.TCPClient;
/**
 * @version 1.0
 * @author juanp
 * Clase de prueba para evaluar la concurrencia en el cliente TCP al enviar múltiples solicitudes simultáneas al servidor.
 * Simula el registro concurrente de pacientes, enfermedades y diagnósticos utilizando hilos.
 */
public class ConcurrencyTest {
    private static final int THREAD_COUNT = 10;
    private static final String FASTA_CONTENT_TYPE = "application/fasta";

    private static final String PATIENT_MESSAGE = new RequestBuilder()
            .method("POST")
            .action("patient")
            .contentType(FASTA_CONTENT_TYPE)
            .body(">99887766|Carlos|Ramirez|28|carlos@test.com|MASCULINO|Bogota|Colombia")
            .build();

    private static final String DISEASE_MESSAGE = new RequestBuilder()
            .method("POST")
            .action("disease")
            .contentType(FASTA_CONTENT_TYPE)
            .body(">VirusConcurrencia|ALTA\nACGTACGTACGTACGTACGTACGTACGTACGT")
            .build();

    private static final String DIAGNOSTIC_MESSAGE = new RequestBuilder()
            .method("POST")
            .action("diagnose")
            .contentType(FASTA_CONTENT_TYPE)
            .body(">99887766|2025-10-15\nACGTACGTACGT")
            .build();

    public static void main(String[] args) {
        IConfigReader configReader = new PropertiesManager("application.properties");
        ISSLConfig clientConfig = new SSLConfig(configReader);
        TCPClient tcpClient = new SSLTCPClient(clientConfig);
        System.out.println("Prueba de concurrencia con " + THREAD_COUNT + " hilos");
        System.out.println();
        System.out.println("--- Test 1: Registrar mismo paciente ---");
        runConcurrentTest(tcpClient, PATIENT_MESSAGE);
        System.out.println();
        System.out.println("--- Test 2: Registrar misma enfermedad ---");
        runConcurrentTest(tcpClient, DISEASE_MESSAGE);
        System.out.println();
        System.out.println("--- Test 3: Generar mismo diagnóstico ---");
        runConcurrentTest(tcpClient, DIAGNOSTIC_MESSAGE);
    }

    private static void runConcurrentTest(TCPClient tcpClient, String message) {
        Thread[] threads = new Thread[THREAD_COUNT];
        String[] results = new String[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            int index = i;
            threads[i] = new Thread(() -> {
                try {
                    results[index] = tcpClient.send(message);
                } catch (Exception exception) {
                    results[index] = "[ERROR] " + exception.getMessage();
                }
            }, "BioGuard-Test-" + (i + 1));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            System.out.println("  Hilo " + (i + 1) + ": " + results[i]);
        }
    }
}
