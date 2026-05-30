package server;

public class StatusDescription{
    private int status;
    private String description;

    public StatusDescription(){
        description = "";
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
