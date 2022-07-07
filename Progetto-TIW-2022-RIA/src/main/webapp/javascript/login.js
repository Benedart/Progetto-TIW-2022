(function() { // avoid variables ending up in the global scope

  document.getElementById("login-button").addEventListener('click', (e) => {
    e.preventDefault()

    let form = e.target.closest("form");

    if (form.checkValidity()) {
      makeCall("POST", 'CheckLogin', form,
        function(req) {
          if (req.readyState === XMLHttpRequest.DONE) {
            let message = req.responseText;

            switch (req.status) {
              case 200:
                sessionStorage.setItem('utente', message);
                window.location.href = "home.html";
                break;
              case 400: // bad request
                document.getElementById("errorMessage").textContent = message;
                document.getElementById("errorMessage").hidden = false;
                break;
              case 401: // unauthorized
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
    	 form.reportValidity();
    }
  });
})();