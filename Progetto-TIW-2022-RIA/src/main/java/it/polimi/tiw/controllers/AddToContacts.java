package it.polimi.tiw.controllers;

import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.RubricaDAO;
import it.polimi.tiw.utils.ConnectionHandler;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/AddToContacts") // Filtered
@MultipartConfig
public class AddToContacts extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public AddToContacts() {
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();

        Utente utente = (Utente) session.getAttribute("utente");
        int IDutenteDaSalvare;

        resp.setContentType("text/plain");

        try {
            IDutenteDaSalvare = Integer.parseInt(req.getParameter("IDUtenteDaSalvare"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: valore utente da salvare non valido");
            return;
        }

        RubricaDAO rubricaDAO = new RubricaDAO(connection);

        try {
            rubricaDAO.addUser(utente.getIDUtente(), IDutenteDaSalvare);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile effettuare il salvataggio in rubrica");
        }
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
