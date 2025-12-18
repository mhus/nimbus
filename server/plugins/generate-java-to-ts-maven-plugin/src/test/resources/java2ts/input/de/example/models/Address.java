package de.example.models;

@GenerateTypeScript("models")
public class Address {

    private String street;
    private String city;

    @TypeScript(type="number")
    private Integer houseNumber;
}
