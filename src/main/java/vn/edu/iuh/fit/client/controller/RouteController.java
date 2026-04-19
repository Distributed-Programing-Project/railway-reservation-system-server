package vn.edu.iuh.fit.client.controller;

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

