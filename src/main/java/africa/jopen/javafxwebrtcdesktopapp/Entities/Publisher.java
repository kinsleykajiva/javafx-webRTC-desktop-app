package africa.jopen.javafxwebrtcdesktopapp.Entities;


import java.math.BigInteger;


public class Publisher {
    private BigInteger id;
    private String display;

    private BigInteger handleId;

    public Publisher() {

    }

    public Publisher(BigInteger id, String display) {
        this.id = id;
        this.display = display;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public BigInteger getHandleId() {
        return handleId;
    }

    public void setHandleId(BigInteger handleId) {
        this.handleId = handleId;
    }

    @Override
    public String toString() {
        return "Publisher{" +
                "id=" + id +
                ", display='" + display + '\'' +
                ", handleId=" + handleId +
                '}';
    }
}
