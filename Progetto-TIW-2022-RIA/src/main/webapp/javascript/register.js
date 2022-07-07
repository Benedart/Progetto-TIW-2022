function passwordMatch(){
    let pswd1 = document.getElementById("pswd1").value;
    let pswd2 = document.getElementById("pswd2").value;

    if(pswd1 != null && pswd2 != null && pswd1 !== "" && pswd2 !== "")
        return pswd1.match(pswd2);

    return false;
}

(function() { // avoid variables ending up in the global scope

    document.getElementById("registerForm").addEventListener('submit', (e) => {
        e.preventDefault()

        let form = e.target;

        if (form.checkValidity()) {
            if(!passwordMatch()){
                document.getElementById("errorMessage").textContent = "Le password non corrispondono (o sono mancanti)!";
                document.getElementById("errorMessage").hidden = false;
                return false;
            }

            makeCall("POST", 'CheckRegister', form,
                function(req) {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        let message = req.responseText;

                        switch (req.status) {
                            case 200:
                                window.location.href = "index.html";
                                break;
                            case 400: // bad request
                                document.getElementById("errorMessage").textContent = message;
                                document.getElementById("errorMessage").hidden = false;
                                break;
                            case 401: // unauthorized
                                document.getElementById("errorMessage").textContent = message;
                                document.getElementById("errorMessage").hidden = false;
                                break;
                            case 409: // conflict
                                document.getElementById("errorMessage").textContent = message;
                                document.getElementById("errorMessage").hidden = false;
                                break;
                            case 500: // server error
                                document.getElementById("errorMessage").textContent = message;
                                document.getElementById("errorMessage").hidden = false;
                                break;
                        }
                    }
                }
            );
        } else {
            document.getElementById("errorMessage").hidden = true;
            form.reportValidity();
        }
    });
})();