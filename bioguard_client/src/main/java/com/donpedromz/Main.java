package com.donpedromz;

import com.donpedromz.cli.RegistrationCLI;
import com.donpedromz.common.IConfigReader;
import com.donpedromz.common.PropertiesManager;
import com.donpedromz.network.config.SSLConfig;
import com.donpedromz.network.config.ISSLConfig;
import com.donpedromz.domain.diagnostic.DiagnosticRegistration;
import com.donpedromz.domain.disease.DiseaseRegistration;
import com.donpedromz.fasta.DiagnosticFastaMessageBuilder;
import com.donpedromz.fasta.DiseaseFastaMessageBuilder;
import com.donpedromz.fasta.PatientFastaMessageBuilder;
import com.donpedromz.fasta.file.DiagnosticScanner;
import com.donpedromz.fasta.file.DiseaseScanner;
import com.donpedromz.fasta.file.FileScanner;
import com.donpedromz.network.SSLTCPClient;
import com.donpedromz.network.TCPClient;
import com.donpedromz.service.RegistrationClient;
import com.donpedromz.service.TCPRegistrationClient;

/**
 * Boots the BioGuard SSL client application and wires its dependencies.
 *
 * @author @donpedromz
 */
public class Main {

    /**
     * Launches the CLI registration workflow.
     *
     * @param args optional CLI arguments (unused)
     */
    public static void main(String[] args) {
        IConfigReader configReader = new PropertiesManager("application.properties");
        ISSLConfig clientConfig = new SSLConfig(configReader);
        TCPClient tcpClient = new SSLTCPClient(clientConfig);
        RegistrationClient registrationClient = new TCPRegistrationClient(
                tcpClient,
                new PatientFastaMessageBuilder(),
                new DiseaseFastaMessageBuilder(),
                new DiagnosticFastaMessageBuilder()
        );

        FileScanner<DiseaseRegistration> diseaseScanner = new DiseaseScanner();
        FileScanner<DiagnosticRegistration> diagnosticScanner = new DiagnosticScanner();

        new RegistrationCLI(registrationClient, diseaseScanner, diagnosticScanner).run();
    }
}
