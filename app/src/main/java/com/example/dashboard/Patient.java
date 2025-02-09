package com.example.dashboard; // Correct your package name

public class Patient {
    private String name;
    private String address;
    private int age;

    // Constructor (add this!)
    public Patient(String name, String address, int age) {
        this.name = name;
        this.address = address;
        this.age = age;
    }

    // Getters and setters (already present, but included for completeness)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}