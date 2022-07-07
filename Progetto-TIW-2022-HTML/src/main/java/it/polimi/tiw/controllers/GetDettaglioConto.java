package it.polimi.tiw.controllers;

import it.polimi.tiw.beans.Conto;
import it.polimi.tiw.beans.Trasferimento;
import it.polimi.tiw.beans.Utente;
import it.polimi.tiw.dao.ContoDAO;
import it.polimi.tiw.dao.TrasferimentoDAO;
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
import java.util.Map;

@WebServlet("/GetDettaglioConto") // Filtered
public class GetDettaglioConto extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public GetDettaglioConto() {
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
        HttpSession session = req.getSession();

        String path;
        ServletContext servletContext = getServletContext();
        WebContext ctx = new WebContext(req, resp, servletContext, req.getLocale());

        // Get and check params
        Integer IDConto;
        try {
            IDConto = Integer.parseInt(req.getParameter("IDConto"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "IDConto mancante o vuoto");
            return;
        }

        ContoDAO contoDAO = new ContoDAO(connection);
        try {
            Conto conto = contoDAO.getContoByID(IDConto);
            // retrieves current user from the session
            Utente utente = (Utente) session.getAttribute("utente");

            // If the specified account isn't owned by the current users, redirects to the homepage
            if(conto == null || utente.getIDUtente() != conto.getIDUtente()){
                path = req.getContextPath() + "/HomeRedirect";
                resp.sendRedirect(path);
                return;
            }

            TrasferimentoDAO trasferimentoDAO = new TrasferimentoDAO(connection);

            Map<Trasferimento, String> entrateMap = trasferimentoDAO.getEntrateByConto(conto.getIDConto());
            Map<Trasferimento, String> usciteMap = trasferimentoDAO.getUsciteByConto(conto.getIDConto());

            path = "/statoConto.html";
            ctx.setVariable("conto", conto);
            ctx.setVariable("entrate", entrateMap);
            ctx.setVariable("uscite", usciteMap);
            ctx.setVariable("errorMsg", req.getParameter("errorMsg"));

            resp.setContentType("text/html;charset=UTF-8");
            templateEngine.process(path, ctx, resp.getWriter());
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: impossibile ricavare informazioni sul conto richiesto");
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
