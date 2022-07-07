package it.polimi.tiw.controllers;

import it.polimi.tiw.beans.Conto;
import it.polimi.tiw.beans.Trasferimento;
import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.ContoDAO;
import it.polimi.tiw.dao.TrasferimentoDAO;
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

@WebServlet("/TransferMoney") // Filtered
@MultipartConfig
public class TransferMoney extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public TransferMoney() {
        super();
    }

    @Override
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        // retrieve and check the submitted parameters
        Integer IDContoSrc;
        Integer IDContoDst;
        Float importo;
        String emailDest = req.getParameter("emailDest");
        String causale = req.getParameter("causale");

        try {
            IDContoSrc = Integer.parseInt(req.getParameter("IDConto"));
            IDContoDst = Integer.parseInt(req.getParameter("IDContoDst"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: valore codice conto destinatario non valido");
            return;
        }

        try {
            importo = Float.parseFloat(req.getParameter("importo"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: valore dell'importo non valido");
            return;
        }

        if (emailDest == null || causale == null || emailDest.isEmpty() || causale.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: credenziali mancanti o vuote");
            return;
        }

        // import must be positive
        if(importo <= 0){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: l'importo deve essere un valore positivo");
            return;
        }

        // import must have 2 decimals maximum
        Pattern importoPattern = Pattern.compile("^\\d+(\\.\\d(\\d)?)?$");
        if(!importoPattern.matcher(importo.toString()).matches()){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: formato importo non valido. L'importo deve essere della forma XX o XX.XX");
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
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("SQL error: Impossibile ricavare informazioni sul conto selezionato");
            return;
        }

        if(contoSrc == null){
            // internal error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Impossibile trovare il conto di partenza selezionato");
            return;
        }

        if(contoDst == null){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: il conto selezionato non esiste");
            return;
        }

        // retrieving specified user
        UtenteDAO utenteDAO = new UtenteDAO(connection);
        Utente utenteDst;
        try {
            utenteDst = utenteDAO.getUtenteByEmail(emailDest);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("SQL error: Impossibile ricavare informazioni sull'utente destinatario");
            return;
        }

        if(utenteDst == null){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: l'utente selezionato non esiste");
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
                        String path = req.getContextPath() + "/TransactionResult?IDContoSrc=" + trasferimento.getIDContoSrc() +
                            "&IDContoDst=" + trasferimento.getIDContoDst() + "&Data=" + trasferimento.getTimestamp();

                        resp.getWriter().println(path);
                        resp.setStatus(HttpServletResponse.SC_OK);
                    } catch (SQLException e) {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("SQL error: Errore durante il trasferimento, i dati non sono stati modificati");
                    }
                }else{
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().println("Errore: Il conto di destinazione selezionato corrisponde con quello attuale");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Errore: Email destinatario errata per il conto specificato");
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Errore: fondi insufficienti");
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