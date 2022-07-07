package it.polimi.tiw.controllers;

import com.google.gson.Gson;
import it.polimi.tiw.beans.Conto;
import it.polimi.tiw.beans.Trasferimento;
import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.ContoDAO;
import it.polimi.tiw.dao.RubricaDAO;
import it.polimi.tiw.dao.TrasferimentoDAO;
import it.polimi.tiw.dao.UtenteDAO;
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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@WebServlet("/TransactionResult") // Filtered
public class TransactionResult extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public TransactionResult() {
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

        // Retrieving parameters from the query string
        Integer IDcontoDst, IDcontoSrc;
        try {
            IDcontoDst = Integer.parseInt(req.getParameter("IDContoDst"));
            IDcontoSrc = Integer.parseInt(req.getParameter("IDContoSrc"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("IDConto mancante o vuoto");
            return;
        }

        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timestamp timestamp;
        try {
            timestamp = new Timestamp(dateFormatter.parse(req.getParameter("Data")).getTime());
        } catch (ParseException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("La formattazione della data non è andata a buon fine");
            return;
        }

        // checking that the source account is owned by the current user
        ContoDAO contoDAO = new ContoDAO(connection);
        Conto contoSrc;
        try {
            contoSrc = contoDAO.getContoByID(IDcontoSrc);
            // retrieves current user from the session
            Utente utente = (Utente) session.getAttribute("utente");

            // If the specified account isn't owned by the current users, redirects to the homepage
            if(contoSrc == null || utente.getIDUtente() != contoSrc.getIDUtente()){
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().println("Il conto richiesto non ti appartiene");
                return;
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile ricavare informazioni sul conto richiesto");
            return;
        }

        // checking that the requested transaction is the last one for the current account
        // (without this check the info could be incorrect, as there's no history of the account's amount before and
        // after different transactions)
        TrasferimentoDAO trasferimentoDAO = new TrasferimentoDAO(connection);
        Trasferimento trasferimento;
        try {
            if(!trasferimentoDAO.isLastTrasferimento(IDcontoSrc, timestamp)){
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Le informazioni sulla transazione richiesta non sono più disponibili");
                return;
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile ricavare informazioni sul trasferimento richiesto");
            return;
        }

        // retrieving the specified transaction
        try {
            trasferimento = trasferimentoDAO.getTrasferimento(IDcontoSrc, IDcontoDst, timestamp);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile ricavare informazioni sul trasferimento");
            return;
        }

        if(trasferimento == null){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: il trasferimento richiesto non esiste");
            return;
        }

        // retrieving data about the destination user (passing through his account)
        Conto contoDst;
        try {
            contoDst = contoDAO.getContoByID(trasferimento.getIDContoDst());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile ricavare informazioni sul conto di destinazione");
            return;
        }

        if(contoDst == null){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: conto destinazione non esistente");
            return;
        }

        UtenteDAO utenteDAO = new UtenteDAO(connection);
        Utente utenteDst;
        try {
            utenteDst = utenteDAO.getUtenteById(contoDst.getIDUtente());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile ricavare informazioni sul destinatario");
            return;
        }

        if(utenteDst == null){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: il destinatario specificato non esiste");
            return;
        }

        // check if the userDst is a potential new contact
        RubricaDAO rubricaDAO = new RubricaDAO(connection);
        boolean newContact;
        try {
            newContact = !rubricaDAO.isPresent(contoSrc.getIDUtente(), contoDst.getIDUtente());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Errore SQL: impossibile ricavare informazioni sulla rubrica");
            return;
        }

        // creating a utente object with just id and email, without confidential informations
        Utente utenteDstRidotto = new Utente();
        utenteDstRidotto.setIDUtente(utenteDst.getIDUtente());
        utenteDstRidotto.setEmail(utenteDst.getEmail());

        // Sending transaction info back
        TransactionInfo transactionInfo = new TransactionInfo(contoSrc, contoDst, trasferimento.getImporto(), utenteDstRidotto, newContact);

        Gson gson = new Gson();
        String json = gson.toJson(transactionInfo);

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

    // helper class
    private static class TransactionInfo{
        final Conto contoSrc;
        final Conto contoDst;
        final float importo;
        final Utente utenteDst;
        final boolean newContact;

        TransactionInfo(Conto contoSrc, Conto contoDst, float importo, Utente utenteDst, boolean newContact) {
            this.contoSrc = contoSrc;
            this.contoDst = contoDst;
            this.importo = importo;
            this.utenteDst = utenteDst;
            this.newContact = newContact;
        }
    }
}