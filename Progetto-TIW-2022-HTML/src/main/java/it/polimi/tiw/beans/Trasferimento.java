package it.polimi.tiw.beans;

import java.sql.Timestamp;
import java.util.Date;

public class Trasferimento {
    private int IDContoSrc;
    private int IDContoDst;
    private Timestamp timestamp;
    private float importo;
    private String causale;

    public String getCausale() {
        return causale;
    }

    public void setCausale(String causale) {
        this.causale = causale;
    }

    public int getIDContoSrc() {
        return IDContoSrc;
    }

    public void setIDContoSrc(int IDContoSrc) {
        this.IDContoSrc = IDContoSrc;
    }

    public int getIDContoDst() {
        return IDContoDst;
    }

    public void setIDContoDst(int IDContoDst) {
        this.IDContoDst = IDContoDst;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public float getImporto() {
        return importo;
    }

    public void setImporto(float importo) {
        this.importo = importo;
    }
}
