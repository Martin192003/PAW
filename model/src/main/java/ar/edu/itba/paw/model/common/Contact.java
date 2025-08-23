package ar.edu.itba.paw.model.common;

public class Contact {
    private int id;
    private String name;
    private String email;
    private String phoneNumber;
    private String website;
    private String instagram;


    public Contact(int id, String name, String email, String phoneNumber, String website, String instagram) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.website = website;
        this.instagram = instagram;
    }

    public int getId() { return id; }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getWebsite() { return website; }
    public String getInstagram() { return instagram; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setWebsite(String website) { this.website = website; }
    public void setInstagram(String instagram) { this.instagram = instagram; }

}
