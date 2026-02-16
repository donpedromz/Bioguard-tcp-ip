package com.donpedromz.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @version 1.0
 * @author juanp
 * Clase que implementa la interfaz IConfigReader para leer configuraciones desde un archivo de propiedades.
 */
public class PropertiesManager implements IConfigReader{
    /**
     * Objeto Properties que se utiliza para cargar y almacenar
     * las configuraciones leídas desde el archivo de propiedades.
     */
    private final Properties props = new Properties();
    public PropertiesManager(String fileName){
        /*
        Try with-resources Implementation
         */
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)){
            if(is == null){
                throw new RuntimeException("Error no se encontro el archivo");
            }
            props.load(is);
        }catch(IOException e) {
            System.out.println("Error critico al leer las propiedades: " + e.getMessage());
        }
    }
    @Override
    public String getString(String key) {
        return props.getProperty(key);
    }
    @Override
    public int getInt(String key) {
        return Integer.parseInt((props.getProperty(key)));
    }
    @Override
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(props.getProperty(key));
    }
}
