package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Utente;

import java.sql.*;

public class UtenteDAO {
    private final Connection connection;

    public UtenteDAO(Connection connection){
        this.connection = connection;
    }

    public Utente checkLogin(String email, String password) throws SQLException {
        String query = "SELECT IDUtente, nome, cognome FROM utenti WHERE email = ? AND password = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, email);
        statement.setString(2, password);

        ResultSet result = statement.executeQuery();

        if (!result.isBeforeFirst()) // no results, credential check failed
            return null;

        result.next();
        Utente utente = new Utente();

        utente.setEmail(email);
        utente.setPassword(password);
        utente.setIDUtente(result.getInt("IDUtente"));
        utente.setNome(result.getString("nome"));
        utente.setCognome(result.getString("cognome"));

        return utente;
    }

    public boolean checkRegister(String email) throws SQLException {
        String query = "SELECT * FROM utenti WHERE email = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, email);
        ResultSet result = statement.executeQuery();

        return result.isBeforeFirst();
    }

    public int registerUser(String email, String password, String nome, String cognome) throws SQLException {
        String query = "INSERT INTO utenti(Nome, Cognome, Email, Password) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, nome);
        statement.setString(2, cognome);
        statement.setString(3, email);
        statement.setString(4, password);

        int code = statement.executeUpdate();
        //System.out.println("CODE:" + code);

        if(code == 0) throw new SQLException("Registration failed, no rows affected");

        int key = -1;
        try(ResultSet generatedKey = statement.getGeneratedKeys()){
            if(generatedKey.next()) {
                key = generatedKey.getInt(1);
                //System.out.println("KEY: " + key);
            }
        }

        return key;
    }

    public Utente getUtenteById(int IDUtente) throws SQLException {
        String query = "SELECT Nome, Cognome, Email, Password FROM utenti WHERE IDUtente = ?";
        PreparedStatement statement = connection.prepareStatement(query);

        statement.setInt(1, IDUtente);

        ResultSet result = statement.executeQuery();

        if(!result.isBeforeFirst())
            return null;

        result.next();
        Utente utente = new Utente();
        utente.setIDUtente(IDUtente);
        utente.setNome(result.getString("Nome"));
        utente.setCognome(result.getString("Cognome"));
        utente.setEmail(result.getString("Email"));
        utente.setPassword(result.getString("Password"));

        return utente;
    }

    public Utente getUtenteByEmail(String email) throws SQLException {
        String query = "SELECT IDUtente, nome, cognome, password FROM utenti WHERE email = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, email);

        ResultSet result = statement.executeQuery();

        if(!result.isBeforeFirst())
            return null;

        result.next();
        Utente utente = new Utente();
        utente.setEmail(email);
        utente.setIDUtente(result.getInt("IDUtente"));
        utente.setNome(result.getString("nome"));
        utente.setCognome(result.getString("cognome"));
        utente.setPassword(result.getString("password"));

        return utente;
    }
}
