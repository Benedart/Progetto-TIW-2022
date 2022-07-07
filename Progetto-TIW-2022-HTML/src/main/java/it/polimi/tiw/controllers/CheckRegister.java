package it.polimi.tiw.controllers;

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

@WebServlet("/CheckRegister")
public class CheckRegister extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private TemplateEngine templateEngine;

    public CheckRegister(){
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
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String passwordCheck = req.getParameter("passwordCheck");
        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");

        String path;
        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(req, resp, servletContext, req.getLocale());

        // checking credentials are not null or empty
        if(email == null || password == null || passwordCheck == null || nome == null || cognome == null ||
            email.isEmpty() || password.isEmpty() || nome.isEmpty() || cognome.isEmpty()) {
            path = "/register.html";
            ctx.setVariable("errorMsg", "Errore: Credenziali mancanti o nulle");
            templateEngine.process(path, ctx, resp.getWriter());
            return;
        }

        // Validate email
        Pattern emailPattern = Pattern.compile("^.+@.+\\..+$");
        if(!emailPattern.matcher(email).matches()){
            path = "/register.html";
            ctx.setVariable("errorMsg", "Errore: email non valida");
            templateEngine.process(path, ctx, resp.getWriter());
            return;
        }

        boolean isDuplicate = false;
        boolean pswError = false;

        // check that the entered passwords match
        if(!passwordCheck.equals(password)) {
            pswError = true;
            ctx.setVariable("errorMsg", "Le password inserite non corrispondono!");
        }

        if(!pswError){
            UtenteDAO utenteDAO = new UtenteDAO(connection);

            // checks the uniqueness of the email
            try {
                isDuplicate = utenteDAO.checkRegister(email);
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: impossibile controllare unicità dell'email");
            }

            if(isDuplicate){
                ctx.setVariable("errorMsg", "L'email specificata è già in uso!");
            }else {
                // adds the user to the db
                try {
                    utenteDAO.registerUser(email, password, nome, cognome);
                } catch (SQLException e) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: impossibile registrare l'utente");
                    return;
                }

                // redirects to the login page
                path = getServletContext().getContextPath() + "/index.html";
                resp.sendRedirect(path);

                // alternatively, redirects to the registered user's homepage (NOT TESTED)
                // asks the db for the user to ensure that it was added correctly
                /*
                path = req.getContextPath() + "/HomeRedirect";
                try {
                    Utente utente = utenteDAO.getUtenteByEmail(email);
                    req.getSession().setAttribute("utente", utente);
                    resp.sendRedirect(path);
                } catch (SQLException e) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore SQL: impossibile ricavare l'utente richiesto");
                    return;
                }
                 */
            }
        }

        // error handling
        path = "/register.html";
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
