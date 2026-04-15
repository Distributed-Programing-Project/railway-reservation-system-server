package vn.edu.iuh.fit.common.request;

import lombok.AllArgsConstructor;
import vn.edu.iuh.fit.common.enums.ActionType;

import java.io.Serializable;


public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private ActionType action;
    private Object data;

    public Request() {
    }

    public Request(ActionType action, Object data) {
        this.action = action;
        this.data = data;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
