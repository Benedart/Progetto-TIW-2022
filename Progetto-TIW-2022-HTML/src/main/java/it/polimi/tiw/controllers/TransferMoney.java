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
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/TransferMoney") // Filtered
public class TransferMoney extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public TransferMoney() {
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = "/GetDettaglioConto";
        WebContext ctx = new WebContext(req, resp, getServletContext(), req.getLocale());

        // retrieve and check the submitted parameters
        Integer IDContoSrc;
        Integer IDContoDst;
        Float importo;
        String emailDest = req.getParameter("emailDest");
        String causale = req.getParameter("causale");

        try{
            IDContoSrc = Integer.parseInt(req.getParameter("IDConto"));
        }catch (NumberFormatException | NullPointerException e){
            resp.sendRedirect(req.getContextPath() + path); // broken request (on purpose)
            return;
        }

        try {
            IDContoDst = Integer.parseInt(req.getParameter("IDContoDst"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                                        "&errorMsg=" + "Errore: valore codice conto destinatario non valido");
            return;
        }

        try {
            importo = Float.parseFloat(req.getParameter("importo"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                    "&errorMsg=" + "Errore: valore dell'importo non valido");
            return;
        }

        if (emailDest == null || causale == null || emailDest.isEmpty() || causale.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                    "&errorMsg=" + "Errore: credenziali mancanti o vuote");
            return;
        }

        // import must be positive
        if(importo <= 0){
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                    "&errorMsg=" + "Errore: l'importo deve essere un valore positivo");
            return;
        }

        // import must have 2 decimals maximum
        Pattern importoPattern = Pattern.compile("^\\d+(\\.\\d(\\d)?)?$");
        if(!importoPattern.matcher(importo.toString()).matches()){
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                    "&errorMsg=" + "Errore: formato importo non valido. L'importo deve essere della forma XX o XX.XX");
            return;
        }

        ContoDAO contoDAO = new ContoDAO(connection);

        // retrieving specified accounts
        Conto contoSrc;
        Conto contoDst;
        try {
            contoSrc = contoDAO.getContoByID(IDContoSrc);
            contoDst = contoDAO.getContoByID(IDContoDst);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: Impossibile ricavare informazioni sul conto selezionato");
            return;
        }

        if(contoSrc == null){
            // internal error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Impossibile trovare il conto di partenza selezionato");
            return;
        }

        if(contoDst == null){
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                    "&errorMsg=" + "Errore: il conto selezionato non esiste");
            return;
        }

        // retrieving specified user
        UtenteDAO utenteDAO = new UtenteDAO(connection);
        Utente utenteDst;
        try {
            utenteDst = utenteDAO.getUtenteByEmail(emailDest);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: Impossibile ricavare informazioni sull'utente destinatario");
            return;
        }

        if(utenteDst == null){
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                    "&errorMsg=" + "Errore: l'utente selezionato non esiste");
            return;
        }

        // first checks funds availability
        if (contoSrc.getSaldo() >= importo) {
            // checks that the specified account is owned by the specified user
            if (contoDst.getIDUtente() == utenteDst.getIDUtente()) {
                // checks that the specified account differs from the one in use
                if(contoDst.getIDConto() != contoSrc.getIDConto()){
                    TrasferimentoDAO trasferimentoDAO = new TrasferimentoDAO(connection);

                    try {
                        // transaction
                        Trasferimento trasferimento = trasferimentoDAO.transfer(contoSrc, contoDst, importo, causale);

                        // passing transaction's values
                        path = req.getContextPath() + "/TransactionResult?IDContoSrc=" + trasferimento.getIDContoSrc() +
                        "&IDContoDst=" + trasferimento.getIDContoDst() + "&Data=" + trasferimento.getTimestamp();

                        resp.sendRedirect(path);
                    } catch (SQLException e) {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: Errore durante il trasferimento, i dati non sono stati modificati");
                    }
                }else{
                    resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                            "&errorMsg=" + "Errore: Il conto di destinazione selezionato corrisponde con quello attuale");
                }
            } else {
                resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                        "&errorMsg=" + "Errore: Email destinatario errata per il conto specificato");
            }
        } else {
            resp.sendRedirect(req.getContextPath() + path + "?IDConto=" + IDContoSrc +
                    "&errorMsg=" + "Errore: fondi insufficienti");
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