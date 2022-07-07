package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Conto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContoDAO {
    private final Connection connection;

    public ContoDAO(Connection connection){
        this.connection = connection;
    }

    public List<Conto> getContiByUtente(int IDUtente) throws SQLException {
        String query = "SELECT IDConto, saldo FROM conti WHERE IDUtente = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, IDUtente);

        ResultSet result = statement.executeQuery();

        List<Conto> conti = new ArrayList<>();
        while(result.next()){
            Conto conto = new Conto();
            conto.setIDConto(result.getInt("IDConto"));
            conto.setIDUtente(IDUtente);
            conto.setSaldo(result.getFloat("saldo"));

            conti.add(conto);
        }

        return conti;
    }

    public List<Integer> getContiByEmail(String email) throws SQLException {
        String query = "SELECT IDConto FROM conti natural join utenti WHERE email = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, email);

        System.out.println(statement);

        ResultSet result = statement.executeQuery();

        List<Integer> conti = new ArrayList<>();
        while(result.next())
            conti.add(result.getInt("IDConto"));

        return conti;
    }

    public Conto getContoByID(int IDConto) throws SQLException {
        String query = "SELECT IDUtente, saldo FROM conti WHERE IDConto = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, IDConto);

        ResultSet result = statement.executeQuery();

        if(!result.isBeforeFirst())
            return null;

        result.next();
        Conto conto = new Conto();
        conto.setIDConto(IDConto);
        conto.setIDUtente(result.getInt("IDUtente"));
        conto.setSaldo(result.getFloat("saldo"));

        return conto;
    }

    public int addConto(int IDUtente) throws SQLException {
        String query = "INSERT INTO conti(IDUtente, Saldo) VALUES (?, 1000)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, IDUtente);

        int code = statement.executeUpdate();

        if(code == 0) throw new SQLException("Registration failed, no rows affected");

        return code;
    }
}