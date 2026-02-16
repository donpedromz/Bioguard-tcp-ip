package com.donpedromz.data.disease;

import com.donpedromz.data.IntegrityVerifier;
import com.donpedromz.data.FastaUtils;
import com.donpedromz.data.exceptions.ConflictException;
import com.donpedromz.data.exceptions.CorruptedDataException;
import com.donpedromz.data.exceptions.PersistenceException;
import com.donpedromz.data.exceptions.ValidationException;
import com.donpedromz.entities.Disease;
import com.donpedromz.entities.InfectiousnessLevel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * @author juanp
 * @version 1.0
 * Implementación de {@link IDiseaseRepository} que persiste enfermedades en archivos FASTA.
 */
public class FastaDiseaseRepository implements IDiseaseRepository {
    /**
     * Objeto de bloqueo para sincronizar el acceso al directorio de enfermedades y evitar condiciones de carrera
     */
    private static final Object DIRECTORY_LOCK = new Object();
    /**
     * Extensión de archivo utilizada para los archivos FASTA de enfermedades.
     * Se utiliza para validar y filtrar archivos en el directorio.
     */
    private static final String FASTA_EXTENSION = ".fasta";
    /**
     * Longitud mínima requerida para la secuencia genética de una enfermedad.
     */
    private static final int MIN_DISEASE_SEQUENCE_LENGTH = 15;

    /**
     * Valida nombres de enfermedades con letras (incluye acentos), números,
     * y separadores por espacio o guion entre bloques.
     */
    private static final String DISEASE_NAME_REGEX = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9]+(?:[ -][A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9]+)*$";
    /**
     * Valida secuencias genéticas compuestas exclusivamente por A, C, G y T.
     */
    private static final String GENETIC_SEQUENCE_REGEX = "^[ACGT]+$";
    /**
     * Ruta del directorio donde se almacenan los archivos FASTA de enfermedades.
     * Cada archivo representa una enfermedad registrada, con su nombre basado en el hash SHA-256
     * del contenido canónico para garantizar unicidad e integridad.
     */
    private final Path diseasesDirectory;
    /**
     * Verificador de integridad inyectado para cálculo de hashes y validación de archivos.
     */
    private final IntegrityVerifier integrityVerifier;

    /**
     * Crea el repositorio FASTA de enfermedades.
     *
     * @param diseasesDirectoryPath ruta del directorio donde se almacenarán archivos FASTA
     * @param integrityVerifier verificador de integridad para hashing y validación de archivos
     */
    public FastaDiseaseRepository(String diseasesDirectoryPath, IntegrityVerifier integrityVerifier) {
        if (diseasesDirectoryPath == null || diseasesDirectoryPath.isBlank()) {
            throw new ValidationException("La ruta del directorio de enfermedades es obligatoria.");
        }
        if (integrityVerifier == null) {
            throw new ValidationException("integrityVerifier no puede ser null");
        }
        this.diseasesDirectory = Paths.get(diseasesDirectoryPath).toAbsolutePath().normalize();
        this.integrityVerifier = integrityVerifier;
        initializeDirectory();
    }

    /**
     * Crea el repositorio FASTA de enfermedades a partir de configuración.
     *
     * @param storageConfig configuración de almacenamiento FASTA
     * @param integrityVerifier verificador de integridad para hashing y validación de archivos
     */
    public FastaDiseaseRepository(IDiseaseFastaStorageConfig storageConfig, IntegrityVerifier integrityVerifier) {
        this(requireDiseasesDirectory(storageConfig), integrityVerifier);
    }

    /**
     * Valida la configuración de almacenamiento y extrae la ruta del directorio de enfermedades.
     *
     * @param storageConfig configuración de almacenamiento FASTA a validar
     * @return ruta del directorio de enfermedades extraída de la configuración
     */
    private static String requireDiseasesDirectory(IDiseaseFastaStorageConfig storageConfig) {
        if (storageConfig == null) {
            throw new ValidationException("storageConfig no puede ser null");
        }
        return storageConfig.getDiseasesDirectory();
    }

    /**
     * Lista todas las enfermedades almacenadas en el directorio de archivos FASTA.
     *
     * @return lista de enfermedades registradas, o lista vacía si no hay archivos válidos o el directorio no existe
     */
    @Override
    public List<Disease> findAll() {
        synchronized (DIRECTORY_LOCK) {
            if (!Files.exists(diseasesDirectory) || !Files.isDirectory(diseasesDirectory)) {
                return List.of();
            }
            List<Disease> diseases = new ArrayList<>();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(diseasesDirectory)) {
                for (Path file : files) {
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }
                    String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!fileName.endsWith(FASTA_EXTENSION)) {
                        continue;
                    }
                    try {
                        Disease disease = mapFastaFileToDisease(file);
                        if (disease != null) {
                            diseases.add(disease);
                        }
                    } catch (CorruptedDataException corruptedException) {
                        System.out.println("[TCP][CorruptedData] " + corruptedException.getMessage());
                    }
                }
            } catch (IOException exception) {
                throw new PersistenceException("Error al listar enfermedades FASTA", exception);
            }
            return diseases;
        }
    }

    /**
     * Persiste una enfermedad en formato FASTA usando el hash SHA-256 del contenido
     * como identificador y nombre de archivo.
     *
     * @param entity enfermedad a registrar
     */
    @Override
    public void save(Disease entity) {
        synchronized (DIRECTORY_LOCK) {
            validateDiseaseForPersistence(entity);
            String canonicalSampleContent = buildCanonicalSampleContent(entity);
            String sampleHash = integrityVerifier.computeHash(canonicalSampleContent);
            ensureSampleHashIsUnique(sampleHash);
            String fastaContent = buildFastaContent(entity);
            Path targetFile = diseasesDirectory.resolve(buildFileName(sampleHash));

            try {
                Files.writeString(
                        targetFile,
                        fastaContent,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW
                );
            } catch (IOException exception) {
                throw new PersistenceException("Error al guardar el archivo FASTA de la enfermedad", exception);
            }
        }
    }

    /**
     * Inicializa el directorio de enfermedades e imprime información de arranque.
     */
    private void initializeDirectory() {
        synchronized (DIRECTORY_LOCK) {
            try {
                boolean directoryAlreadyExists = Files.exists(diseasesDirectory);
                Files.createDirectories(diseasesDirectory);
                printDirectoryStartupInfo(directoryAlreadyExists);
            } catch (IOException exception) {
                throw new PersistenceException("No fue posible preparar el directorio de enfermedades", exception);
            }
        }
    }

    /**
     * Cuenta los archivos FASTA válidos almacenados en el directorio.
     *
     * @return cantidad de enfermedades registradas
     * @throws IOException si ocurre un error al listar archivos
     */
    private long countRegisteredDiseases() throws IOException {
        long total = 0L;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(diseasesDirectory)) {
            for (Path file : files) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (fileName.endsWith(FASTA_EXTENSION)) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Verifica si ya existe una enfermedad registrada con el mismo hash canónico.
     * Este método recorre los archivos FASTA en el directorio, calcula el hash canónico de cada enfermedad almacenada
     * y lo compara con el hash proporcionado. Si encuentra una coincidencia, retorna {@code true},
     * indicando que ya existe una enfermedad con el mismo contenido.
     *
     * @param canonicalHash hash SHA-256 del contenido canónico de la enfermedad a verificar
     * @return {@code true} si ya existe una enfermedad con el mismo hash, {@code false} en caso contrario
     */
    private boolean existsCanonicalHash(String canonicalHash) {
        if (canonicalHash == null || canonicalHash.isBlank()) {
            return false;
        }
        if (!Files.exists(diseasesDirectory) || !Files.isDirectory(diseasesDirectory)) {
            return false;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(diseasesDirectory)) {
            for (Path file : files) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!fileName.endsWith(FASTA_EXTENSION)) {
                    continue;
                }
                Disease storedDisease;
                try {
                    storedDisease = mapFastaFileToDisease(file);
                } catch (CorruptedDataException corruptedException) {
                    System.out.println("[TCP][CorruptedData] " + corruptedException.getMessage());
                    continue;
                }
                if (storedDisease == null) {
                    continue;
                }
                String storedCanonicalContent = buildCanonicalSampleContent(storedDisease);
                String storedCanonicalHash = integrityVerifier.computeHash(storedCanonicalContent);
                if (canonicalHash.equals(storedCanonicalHash)) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new PersistenceException("Error al buscar enfermedad por hash", exception);
        }
    }

    /**
     * Valida y normaliza la enfermedad antes de persistirla.
     *
     * @param disease entidad a validar
     */
    private void validateDiseaseForPersistence(Disease disease) {
        if (disease == null) {
            throw new ValidationException("Disease no puede ser null");
        }
        UUID diseaseUuid = disease.getUuid();
        if (diseaseUuid == null) {
            diseaseUuid = UUID.randomUUID();
        }
        String diseaseName = FastaUtils.trimOrEmpty(disease.getDiseaseName());
        String infectiousness = FastaUtils.trimOrEmpty(disease.getInfectiousness());
        String geneticSequence = FastaUtils.trimOrEmpty(disease.getGeneticSequence());
        List<String> invalidFields = new ArrayList<>();
        if (diseaseName.isEmpty() || !diseaseName.matches(DISEASE_NAME_REGEX)) {
            invalidFields.add("diseaseName");
        }
        if (geneticSequence.isEmpty() || !geneticSequence.matches(GENETIC_SEQUENCE_REGEX)) {
            invalidFields.add("geneticSequence");
        } else if (geneticSequence.length() < MIN_DISEASE_SEQUENCE_LENGTH) {
            invalidFields.add("geneticSequence (mínimo " + MIN_DISEASE_SEQUENCE_LENGTH + " nucleótidos)");
        }
        if (infectiousness.isEmpty()) {
            invalidFields.add("infectiousness");
        }
        InfectiousnessLevel infectiousnessLevel = null;
        if (!infectiousness.isEmpty()) {
            try {
                infectiousnessLevel = InfectiousnessLevel.from(infectiousness);
            } catch (RuntimeException exception) {
                invalidFields.add("infectiousness");
            }
        }
        if (!invalidFields.isEmpty()) {
            throw new ValidationException("Campos inválidos: " + String.join(", ", invalidFields));
        }
        disease.setUuid(diseaseUuid);
        disease.setDiseaseName(diseaseName);
        disease.setInfectiousness(infectiousnessLevel.name());
        disease.setGeneticSequence(geneticSequence);
    }

    /**
     * Construye el contenido FASTA serializado para una enfermedad.
     *
     * @param disease enfermedad fuente
     * @return contenido en formato FASTA
     */
    private String buildFastaContent(Disease disease) {
        return ">" + disease.getUuid() + "|" + disease.getDiseaseName() + "|" + disease.getInfectiousness() +
                System.lineSeparator() +
                disease.getGeneticSequence();
    }

    /**
     * Construye el contenido canónico utilizado para calcular el hash de una enfermedad.
     * Este contenido se compone del nombre de la enfermedad, el nivel de infecciosidad y la secuencia genética,
     *
     * @param disease enfermedad fuente
     * @return contenido canónico para hash, sin incluir el UUID ni otros metadatos variables
     */
    private String buildCanonicalSampleContent(Disease disease) {
        return ">" + disease.getDiseaseName() + "|" + disease.getInfectiousness() +
                System.lineSeparator() +
                disease.getGeneticSequence();
    }

    /**
     * Mapea un archivo FASTA a una instancia de {@link Disease}. El método lee el contenido del archivo,
     * valida su formato y estructura, y extrae los campos necesarios para construir la enfermedad. Si el archivo
     * está corrupto, tiene un formato inválido o no cumple con los requisitos,
     * se lanza una excepción o se retorna {@code null}.
     *
     * @param filePath ruta del archivo FASTA a mapear
     * @return instancia de Disease representada por el archivo, o {@code null} si el archivo no es válido
     */
    private Disease mapFastaFileToDisease(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            if (lines.size() < 2) {
                return null;
            }
            String header = lines.get(0).trim();
            String sequence = lines.get(1).trim();
            if (!header.startsWith(">") || sequence.isBlank()) {
                return null;
            }
            String[] headerFields = header.substring(1).split("\\|");
            if (headerFields.length < 3) {
                return null;
            }
            UUID diseaseUuid = UUID.fromString(headerFields[0].trim());
            String diseaseName = headerFields[1].trim();
            String infectiousness = headerFields[2].trim();

            Disease disease = new Disease(diseaseUuid, diseaseName, infectiousness, sequence);
            verifyFileIntegrity(filePath, disease);
            return disease;
        } catch (CorruptedDataException corruptedException) {
            throw corruptedException;
        } catch (RuntimeException | IOException exception) {
            return null;
        }
    }

    /**
     * Verifica la integridad del archivo FASTA comparando el hash SHA-256 del contenido
     * canónico con el nombre del archivo.
     *
     * @param filePath ruta del archivo FASTA
     * @param disease  enfermedad parseada del archivo
     * @throws CorruptedDataException si el hash del contenido no coincide con el nombre del archivo
     */
    private void verifyFileIntegrity(Path filePath, Disease disease) {
        String fileName = filePath.getFileName().toString();
        String expectedHash = fileName.endsWith(FASTA_EXTENSION)
                ? fileName.substring(0, fileName.length() - FASTA_EXTENSION.length())
                : fileName;
        String canonicalContent = buildCanonicalSampleContent(disease);
        String actualHash = integrityVerifier.computeHash(canonicalContent);
        if (!expectedHash.equals(actualHash)) {
            throw new CorruptedDataException(
                    "Archivo FASTA corrupto o modificado: " + fileName + " en " + filePath.toAbsolutePath()
            );
        }
    }
    /**
     * Verifica que no exista una enfermedad previamente registrada con el mismo hash.
     * @param canonicalHash hash SHA-256 del contenido FASTA
     */
    private void ensureSampleHashIsUnique(String canonicalHash) {
        if (existsCanonicalHash(canonicalHash)) {
            throw new ConflictException("Ya existe una enfermedad registrada con el mismo contenido FASTA.");
        }
    }
    /**
     * Construye el nombre del archivo FASTA a partir del hash.
     * @param hash hash SHA-256 del contenido FASTA
     * @return nombre de archivo con extensión FASTA
     */
    private String buildFileName(String hash) {
        return hash + FASTA_EXTENSION;
    }

    /**
     * Imprime mensajes informativos del estado del directorio y cantidad de registros.
     * @param directoryAlreadyExists indica si el directorio ya existía antes de inicializar
     * @throws IOException si ocurre error al contar archivos
     */
    private void printDirectoryStartupInfo(boolean directoryAlreadyExists) throws IOException {
        if (directoryAlreadyExists) {
            System.out.println("[FASTA][DiseaseRepository] Directorio existente: " + diseasesDirectory);
        } else {
            System.out.println("[FASTA][DiseaseRepository] Directorio creado: " + diseasesDirectory);
        }
        long registeredDiseases = countRegisteredDiseases();
        if (registeredDiseases > 0) {
            System.out.println("[FASTA][DiseaseRepository] Enfermedades registradas: " + registeredDiseases);
        }
    }
}
