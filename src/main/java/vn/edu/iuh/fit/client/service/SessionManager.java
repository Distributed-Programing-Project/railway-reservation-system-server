package vn.edu.iuh.fit.client.service;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private String employeeId;
    private String username;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setSession(String employeeId, String username) {
        this.employeeId = employeeId;
        this.username = username;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void clear() {
        this.employeeId = null;
        this.username = null;
    }
}
