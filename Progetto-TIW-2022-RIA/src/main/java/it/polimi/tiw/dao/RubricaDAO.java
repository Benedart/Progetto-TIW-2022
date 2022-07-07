package it.polimi.tiw.dao;

import it.polimi.tiw.beans.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RubricaDAO {
    private final Connection connection;

    public RubricaDAO(Connection connection){
        this.connection = connection;
    }

    public int addUser(int IDUtente, int IDUtenteDaSalvare) throws SQLException {
        String query = "INSERT INTO Rubrica(IDUtente, IDUtenteSalvato) VALUES (?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, IDUtente);
        statement.setInt(2, IDUtenteDaSalvare);

        int code = statement.executeUpdate();

        if(code == 0) throw new SQLException("Salvataggio in rubrica fallito, nessuna riga alterata");

        return code;
    }

    public boolean isPresent(int IDUtente, int IDUtenteSalvato) throws SQLException {
        String query = "SELECT * FROM Rubrica WHERE IDUtente = ? AND IDUtenteSalvato = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, IDUtente);
        statement.setInt(2, IDUtenteSalvato);

        ResultSet result = statement.executeQuery();

        return result.isBeforeFirst();
    }

    public List<Utente> getContactByUsername(int IDUtente, String username) throws SQLException {
        String query = "SELECT IDUtenteSalvato, email FROM rubrica join utenti u on rubrica.IDUtenteSalvato = u.IDUtente WHERE rubrica.IDUtente = ? AND email LIKE ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, IDUtente);
        statement.setString(2, username + "%_");

        System.out.println(statement);

        ResultSet result = statement.executeQuery();

        List<Utente> utenti = new ArrayList<>();
        while(result.next()){
            Utente utente = new Utente();
            utente.setIDUtente(result.getInt("IDUtenteSalvato"));
            utente.setEmail(result.getString("email"));

            utenti.add(utente);
        }

        return utenti;
    }
}