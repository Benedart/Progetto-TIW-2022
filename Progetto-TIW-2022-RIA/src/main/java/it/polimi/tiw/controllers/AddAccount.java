package it.polimi.tiw.controllers;

import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.ContoDAO;
import it.polimi.tiw.utils.ConnectionHandler;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/AddAccount") // Filtered
public class AddAccount extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public AddAccount() {
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();

        Utente utente = (Utente) session.getAttribute("utente");

        ContoDAO contoDAO = new ContoDAO(connection);

        String path;

        try {
            contoDAO.addConto(utente.getIDUtente());

            path = req.getContextPath() + "/home.html";
            resp.sendRedirect(path);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore: errore durante la creazione del conto");
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
