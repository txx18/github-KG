package io.github.txx18.githubKG.model;

/**
 * @author Shane Tang
 * @create 2019-12-23 21:32
 */

public class ResponseSimpleFactory {

    private String status;

    private String msg;

    private Object data;

    public ResponseSimpleFactory(String status) {
        this.status = status;
    }

    public ResponseSimpleFactory(String status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public ResponseSimpleFactory(String status, String msg, Object data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public static ResponseSimpleFactory createResponse(String msg){
        return new ResponseSimpleFactory(msg);
    }

    public static ResponseSimpleFactory createResponse(String status, String msg){
        return new ResponseSimpleFactory(status, msg);
    }
    
    public static ResponseSimpleFactory createResponse(String status, String msg, Object data){
        return new ResponseSimpleFactory(status, msg, data);
    }

    public static ResponseSimpleFactory createSimpleResponse(String cmd) {
        switch (cmd) {
            case "ok":
                return new ResponseSimpleFactory("success", "ok", null);
            case "no":
                return new ResponseSimpleFactory("fail", "服务器内部错误", null);
            default:
                throw new IllegalStateException("Unexpected value: " + cmd);
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
