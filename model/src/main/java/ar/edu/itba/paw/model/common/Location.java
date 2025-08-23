package ar.edu.itba.paw.model.common;

public class Location {

    private int id;
    private String city;
    private String province;
    private String country;
    private static final String DEFAULT_COUNTRY = "Argentina";
    private String zone;
    private String address;
    private String longitude;
    private String latitude;



    public Location(int id, String name, String city, String province, String country, String zone, String address, String longitude, String latitude) {
        this.id = id;
        this.city = city;
        this.province = province;
        this.country = country;
        this.zone = zone;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Location(int id, String city, String province, String zone, String address, String longitude, String latitude) {
        this.id = id;
        this.city = city;
        this.province = province;
        this.country = DEFAULT_COUNTRY ;
        this.zone = zone;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public int getId() { return id; }

    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getCountry() { return country; }
    public String getZone() { return zone; }
    public String getAddress() { return address; }
    public String getLongitude() { return longitude; }
    public String getLatitude() { return latitude; }



    public void setZone(String zone) { this.zone = zone; }
    public void setAddress(String address) { this.address = address; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    public void setCity(String city) { this.city = city; }
    public void setProvince(String province) { this.province = province; }
    public void setCountry(String country) { this.country = country; }
}
