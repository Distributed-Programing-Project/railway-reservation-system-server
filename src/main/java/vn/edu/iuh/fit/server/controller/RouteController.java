package vn.edu.iuh.fit.server.controller;

public interface RouteController {
    void initialize();

    void setParentController();

    void setEditMode(String routeId);

    void handleAddRoute();

    void handleEditRoute();

    void handleDeleteRoute();

    void handleRefresh();

    void handleDevelopRoute();

    void handleSave();

    void handleCancel();
}

