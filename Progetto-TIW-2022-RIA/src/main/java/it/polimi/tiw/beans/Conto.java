package it.polimi.tiw.beans;

public class Conto {
    private int IDConto;
    private int IDUtente;
    private float saldo;

    public int getIDConto() {
        return IDConto;
    }

    public void setIDConto(int IDConto) {
        this.IDConto = IDConto;
    }

    public int getIDUtente() {
        return IDUtente;
    }

    public void setIDUtente(int IDUtente) {
        this.IDUtente = IDUtente;
    }

    public float getSaldo() {
        return saldo;
    }

    public void setSaldo(float saldo) {
        this.saldo = saldo;
    }
}
