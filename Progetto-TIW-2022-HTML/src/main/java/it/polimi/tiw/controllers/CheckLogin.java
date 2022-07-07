package it.polimi.tiw.controllers;

import it.polimi.tiw.beans.Utente;
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

@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public CheckLogin() {
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Getting and checking the credentials
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(req, resp, servletContext, req.getLocale());
        String path;

        if(email == null || password == null || email.isEmpty() || password.isEmpty()){
            path = "index.html";
            ctx.setVariable("errorMsg", "Credenziali vuote o mancanti");
            templateEngine.process(path, ctx, resp.getWriter());
            return;
        }

        UtenteDAO utenteDAO = new UtenteDAO(connection);

        try {
            // Checking validity of credentials
            Utente utente = utenteDAO.checkLogin(email, password);

            if(utente == null){
                path = "/index.html";
                ctx.setVariable("errorMsg", "Username o password errati");
                templateEngine.process(path, ctx, resp.getWriter());
            }else{
                path = req.getContextPath() + "/HomeRedirect";
                req.getSession().setMaxInactiveInterval(300);
                req.getSession().setAttribute("utente", utente);
                resp.sendRedirect(path);
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Impossibile validare le cerdenziali");
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
