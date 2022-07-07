package it.polimi.tiw.controllers;

import com.google.gson.Gson;
import it.polimi.tiw.beans.Conto;
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
import java.util.List;

@WebServlet("/GetConti") // Filtered
public class GetConti extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public GetConti() {
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

        resp.setContentType("text/plain");

        List<Conto> conti;
        try {
            conti = contoDAO.getContiByUtente(utente.getIDUtente());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("SQL error: impossibile ricavare i conti dell'utente");
            return;
        }

        // Redirect to the Homepage and add accounts to the parameters

        Gson gson = new Gson();
        String json = gson.toJson(conti);

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
