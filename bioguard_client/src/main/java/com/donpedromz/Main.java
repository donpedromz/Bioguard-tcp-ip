package com.donpedromz;

import com.donpedromz.cli.RegistrationCli;
import com.donpedromz.common.IConfigReader;
import com.donpedromz.common.PropertiesManager;
import com.donpedromz.config.ClientNetworkConfig;
import com.donpedromz.config.ISSLConfig;
import com.donpedromz.domain.disease.DiseaseRegistration;
import com.donpedromz.fasta.DiseaseFastaMessageBuilder;
import com.donpedromz.fasta.PatientFastaMessageBuilder;
import com.donpedromz.fasta.file.DiagnosticScanner;
import com.donpedromz.fasta.file.DiseaseScanner;
import com.donpedromz.fasta.file.FastaFileScanner;
import com.donpedromz.network.SSLTCPClient;
import com.donpedromz.network.TCPClient;
import com.donpedromz.service.RegistrationClient;
import com.donpedromz.service.TcpRegistrationClient;

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
        ISSLConfig clientConfig = new ClientNetworkConfig(configReader);
        TCPClient tcpClient = new SSLTCPClient(clientConfig);
        RegistrationClient registrationClient = new TcpRegistrationClient(
                tcpClient,
                new PatientFastaMessageBuilder(),
                new DiseaseFastaMessageBuilder()
        );

        FastaFileScanner<DiseaseRegistration> diseaseScanner = new DiseaseScanner();
        FastaFileScanner<String> diagnosticScanner = new DiagnosticScanner();

        new RegistrationCli(registrationClient, diseaseScanner, diagnosticScanner).run();
    }
}
