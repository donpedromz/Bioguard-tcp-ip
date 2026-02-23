package com.donpedromz.model;

import java.util.UUID;

/**
 * @author juanp
 * @version 1.0
 * Clase que representa la informacion de la entidad
 * Paciente
 */
public class Patient {
    private UUID uuid;
    private String patientDocument;
    private String firstName;
    private String lastName;
    private int edad;
    private String email;
    private String city;
    private String rawGender;
    private Gender gender;
    private String country;

    public Patient() {
    }

    public Patient(String patientDocument, String firstName, String lastName, int edad, String email, String gender, String city, String country) {
        this(null, patientDocument, firstName, lastName, edad, email, gender, city, country);
    }
    public Patient(UUID uuid, String patientDocument, String firstName, String lastName, int edad, String email, String gender, String city, String country) {
        this.uuid = uuid;
        this.patientDocument = patientDocument;
        this.firstName = firstName;
        this.lastName = lastName;
        this.edad = edad;
        this.email = email;
        this.city = city;
        this.country = country;
        this.rawGender = gender;
    }
    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public String getPatientDocument() {
        return patientDocument;
    }
    public void setPatientDocument(String patientDocument) {
        this.patientDocument = patientDocument;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public int getEdad() {
        return edad;
    }
    public void setEdad(int edad) {
        this.edad = edad;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Obtiene el valor del rawGender como una cadena.
     * Si el genero es nulo, devuelve el valor crudo almacenado.
     * @return El genero del paciente como cadena, o el valor crudo si no se pudo mapear a un enum valido.
     */
    public String getGender() {
        if (gender != null) {
            return gender.getValue();
        }
        return rawGender;
    }

    public void setGender(String gender) {
        this.rawGender = gender;
        try {
            this.gender = Gender.fromValue(gender);
        } catch (RuntimeException exception) {
            this.gender = null;
        }
    }

    public Gender getGenderEnum() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "Patient{" +
                ", uuid=" + uuid +
                ", patientDocument='" + patientDocument + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", edad=" + edad +
                ", email='" + email + '\'' +
                ", city='" + city + '\'' +
                ", gender='" + gender + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
