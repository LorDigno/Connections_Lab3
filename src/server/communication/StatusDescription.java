package server.communication;

public class StatusDescription{
    private ResponseStatus status;
    private String description;

    public StatusDescription(){
        description = "";
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
