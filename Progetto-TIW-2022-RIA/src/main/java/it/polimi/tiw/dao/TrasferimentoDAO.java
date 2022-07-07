package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Conto;
import it.polimi.tiw.beans.Trasferimento;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class TrasferimentoDAO {
    private final Connection connection;

    public TrasferimentoDAO(Connection connection){
        this.connection = connection;
    }

    public Trasferimento transfer(Conto contoSrc, Conto contoDst, Float importo, String causale) throws SQLException {
        String query = "UPDATE conti SET saldo = ? WHERE IDConto = ?";
        String insert = "INSERT INTO trasferimenti (IDContoSrc, IDContoDst, Data, Importo, Causale) VALUES (?, ?, ?, ?, ?)";

        connection.setAutoCommit(false);

        try{
            PreparedStatement statementAddebito = connection.prepareStatement(query);
            statementAddebito.setFloat(1, contoSrc.getSaldo() - importo);
            statementAddebito.setInt(2, contoSrc.getIDConto());

            PreparedStatement statementAccredito = connection.prepareStatement(query);
            statementAccredito.setFloat(1, contoDst.getSaldo() + importo);
            statementAccredito.setInt(2, contoDst.getIDConto());

            Date date = new Date();
            // this is needed to ensure that the timestamp is not wrongly approximated
            Timestamp timestamp = new Timestamp(date.getTime() / 1000 * 1000);
            PreparedStatement statementInsert = connection.prepareStatement(insert);
            statementInsert.setInt(1, contoSrc.getIDConto());
            statementInsert.setInt(2, contoDst.getIDConto());
            statementInsert.setTimestamp(3, timestamp);
            statementInsert.setFloat(4, importo);
            statementInsert.setString(5, causale);

            statementInsert.executeUpdate();
            statementAddebito.executeUpdate();
            statementAccredito.executeUpdate();
            connection.commit();

            Trasferimento trasferimento = new Trasferimento();
            trasferimento.setCausale(causale);
            trasferimento.setTimestamp(timestamp);
            trasferimento.setImporto(importo);
            trasferimento.setIDContoDst(contoDst.getIDConto());
            trasferimento.setIDContoSrc(contoSrc.getIDConto());

            return trasferimento;
        }catch (SQLException e){
            connection.rollback();
            throw e;
        }finally {
            connection.setAutoCommit(true);
        }
    }

    public Trasferimento getTrasferimento(int idContoSrc, int idContoDst, Timestamp timestamp) throws SQLException {
        String query = "SELECT Importo, Causale FROM trasferimenti WHERE IDContoSrc = ? AND IDContoDst = ? AND Data = ?";
        PreparedStatement statement = connection.prepareStatement(query);

        statement.setInt(1, idContoSrc);
        statement.setInt(2, idContoDst);
        statement.setTimestamp(3, timestamp);

        ResultSet result = statement.executeQuery();

        if(!result.isBeforeFirst())
            return null;

        result.next();

        Trasferimento trasferimento = new Trasferimento();
        trasferimento.setIDContoSrc(idContoSrc);
        trasferimento.setIDContoDst(idContoDst);
        trasferimento.setTimestamp(new Timestamp(timestamp.getTime()));
        trasferimento.setImporto(result.getFloat("Importo"));
        trasferimento.setCausale(result.getString("Causale"));

        return trasferimento;
    }

    public LinkedHashMap<Trasferimento, String> getEntrateByConto(int idConto) throws SQLException {
        String query = "SELECT IDContoSrc, IDContoDst, email, data, importo, causale " +
                        "FROM (trasferimenti join conti c on c.IDConto = trasferimenti.IDContoSrc) natural join utenti " +
                        "WHERE IDContoDst = ? " +
                        "ORDER BY data DESC";
        return getTrasferimentiMail(idConto, query);
    }

    public LinkedHashMap<Trasferimento, String> getUsciteByConto(int idConto) throws SQLException {
        String query =  "SELECT IDContoSrc, IDContoDst, email, data, importo, causale " +
                        "FROM (trasferimenti join conti c on c.IDConto = trasferimenti.IDContoDst) natural join utenti " +
                        "WHERE IDContoSrc = ? " +
                        "ORDER BY data DESC";
        return getTrasferimentiMail(idConto, query);
    }

    private LinkedHashMap<Trasferimento, String> getTrasferimentiMail(int idConto, String query) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, idConto);

        ResultSet result = statement.executeQuery();

        if(!result.isBeforeFirst())
            return null;

        LinkedHashMap<Trasferimento, String> trasferimentiMail = new LinkedHashMap<>();
        while(result.next()){
            Trasferimento trasferimento = new Trasferimento();
            trasferimento.setIDContoSrc(result.getInt("IDContoSrc"));
            trasferimento.setIDContoDst(result.getInt("IDContoDst"));
            trasferimento.setTimestamp(result.getTimestamp("data"));
            trasferimento.setImporto(result.getFloat("importo"));
            trasferimento.setCausale(result.getString("causale"));

            trasferimentiMail.put(trasferimento, result.getString("email"));
        }

        return trasferimentiMail;
    }

    public boolean isLastTrasferimento(int idContoSrc, Timestamp timestamp) throws SQLException {
        String query = "SELECT * FROM trasferimenti WHERE (IDContoSrc = ? OR IDContoDst = ?) AND Data > ?";
        PreparedStatement statement = connection.prepareStatement(query);

        statement.setInt(1, idContoSrc);
        statement.setInt(2, idContoSrc);
        statement.setTimestamp(3, timestamp);

        ResultSet result = statement.executeQuery();

        return !result.isBeforeFirst();
    }
}
