package util;

public interface Config {

    int TOTAL_PAGES = 40;      // change me to include more pages, currently there's only 13 pages
    String JSESSIONID = "33724F9C4FAA19BD26291109866D0C05";    // change me with a working session id
    String XBE = "N4"; // change me for working XBE parameter (I don't know what is it, but the session won't be valid without it)

    String JDBC_URL = "jdbc:mysql://192.168.1.10:3306/shahid_db?useUnicode=true&characterEncoding=UTF-8";
    String JDBC_USERNAME = "root";
    String JDBC_PASSWORD = "system";
    int MAX_RECENT = 20;
}
