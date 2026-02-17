package com.donpedromz.fasta.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @version 1.0
 * @author juanp
 * Clase abstracta que proporciona la funcionalidad para escanear archivos FASTA desde una ruta dada,
 * @param <T> el tipo de entidad que se mapeará a partir del contenido de los archivos FASTA,
 */
public abstract class FastaFileScanner<T> {
    /**
     * Constante que define la extensión de los archivos FASTA, utilizada para filtrar los archivos a escanear.
     */
    private static final String FASTA_EXTENSION = ".fasta";
    /**
     * Expresión regular para validar fechas en formato YYYY-MM-DD,
     * utilizada en el escaneo de archivos FASTA de diagnóstico.
     */
    protected static final String DATE_FORMAT_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";

    /**
     * Convierte el contenido bruto de un archivo FASTA
     * en una lista de líneas, eliminando espacios en blanco y líneas vacías.
     * @param content raw file content
     * @return list of trimmed, non-empty lines
     * @throws FastaScanException if the content is null or blank
     */
    protected static List<String> getLines(String content) {
        if (content == null || content.isBlank()) {
            throw new FastaScanException("El archivo FASTA no puede estar vacío.");
        }
        List<String> lines = new ArrayList<>();
        String[] raw = content.trim().split("\\R");
        for (String line : raw) {
            if (line == null) {
                continue;
            }
            String normalized = line.trim();
            if (!normalized.isEmpty()) {
                lines.add(normalized);
            }
        }
        return lines;
    }

    /**
     * Convierte el encabezado de un archivo FASTA en un array de campos, validando que el formato sea correcto
     *
     * @param header     the first line of the FASTA file
     * @param formatHint human-readable format hint included in error messages (e.g. {@code ">nombre|nivel"})
     * @return array of trimmed header fields (without the {@code >} prefix)
     * @throws FastaScanException if the header format is invalid
     */
    protected static String[] parseHeader(String header, String formatHint) {
        if (!header.startsWith(">")) {
            throw new FastaScanException("El encabezado FASTA debe iniciar con '>' y formato " + formatHint + ".");
        }
        String[] parts = header.substring(1).split("\\|", 2);
        if (parts.length < 2) {
            throw new FastaScanException("El encabezado FASTA debe seguir el formato " + formatHint + ".");
        }
        return parts;
    }

    /**
     * Escanea la ruta de entrada para encontrar archivos FASTA,
     * lee su contenido y los mapea a entidades del tipo T utilizando el método mapFileContent.
     * @param inputPath ruta de un archivo FASTA o un directorio que contenga archivos FASTA
     * @return lista de FastaScanItem con el nombre del archivo (sin extensión) y la entidad mapeada
     */
    public List<FastaScanItem<T>> scan(String inputPath) {
        Path root = resolvePath(inputPath);
        List<Path> fastaFiles = collectFastaFiles(root);
        if (fastaFiles.isEmpty()) {
            throw new FastaScanException("No se encontraron archivos con extensión .fasta en la ruta indicada.");
        }

        List<FastaScanItem<T>> entities = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Path file : fastaFiles) {
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                List<T> mapped = mapFileContent(content);
                if (mapped == null || mapped.isEmpty()) {
                    errors.add(file.getFileName() + ": no produjo entidades válidas");
                    continue;
                }
                String normalizedFileName = removeFastaExtension(file.getFileName().toString());
                for (T item : mapped) {
                    entities.add(new FastaScanItem<>(normalizedFileName, item));
                }
            } catch (FastaScanException exception) {
                errors.add(file.getFileName() + ": " + exception.getMessage());
            } catch (IOException exception) {
                errors.add(file.getFileName() + ": no fue posible leer el archivo");
            }
        }

        if (entities.isEmpty()) {
            StringBuilder message = new StringBuilder("No se encontraron archivos FASTA válidos.");
            if (!errors.isEmpty()) {
                message.append(" Errores: ");
                appendErrors(message, errors);
            }
            throw new FastaScanException(message.toString());
        }

        if (!errors.isEmpty()) {
            StringBuilder warning = new StringBuilder("Se descartaron archivos FASTA inválidos: ");
            appendErrors(warning, errors);
            System.out.println(warning);
        }

        return entities;
    }

    /**
     * Método abstracto que debe ser implementado por las subclases para mapear el contenido de un archivo FASTA
     * @param content contenido del archivo FASTA leído como una cadena
     * @return lista de entidades del tipo T mapeadas a partir del contenido del archivo
     */
    protected abstract List<T> mapFileContent(String content);

    /**
     * Resuelve la ruta de entrada, validando que no sea nula o vacía, y que exista en el sistema de archivos.
     * @param inputPath ruta de un archivo FASTA o un directorio que contenga archivos FASTA
     * @return Path resuelto a partir de la ruta de entrada
     */
    private static Path resolvePath(String inputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new FastaScanException("La ruta del archivo FASTA es obligatoria.");
        }
        Path path = Path.of(inputPath.trim());
        if (!Files.exists(path)) {
            throw new FastaScanException("La ruta proporcionada no existe: " + path);
        }
        return path;
    }

    /**
     * Recopila los archivos FASTA desde la ruta dada, ya sea un archivo individual o un directorio que los contenga.
     * @param root ruta de un archivo FASTA o un directorio que contenga archivos FASTA
     * @return lista de Path de los archivos FASTA encontrados, ordenados alfabéticamente por nombre
     */
    private static List<Path> collectFastaFiles(Path root) {
        if (Files.isDirectory(root)) {
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path file : stream) {
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }
                    if (!hasFastaExtension(file)) {
                        continue;
                    }
                    files.add(file);
                }
            } catch (IOException exception) {
                throw new FastaScanException("No fue posible leer el contenido del directorio.", exception);
            }
            sortByName(files);
            return files;
        }

        if (Files.isRegularFile(root) && hasFastaExtension(root)) {
            return List.of(root);
        }

        throw new FastaScanException("La ruta debe ser un archivo .fasta o un directorio que los contenga.");
    }

    /**
     * Valida si el archivo tiene la extensión .fasta (ignorando mayúsculas/minúsculas)
     * @param path ruta del archivo a validar
     * @return true si el archivo tiene extensión .fasta, false en caso contrario
     */
    private static boolean hasFastaExtension(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(FASTA_EXTENSION);
    }

    /**
     * Elimina la extensión .fasta del nombre del archivo, si está presente (ignorando mayúsculas/minúsculas)
     * @param fileName nombre del archivo del cual se desea eliminar la extensión .fasta
     * @return nombre del archivo sin la extensión .fasta, o el nombre original si no tiene esa extensión
     */
    private static String removeFastaExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        if (fileName.toLowerCase(Locale.ROOT).endsWith(FASTA_EXTENSION)) {
            return fileName.substring(0, fileName.length() - FASTA_EXTENSION.length());
        }
        return fileName;
    }

    /**
     * Ordena la lista de archivos alfabéticamente por nombre utilizando un algoritmo de inserción,
     * ignorando mayúsculas/minúsculas para la comparación.
     * @param files lista de Path de los archivos a ordenar
     */
    private static void sortByName(List<Path> files) {
        for (int i = 1; i < files.size(); i++) {
            Path current = files.get(i);
            String currentName = current.getFileName().toString().toLowerCase(Locale.ROOT);
            int j = i - 1;
            while (j >= 0 && files.get(j).getFileName().toString().toLowerCase(Locale.ROOT).compareTo(currentName) > 0) {
                files.set(j + 1, files.get(j));
                j--;
            }
            files.set(j + 1, current);
        }
    }

    /**
     * Agrega los mensajes de error a un StringBuilder,
     * limitando la cantidad de errores mostrados a 5 y agregando una indicación si hay más errores no mostrados.
     * @param builder StringBuilder al cual se agregarán los mensajes de error
     * @param errors lista de mensajes de error a agregar al StringBuilder
     */
    private static void appendErrors(StringBuilder builder, List<String> errors) {
        int max = Math.min(errors.size(), 5);
        for (int i = 0; i < max; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(errors.get(i));
        }
        if (errors.size() > max) {
            builder.append(" | ...");
        }
    }
}
