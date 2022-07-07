package it.polimi.tiw.controllers;

import com.google.gson.Gson;
import it.polimi.tiw.dao.ContoDAO;
import it.polimi.tiw.utils.ConnectionHandler;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/GetContiContatto") // Filtered
public class GetContiContatto extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public GetContiContatto() {
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String emailContatto = req.getParameter("emailContatto");

        resp.setContentType("text/plain");

        if(emailContatto == null){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: Email del destinatario vuota o nulla");
            return;
        }

        ContoDAO contoDAO = new ContoDAO(connection);
        List<Integer> idConti;
        try {
            idConti = contoDAO.getContiByEmail(emailContatto);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("SQL error: impossiible ricavare i conti del contatto");
            return;
        }

        Gson gson = new Gson();
        String json = gson.toJson(idConti);

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
