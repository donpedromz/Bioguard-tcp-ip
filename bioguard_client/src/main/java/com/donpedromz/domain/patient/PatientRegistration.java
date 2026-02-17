package com.donpedromz.domain.patient;

import static com.donpedromz.common.TextUtils.sanitizeText;

/**
 * @version 1.0
 * @author juanp
 * Clase que representa el registro de un paciente,
 * incluyendo su documento de identidad, nombre, edad, correo electrónico, género, ciudad y país de residencia.
 */
public class PatientRegistration {
    private final String documentId;
    private final String firstName;
    private final String lastName;
    private final int age;
    private final String email;
    private final String gender;
    private final String city;
    private final String country;

    public PatientRegistration(
            String documentId,
            String firstName,
            String lastName,
            int age,
            String email,
            String gender,
            String city,
            String country
    ) {
        this.documentId = sanitizeText(documentId);
        this.firstName = sanitizeText(firstName);
        this.lastName = sanitizeText(lastName);
        this.age = age;
        this.email = sanitizeText(email);
        this.gender = sanitizeText(gender);
        this.city = sanitizeText(city);
        this.country = sanitizeText(country);
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public String getGender() {
        return gender;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
