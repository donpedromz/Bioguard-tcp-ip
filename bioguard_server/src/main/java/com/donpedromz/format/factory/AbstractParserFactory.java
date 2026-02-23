package com.donpedromz.format.factory;

import com.donpedromz.exceptions.InvalidMessageFormatException;
import com.donpedromz.format.parser.IContentParser;

import java.util.HashMap;
import java.util.Map;
/**
 * @version 1.0
 * @author juanp
 * Implementación base de ParserFactory que utiliza un mapa para asociar content-types con sus respectivos parsers de contenido. 
 */
public class AbstractParserFactory<T> implements ParserFactory<T> {
    /**
     * Mapa que asocia content-types con sus respectivos parsers de contenido.
     */
    private final Map<String, IContentParser<T>> parserMap = new HashMap<>();
    /**
     * Registra un parser para un content-type específico.
     * @param contentType El content-type que el parser puede manejar (e.g., "application/fasta").
     * @param parser La instancia del parser que se encargará de analizar el contenido con el content-type especificado.
     */
    protected void registerParser(String contentType, IContentParser<T> parser) {
        this.parserMap.put(contentType, parser);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public T parse(String contentType, String body){
        if(!parserMap.containsKey(contentType)){
            throw new InvalidMessageFormatException(
                    "Content-type no soportado: '" + contentType + "'. Content-types válidos: " + parserMap.keySet());
        }
        return parserMap.get(contentType).parse(body);
    }
}
