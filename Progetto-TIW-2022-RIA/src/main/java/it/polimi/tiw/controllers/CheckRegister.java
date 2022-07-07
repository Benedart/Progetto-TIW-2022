package it.polimi.tiw.controllers;

import it.polimi.tiw.dao.UtenteDAO;
import it.polimi.tiw.utils.ConnectionHandler;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/CheckRegister")
@MultipartConfig
public class CheckRegister extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public CheckRegister(){
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String passwordCheck = req.getParameter("passwordCheck");
        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");

        resp.setContentType("text/plain");

        // checking credentials are not null or empty
        if(email == null || password == null || passwordCheck == null || nome == null || cognome == null ||
            email.isEmpty() || password.isEmpty() || nome.isEmpty() || cognome.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: Credenziali mancanti o nulle");
            return;
        }

        // Validate email
        Pattern emailPattern = Pattern.compile("^.+@.+\\..+$");
        if(!emailPattern.matcher(email).matches()){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: Email non valida");
            return;
        }

        // check that the entered passwords match
        if(!passwordCheck.equals(password)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Le password inserite non corrispondono!");
            return;
        }

        UtenteDAO utenteDAO = new UtenteDAO(connection);

        // checks the uniqueness of the email
        boolean isDuplicate;
        try {
            isDuplicate = utenteDAO.checkRegister(email);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("SQL error: impossibile controllare unicità dell'email");
            return;
        }

        if(isDuplicate){
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().println("L'email specificata è già in uso!");
            return;
        }

        // adds the user to the db
        try {
            utenteDAO.registerUser(email, password, nome, cognome);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("SQL error: impossibile registrare l'utente");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public void destroy() {
        try{
            ConnectionHandler.closeConnection(connection);
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
