package it.polimi.tiw.controllers;

import com.google.gson.Gson;
import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.RubricaDAO;
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
import java.util.List;

@WebServlet("/FindContacts") // Filtered
public class FindContacts extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public FindContacts() {
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();

        resp.setContentType("text/plain");

        Utente utente = (Utente) session.getAttribute("utente");
        String typedUsername = req.getParameter("typed");

        if(typedUsername == null) typedUsername = "";

        RubricaDAO rubricaDAO = new RubricaDAO(connection);
        List<Utente> matchingContacts;
        try {
            matchingContacts = rubricaDAO.getContactByUsername(utente.getIDUtente(), typedUsername);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile accedere ai contatti in rubrica");
            return;
        }

        Gson gson = new Gson();
        String json = gson.toJson(matchingContacts);

        System.out.println(json);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
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
