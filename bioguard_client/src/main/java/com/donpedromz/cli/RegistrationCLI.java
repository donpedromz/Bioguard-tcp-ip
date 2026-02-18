package com.donpedromz.cli;

import com.donpedromz.domain.diagnostic.DiagnosticRegistration;
import com.donpedromz.domain.disease.DiseaseRegistration;
import com.donpedromz.domain.patient.PatientRegistration;
import com.donpedromz.fasta.file.FileScanner;
import com.donpedromz.fasta.file.ScanItem;
import com.donpedromz.fasta.file.FastaScanException;
import com.donpedromz.network.TCPClientException;
import com.donpedromz.service.RegistrationClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @version 1.0
 * @author juanp
 * Interfaz de línea de comandos para registrar pacientes, enfermedades y generar diagnósticos en el sistema BioGuard.
 */
public class RegistrationCLI {
    /**
     * Cliente utilizado para comunicarse con el servidor BioGuard y realizar las operaciones de registro y diagnóstico.
     */
    private final RegistrationClient registrationClient;

    /**
     * Scanner que localiza archivos FASTA válidos para el registro de enfermedades.
     * Cada archivo debe contener la información necesaria para crear una entidad DiseaseRegistration.
     */
    private final FileScanner<DiseaseRegistration> diseaseScanner;
    /**
     * Scanner que localiza archivos FASTA válidos para la generación de diagnósticos.
     */
    private final FileScanner<DiagnosticRegistration> diagnosticScanner;

    /**
     * Constructor de la CLI de registro.
     * @param registrationClient cliente utilizado para realizar las operaciones de registro
     *                           y diagnóstico en el servidor BioGuard
     * @param diseaseScanner scanner que localiza archivos FASTA válidos para el registro de enfermedades.
     * @param diagnosticScanner scanner que localiza archivos FASTA válidos para la generación de diagnósticos.
     */
    public RegistrationCLI(
            RegistrationClient registrationClient,
            FileScanner<DiseaseRegistration> diseaseScanner,
            FileScanner<DiagnosticRegistration> diagnosticScanner
    ) {
        this.registrationClient = registrationClient;
        this.diseaseScanner = diseaseScanner;
        this.diagnosticScanner = diagnosticScanner;
    }

    /**
     * Inicia el flujo de la CLI, mostrando un menú de opciones y procesando la entrada del
     * usuario para registrar pacientes,
     * registrar enfermedades o generar diagnósticos.
     * El método se ejecuta en un bucle hasta que el usuario decide salir.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> registerPatient(scanner);
                case "2" -> registerVirus(scanner);
                case "3" -> generateDiagnostic(scanner);
                case "0" -> {
                    System.out.println("Finalizando cliente. ¡Hasta pronto!");
                    running = false;
                }
                default -> System.out.println("Opción inválida, intenta nuevamente.");
            }
        }
    }

    /**
     * Inicia el flujo para generar un diagnóstico a partir de una muestra FASTA.
     * @param scanner scanner utilizado para leer la entrada del usuario, incluyendo la ruta de la muestra FASTA y
     *                la selección de la muestra si se encuentran múltiples archivos válidos.
     */
    private void generateDiagnostic(Scanner scanner) {
        try {
            String inputPath = prompt(scanner, "Ruta del archivo o carpeta con muestras FASTA");
            List<ScanItem<DiagnosticRegistration>> candidates = diagnosticScanner.scan(inputPath);
            if (candidates.isEmpty()) {
                System.out.println("No se encontraron muestras FASTA válidas en la ruta proporcionada.");
                return;
            }

            List<String> labels = new ArrayList<>();
            for (ScanItem<DiagnosticRegistration> c : candidates) {
                labels.add(c.getFileName());
            }
            int index = selectFromCandidates(scanner, labels,
                    "Se encontraron múltiples muestras FASTA válidas. Selecciona una:");
            ScanItem<DiagnosticRegistration> candidate = candidates.get(index);
            String response = registrationClient.generateDiagnostic(candidate.getPayload());
            System.out.println("Respuesta del servidor: " + response);
        } catch (FastaScanException | IllegalArgumentException | TCPClientException ex) {
            System.out.println("No fue posible generar el diagnóstico: " + ex.getMessage());
        }
    }

    /**
     * Inicia el flujo para registrar un nuevo paciente en el sistema BioGuard,
     * solicitando al usuario que ingrese los datos necesarios.
     * @param scanner scanner utilizado para leer la entrada del usuario,
     *                incluyendo los datos del paciente como documento, nombre, edad, etc.
     */
    private void registerPatient(Scanner scanner) {
        try {
            PatientRegistration patient = new PatientRegistration(
                    prompt(scanner, "Documento"),
                    prompt(scanner, "Nombre"),
                    prompt(scanner, "Apellido"),
                    readInt(scanner, "Edad"),
                    prompt(scanner, "Correo electrónico"),
                    prompt(scanner, "Género"),
                    prompt(scanner, "Ciudad"),
                    prompt(scanner, "País")
            );
            String response = registrationClient.registerPatient(patient);
            System.out.println("Respuesta del servidor: " + response);
        } catch (IllegalArgumentException | TCPClientException ex) {
            System.out.println("No fue posible registrar al paciente: " + ex.getMessage());
        }
    }

    /**
     * Inicia el flujo para registrar una nueva enfermedad en el sistema BioGuard a partir de un archivo FASTA.
     * @param scanner scanner utilizado para leer la entrada del usuario,
     *                incluyendo la ruta del archivo o carpeta con archivos FASTA,
     */
    private void registerVirus(Scanner scanner) {
        try {
            String inputPath = prompt(scanner, "Ruta del archivo o carpeta con archivos FASTA");
            List<ScanItem<DiseaseRegistration>> candidates = diseaseScanner.scan(inputPath);
            if (candidates.isEmpty()) {
                System.out.println("No se encontraron archivos FASTA válidos en la ruta proporcionada.");
                return;
            }

            List<String> labels = new ArrayList<>();
            for (ScanItem<DiseaseRegistration> c : candidates) {
                labels.add(c.getFileName() + " (" + c.getPayload().getInfectiousnessLevel() + ")");
            }
            int index = selectFromCandidates(scanner, labels,
                    "Se encontraron múltiples archivos FASTA válidos. Selecciona uno:");
            ScanItem<DiseaseRegistration> candidate = candidates.get(index);
            String response = registrationClient.registerDisease(candidate.getPayload());
            System.out.println("Respuesta del servidor: " + response);
        } catch (FastaScanException | IllegalArgumentException | TCPClientException ex) {
            System.out.println("No fue posible registrar la enfermedad: " + ex.getMessage());
        }
    }

    /**
     * Muestra una lista numerada de opciones y solicita al usuario que seleccione una.
     * Si solo hay un elemento, lo selecciona automáticamente.
     * @param scanner scanner utilizado para leer la entrada del usuario
     * @param displayLabels etiquetas descriptivas para cada opción, mostradas al usuario
     * @param headerMessage mensaje de encabezado que se muestra antes de las opciones
     * @return índice (base 0) del elemento seleccionado
     */
    private int selectFromCandidates(Scanner scanner, List<String> displayLabels, String headerMessage) {
        if (displayLabels.size() == 1) {
            return 0;
        }
        System.out.println(headerMessage);
        for (int i = 0; i < displayLabels.size(); i++) {
            System.out.printf("  %d - %s%n", i + 1, displayLabels.get(i));
        }
        while (true) {
            String value = prompt(scanner, "Número de archivo (1-" + displayLabels.size() + ")");
            try {
                int option = Integer.parseInt(value);
                if (option >= 1 && option <= displayLabels.size()) {
                    return option - 1;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Opción inválida, intenta nuevamente.");
        }
    }

    /**
     * Prompts the user for a value and returns the trimmed input.
     *
     * @param scanner scanner used to read user input
     * @param label label displayed to the user
     * @return the user input without leading or trailing whitespace
     */
    private String prompt(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    /**
     * Repeatedly reads an integer value until the user provides a valid number.
     *
     * @param scanner scanner used to read user input
     * @param label label displayed to the user
     * @return the parsed integer provided by the user
     */
    private int readInt(Scanner scanner, String label) {
        while (true) {
            String value = prompt(scanner, label);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                System.out.println("Debe ingresar un número válido. Intenta nuevamente.");
            }
        }
    }

    /**
     * Prints the CLI menu.
     */
    private void printMenu() {
        System.out.println();
        System.out.println("Selecciona una opción:");
        System.out.println("  1 - Registrar paciente");
        System.out.println("  2 - Registrar virus (archivo FASTA)");
        System.out.println("  3 - Generar diagnóstico (muestra FASTA)");
        System.out.println("  0 - Salir");
        System.out.print("Opción: ");
    }
}
