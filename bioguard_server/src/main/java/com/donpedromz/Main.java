package com.donpedromz;

import com.donpedromz.business.FASTA.FASTADiagnose;
import com.donpedromz.business.FASTA.FASTADiseaseRegister;
import com.donpedromz.business.FASTA.FASTAPatientRegister;
import com.donpedromz.business.FASTA.FASTAProcessor;
import com.donpedromz.business.IMessageProcessor;
import com.donpedromz.business.ProcessingPolicy;
import com.donpedromz.common.IConfigReader;
import com.donpedromz.common.PropertiesManager;
import com.donpedromz.data.IntegrityVerifier;
import com.donpedromz.data.SHA256IntegrityVerifier;
import com.donpedromz.data.diagnostic.CSVHighInfectivityPatientReportRepository;
import com.donpedromz.data.diagnostic.CSVPatientDiagnosticHistoryRepository;
import com.donpedromz.data.diagnostic.CSVDiagnosticRepository;
import com.donpedromz.data.diagnostic.IDiagnosticHistoryRepository;
import com.donpedromz.data.diagnostic.IHighInfectivityPatientReportRepository;
import com.donpedromz.data.diagnostic.IDiagnosticRepository;
import com.donpedromz.data.diagnostic.IDiagnosticStorageConfig;
import com.donpedromz.data.diagnostic.properties.DiagnosticStorageConfig;
import com.donpedromz.data.disease.FastaDiseaseRepository;
import com.donpedromz.data.disease.IDiseaseFastaStorageConfig;
import com.donpedromz.data.disease.IDiseaseRepository;
import com.donpedromz.data.disease.properties.DiseaseFastaStorageConfig;
import com.donpedromz.data.patient.CSVPatientRepository;
import com.donpedromz.data.patient.ICsvStorageConfig;
import com.donpedromz.data.patient.IPatientRepository;
import com.donpedromz.data.patient.properties.CsvStorageConfig;
import com.donpedromz.network.INetworkService;
import com.donpedromz.network.ISSLConfig;
import com.donpedromz.network.SSLTCPServer;
import com.donpedromz.network.TCPConfig;

import java.util.List;

/**
 * @author juanp
 * @version 1.0
 * Clase principal que inicia la aplicación BioGuard. Configura las dependencias necesarias,
 * como los repositorios de datos, los procesadores de mensajes y el servicio de red,
 * y luego inicia el servidor TCP con SSL para escuchar las solicitudes entrantes.
 */
public class Main {
    public static void main(String[] args) {
        IConfigReader configReader = new PropertiesManager("application.properties");
        ICsvStorageConfig csvStorageConfig = new CsvStorageConfig(configReader);
        IDiseaseFastaStorageConfig diseaseFastaStorageConfig = new DiseaseFastaStorageConfig(configReader);
        IDiagnosticStorageConfig diagnosticStorageConfig = new DiagnosticStorageConfig(configReader);
        ISSLConfig sslConfig = new TCPConfig(configReader);
        IntegrityVerifier integrityVerifier = new SHA256IntegrityVerifier();
        ProcessingPolicy policy = getPolicy(csvStorageConfig, diseaseFastaStorageConfig, diagnosticStorageConfig, integrityVerifier);
        INetworkService server = new SSLTCPServer(sslConfig, policy);
        server.start();
    }

    /**
     * Construye la política de procesamiento para la aplicación, creando los repositorios de datos necesarios
     * @param csvStorageConfig Configuración para el almacenamiento de datos de pacientes en formato CSV
     * @param diseaseFastaStorageConfig Configuración para el almacenamiento de datos de enfermedades en formato FASTA
     * @param diagnosticStorageConfig Configuración para el almacenamiento de datos de diagnósticos en formato CSV
     * @return Una instancia de ProcessingPolicy que
     * contiene los procesadores de mensajes configurados para manejar las solicitudes entrantes.
     */
    private static ProcessingPolicy getPolicy(
            ICsvStorageConfig csvStorageConfig,
            IDiseaseFastaStorageConfig diseaseFastaStorageConfig,
            IDiagnosticStorageConfig diagnosticStorageConfig,
            IntegrityVerifier integrityVerifier
    ) {
        IDiseaseRepository diseaseRepository = new FastaDiseaseRepository(diseaseFastaStorageConfig, integrityVerifier);
        IPatientRepository patientRepository = new CSVPatientRepository(csvStorageConfig);
        IMessageProcessor diagnoseProcessor = getProcessor(diagnosticStorageConfig, patientRepository, diseaseRepository, integrityVerifier);
        IMessageProcessor patientRegisterProcessor = new FASTAPatientRegister(patientRepository);
        IMessageProcessor diseaseRegisterProcessor = new FASTADiseaseRegister(diseaseRepository);
        return new FASTAProcessor(
            List.of(diagnoseProcessor, patientRegisterProcessor, diseaseRegisterProcessor)
        );
    }

    /**
     * Construye el procesador de mensajes para manejar las solicitudes de diagnóstico,
     * creando los repositorios de datos necesarios
     * @param diagnosticStorageConfig Configuración para el almacenamiento de datos de diagnósticos en formato CSV,
     *                                que se utiliza para crear los repositorios relacionados con los diagnósticos.
     * @param patientRepository Repositorio de pacientes que se utiliza para acceder
     *                          a la información de los pacientes durante el proceso de diagnóstico.
     * @param diseaseRepository Repositorio de enfermedades que se utiliza para acceder a
     *                          la información de las enfermedades durante el proceso de diagnóstico.
     * @return Una instancia de IMessageProcessor que contiene
     * el procesador de mensajes configurado para manejar las solicitudes de diagnóstico,
     */
    private static IMessageProcessor getProcessor(IDiagnosticStorageConfig diagnosticStorageConfig, IPatientRepository patientRepository, IDiseaseRepository diseaseRepository, IntegrityVerifier integrityVerifier) {
        IDiagnosticRepository diagnosticRepository = new CSVDiagnosticRepository(diagnosticStorageConfig, integrityVerifier);
        IHighInfectivityPatientReportRepository highInfectivityPatientReportRepository =
            new CSVHighInfectivityPatientReportRepository(diagnosticStorageConfig);
        IDiagnosticHistoryRepository diagnosticHistoryRepository =
            new CSVPatientDiagnosticHistoryRepository(diagnosticStorageConfig, integrityVerifier);
        return
                new FASTADiagnose(
                patientRepository,
                diseaseRepository,
            diagnosticRepository,
            highInfectivityPatientReportRepository,
            diagnosticHistoryRepository
        );
    }
}
