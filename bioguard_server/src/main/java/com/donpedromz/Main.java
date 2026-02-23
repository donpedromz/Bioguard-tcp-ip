package com.donpedromz;
import com.donpedromz.controllers.DiagnoseController;
import com.donpedromz.controllers.DiseaseRegisterController;

import com.donpedromz.controllers.PatientRegisterController;import com.donpedromz.format.dto.DiagnoseMessageDto;
import com.donpedromz.format.factory.DiagnosticParserFactory;
import com.donpedromz.format.factory.DiseaseParserFactory;
import com.donpedromz.format.factory.ParserFactory;
import com.donpedromz.format.factory.PatientParserFactory;
import com.donpedromz.controllers.IMessageProcessor;
import com.donpedromz.service.DiagnoseService;
import com.donpedromz.service.DiseaseService;
import com.donpedromz.service.IDiagnoseService;
import com.donpedromz.service.IDiseaseService;
import com.donpedromz.service.IPatientService;
import com.donpedromz.service.PatientService;
import com.donpedromz.common.IConfigReader;
import com.donpedromz.common.PropertiesManager;
import com.donpedromz.infraestructure.integrity.IntegrityVerifier;
import com.donpedromz.infraestructure.integrity.SHA256IntegrityVerifier;
import com.donpedromz.infraestructure.persistence.csv.CSVHighInfectivityPatientReportRepository;
import com.donpedromz.infraestructure.persistence.csv.CSVPatientDiagnosticHistoryRepository;
import com.donpedromz.infraestructure.persistence.csv.CSVDiagnosticRepository;
import com.donpedromz.repositories.diagnostic.IDiagnosticHistoryRepository;
import com.donpedromz.repositories.diagnostic.IHighInfectivityPatientReportRepository;
import com.donpedromz.repositories.diagnostic.IDiagnosticRepository;
import com.donpedromz.repositories.diagnostic.properties.IDiagnosticStorageConfig;
import com.donpedromz.infraestructure.persistence.config.CSVDiagnosticStorageConfig;
import com.donpedromz.infraestructure.persistence.fasta.FastaDiseaseRepository;
import com.donpedromz.repositories.disease.properties.IDiseaseStorageConfig;
import com.donpedromz.repositories.disease.IDiseaseRepository;
import com.donpedromz.infraestructure.persistence.config.DiseaseFastaStorageConfig;
import com.donpedromz.infraestructure.persistence.csv.CSVPatientRepository;
import com.donpedromz.repositories.patient.properties.IPatientStorageConfig;
import com.donpedromz.repositories.patient.IPatientRepository;
import com.donpedromz.infraestructure.persistence.config.CSVPatientStorageConfig;
import com.donpedromz.model.Disease;
import com.donpedromz.model.Patient;
import com.donpedromz.infraestructure.network.INetworkService;
import com.donpedromz.infraestructure.network.ISSLConfig;
import com.donpedromz.infraestructure.network.SSLTCPServer;
import com.donpedromz.infraestructure.network.TCPConfig;
import com.donpedromz.infraestructure.network.routing.FastaRouter;
import com.donpedromz.infraestructure.network.routing.MessageRouter;

import java.util.HashMap;

/**
 * @author juanp
 * @version 2.0
 * Clase principal que inicia la aplicación BioGuard.
 * Configura la inyección de dependencias siguiendo el patrón MVC:
 * Repositories → Services → Controllers → Router → Server.
 */
public class Main {
    public static void main(String[] args) {
        /**
         * Configuración de dependencias y arranque del servidor .
          * Se configuran los repositorios de pacientes, enfermedades y diagnósticos,
         */
        IConfigReader configReader = new PropertiesManager("application.properties");
        IPatientStorageConfig csvStorageConfig = new CSVPatientStorageConfig(configReader);
        IDiseaseStorageConfig diseaseFastaStorageConfig = new DiseaseFastaStorageConfig(configReader);
        IDiagnosticStorageConfig diagnosticStorageConfig = new CSVDiagnosticStorageConfig(configReader);
        ISSLConfig sslConfig = new TCPConfig(configReader);
        IntegrityVerifier integrityVerifier = new SHA256IntegrityVerifier();
        /**
         * Repositorios de datos, con sus respectivas configuraciones y dependencias inyectadas.
         */
        IPatientRepository patientRepository = new CSVPatientRepository(csvStorageConfig);
        IDiseaseRepository diseaseRepository = new FastaDiseaseRepository(diseaseFastaStorageConfig, integrityVerifier);
        IDiagnosticRepository diagnosticRepository = new CSVDiagnosticRepository(diagnosticStorageConfig, integrityVerifier);
        IHighInfectivityPatientReportRepository highInfectivityReportRepository =
                new CSVHighInfectivityPatientReportRepository(diagnosticStorageConfig);
        IDiagnosticHistoryRepository diagnosticHistoryRepository =
                new CSVPatientDiagnosticHistoryRepository(diagnosticStorageConfig, integrityVerifier);
        /**
         * Servicios de negocio, con los repositorios inyectados. Estos servicios implementan la lógica de negocio
         */
        IDiagnoseService diagnoseService = new DiagnoseService(
                patientRepository,
                diseaseRepository,
                diagnosticRepository,
                highInfectivityReportRepository,
                diagnosticHistoryRepository
        );
        IPatientService patientService = new PatientService(patientRepository);
        IDiseaseService diseaseService = new DiseaseService(diseaseRepository);
        /**
         * Factories de parsers para convertir el cuerpo de las solicitudes en objetos de dominio. 
         * Estos parsers se inyectan en los controladores para separar la lógica de parsing de la lógica de negocio.
         */
        ParserFactory<DiagnoseMessageDto> diagnosticParserFactory = new DiagnosticParserFactory();
        ParserFactory<Patient> patientParserFactory = new PatientParserFactory();
        ParserFactory<Disease> diseaseParserFactory = new DiseaseParserFactory();
        /**
         * Controladores que procesan las solicitudes entrantes, 
         * delegando la lógica de negocio a los servicios y utilizando los parsers para interpretar el contenido de las solicitudes.
         */
        IMessageProcessor diagnoseController = new DiagnoseController(diagnoseService, diagnosticParserFactory);
        IMessageProcessor patientController = new PatientRegisterController(patientService, patientParserFactory);
        IMessageProcessor diseaseController = new DiseaseRegisterController(diseaseService, diseaseParserFactory);
        /**
         * Router de mensajes que enruta las solicitudes entrantes a los controladores 
         * correspondientes según el tipo de solicitud y acción.
         */
        HashMap<String, IMessageProcessor> routingTable = new HashMap<>();
        routingTable.put("POST:diagnose", diagnoseController);
        routingTable.put("POST:patient", patientController);
        routingTable.put("POST:disease", diseaseController);
        MessageRouter router = new FastaRouter(routingTable);
        /**
         * Servidor TCP con SSL que escucha las solicitudes entrantes, las enruta a través del router de mensajes
         * y devuelve las respuestas generadas por los controladores. El servidor se inicia y queda a la espera de conexiones.
         */
        INetworkService server = new SSLTCPServer(sslConfig, router);
        server.start();
    }
}
