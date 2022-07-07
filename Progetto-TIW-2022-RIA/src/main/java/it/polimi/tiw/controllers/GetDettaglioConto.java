package it.polimi.tiw.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.tiw.beans.Conto;
import it.polimi.tiw.beans.Trasferimento;
import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.ContoDAO;
import it.polimi.tiw.dao.TrasferimentoDAO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/GetDettaglioConto") // Filtered
public class GetDettaglioConto extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public GetDettaglioConto() {
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

        // Get and check params
        int IDConto;
        try {
            IDConto = Integer.parseInt(req.getParameter("IDConto"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("IDConto mancante o vuoto");
            return;
        }

        ContoDAO contoDAO = new ContoDAO(connection);
        try {
            Conto conto = contoDAO.getContoByID(IDConto);
            // retrieves current user from the session
            Utente utente = (Utente) session.getAttribute("utente");

            // If the specified account isn't owned by the current users, redirects to the homepage
            if(conto == null || utente.getIDUtente() != conto.getIDUtente()){
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().println("Error: Unauthorized");
                return;
            }

            TrasferimentoDAO trasferimentoDAO = new TrasferimentoDAO(connection);

            Map<Trasferimento, String> entrateMap = trasferimentoDAO.getEntrateByConto(conto.getIDConto());
            Map<Trasferimento, String> usciteMap = trasferimentoDAO.getUsciteByConto(conto.getIDConto());

            // Redirect to the Homepage and add accounts to the parameters
            List<Map<Trasferimento, String>> trasferimenti = new ArrayList<>();
            trasferimenti.add(entrateMap);
            trasferimenti.add(usciteMap);

            // ComplexMapKeySerialization needed to correctly serialize each Trasferimento
            Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().setDateFormat("dd-MM-YYYY HH:mm:ss").create();
            String json = gson.toJson(trasferimenti);

            resp.setContentType("application/x-www-form-urlencoded");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("SQL error: impossibile ricavare informazioni sul conto richiesto");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
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
