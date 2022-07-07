package it.polimi.tiw.controllers;

import it.polimi.tiw.beans.Conto;
import it.polimi.tiw.beans.Trasferimento;
import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.ContoDAO;
import it.polimi.tiw.dao.TrasferimentoDAO;
import it.polimi.tiw.dao.UtenteDAO;
import it.polimi.tiw.utils.ConnectionHandler;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
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
    private TemplateEngine templateEngine;

    public TransactionResult() {
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
        ServletContext servletContext = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();

        String path;
        final WebContext ctx = new WebContext(req, resp, getServletContext(), req.getLocale());

        // Retrieving parameters from the query string
        Integer IDcontoDst, IDcontoSrc;
        try {
            IDcontoDst = Integer.parseInt(req.getParameter("IDContoDst"));
            IDcontoSrc = Integer.parseInt(req.getParameter("IDContoSrc"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "IDConto mancante o vuoto");
            return;
        }

        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timestamp timestamp;
        try {
            timestamp = new Timestamp(dateFormatter.parse(req.getParameter("Data")).getTime());
        } catch (ParseException | NullPointerException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "La formattazione della data non è andata a buon fine");
            e.printStackTrace();
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
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Errore: il conto richiesto è nullo o non fa parte della tua lista conti.");
                return;
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: impossibile ricavare informazioni sul conto richiesto");
            return;
        }

        // checking that the requested transaction is the last one for the current account
        // (without this check the info could be incorrect, as there's no history of the account's amount before and
        // after different transactions)
        TrasferimentoDAO trasferimentoDAO = new TrasferimentoDAO(connection);
        Trasferimento trasferimento;
        try {
            if(!trasferimentoDAO.isLastTrasferimento(IDcontoSrc, timestamp)){
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Le informazioni sulla transazione richiesta non sono più disponibili");
                return;
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL Error: impossibile ricavare informazioni sul trasferimento richiesto");
            return;
        }

        // retrieving the specified transaction
        try {
            trasferimento = trasferimentoDAO.getTrasferimento(IDcontoSrc, IDcontoDst, timestamp);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore SQL: impossibile ricavare informazioni sul trasferimento");
            return;
        }

        if(trasferimento == null){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Errore: il trasferimento richiesto non esiste");
            return;
        }

        // retrieving data about the destination user (passing through his account)
        Conto contoDst;
        try {
            contoDst = contoDAO.getContoByID(trasferimento.getIDContoDst());
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore SQL: impossibile ricavare informazioni sul conto di destinazione");
            return;
        }

        if(contoDst == null){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Errore: conto destinazione non esistente");
            return;
        }

        UtenteDAO utenteDAO = new UtenteDAO(connection);
        Utente utenteDst;
        try {
            utenteDst = utenteDAO.getUtenteById(contoDst.getIDUtente());
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore SQL: impossibile ricavare informazioni sul destinatario");
            return;
        }

        if(utenteDst == null){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Errore: il destinatario specificato non esiste");
            return;
        }

        // Setting context parameters
        path = "/conferma.html";
        ctx.setVariable("contoSrc", contoSrc);
        ctx.setVariable("contoDst", contoDst);
        ctx.setVariable("importo", trasferimento.getImporto());
        ctx.setVariable("utenteDst", utenteDst);

        templateEngine.process(path, ctx, resp.getWriter());
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
