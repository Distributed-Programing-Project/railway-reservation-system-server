package vn.edu.iuh.fit.client;


import vn.edu.iuh.fit.server.util.JPAUtils;

public class Main {
    public static void main(String[] args) {
        JPAUtils.getEntityManager() ;
    }
}
