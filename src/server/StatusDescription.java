package server;

public class StatusDescription{
    private Status status;
    private String description;

    public StatusDescription(){
        description = "";
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
