{
    // page components
    let accountList, accountDetails, transactionModal;

    window.addEventListener("load", () => {
        if (sessionStorage.getItem("utente") == null) {
            window.location.href = "index.html";
        } else {
            document.getElementById("username").textContent = window.sessionStorage.getItem("utente")

            // creating the account list
            accountList = new AccountList(
                document.getElementById("conti"),
                document.getElementById("contiBody")
            );
            accountList.reset();
            accountList.show();

            // preparing the section for the account's details
            accountDetails = new AccountDetails(
                document.getElementById("conto"),
                document.getElementById("entrateContainer"),
                document.getElementById("entrateBody"),
                document.getElementById("usciteContainer"),
                document.getElementById("usciteBody"),
                document.getElementById("errorMessage")
            );
            accountDetails.reset();

            // preparing the section for the transaction form
            transactionModal = new TransactionModal(
                document.getElementById("transferForm"),
                document.getElementById("transferEmailDst"),
                document.getElementById("transferId"),
                document.getElementById("transferButton"),
                document.getElementById("closeTransferMoneyForm"),
                document.getElementById("contactList"),
                document.getElementById("contoDestinatario")
            );
            transactionModal.init();

            // adding listener to logout button
            document.getElementById("logoutButton").addEventListener("click",
                () => sessionStorage.removeItem("utente")
            )
        } // display initial content
    }, false);

    function AccountList(accountList, accountListBody){
        this.accountList = accountList;
        this.accountListBody = accountListBody;

        this.show = function() {
            let self = this;

            makeCall("GET", "GetConti", null,
                function(req) {
                    if (req.readyState === 4) {
                        let message = req.responseText;
                        let errorPar = document.getElementById("contiError");

                        if (req.status === 200) {
                            let conti = JSON.parse(req.responseText);

                            if (conti.length === 0) {
                                errorPar.textContent = "Nessun conto a tuo nome presente!";
                                errorPar.classList.add("alert", "alert-danger");
                                return;
                            }

                            self.update(conti); // self visible by closure
                        } else if (req.status === 403) {
                            window.location.href = req.getResponseHeader("Location");
                            window.sessionStorage.removeItem('utente');
                        } else {
                            errorPar.textContent = message;
                        }
                    }
                }
            )
        }

        this.update = function(conti) {
            this.accountListBody.innerHTML = ""; // empty the table body
            // build updated list
            let self = this;

            conti.forEach(function(conto){
                let row = document.createElement("tr");

                let idConto = document.createElement("td");
                idConto.textContent = conto.IDConto;
                row.appendChild(idConto);

                let saldo = document.createElement("td");
                saldo.textContent = "€" + conto.saldo;
                row.appendChild(saldo);

                let linkcell = document.createElement("td");
                let anchor = document.createElement("a");
                linkcell.appendChild(anchor);

                let linkText = document.createTextNode("Dettagli");
                anchor.appendChild(linkText);
                anchor.setAttribute('IDConto', conto.IDConto); // set a custom HTML attribute
                anchor.setAttribute('Saldo', conto.saldo);
                anchor.addEventListener("click", (e) => {
                    // dependency via module parameter
                    accountDetails.reset();
                    accountDetails.show(e.target.getAttribute("IDConto"), e.target.getAttribute("Saldo")); // the list must know the details container

                    let selectedRows = document.getElementsByClassName("selected-row");
                    for(let i = 0; i<selectedRows.length; i++)
                        selectedRows[i].classList.remove("selected-row");

                    row.classList.add("selected-row");
                }, false);
                anchor.href = "#";
                row.appendChild(linkcell);
                self.accountListBody.appendChild(row);
            })

            this.accountList.hidden = false;
        }

        this.reset = function() {
            this.accountList.hidden = true;
        }
    }

    function AccountDetails(contoContainer, entrateContainer, entrateBody, usciteContainer, usciteBody, errorPar){
        this.contoContainer = contoContainer;
        this.entrateContainer = entrateContainer;
        this.entrateBody = entrateBody;
        this.usciteContainer = usciteContainer;
        this.usciteBody = usciteBody;
        this.errorPar = errorPar;

        this.show = function(IDConto, saldo) {
            let self = this;

            makeCall("GET", "GetDettaglioConto?IDConto=" + IDConto, null,
                function(req) {
                    if (req.readyState === 4) {
                        let message = req.responseText;

                        if (req.status === 200) {
                            let trasferimenti = JSON.parse(req.responseText);

                            if (trasferimenti[0] === null && trasferimenti[1] === null) {
                                errorPar.textContent = "Nessun trasferimento effettuato!";
                                errorPar.classList.add("alert", "alert-warning");
                            }

                            document.getElementById("numeroConto").textContent = "Stato del conto - " + IDConto;
                            document.getElementById("saldoConto").textContent = "€ " + saldo;
                            if(document.getElementById("openTransferModalButton") == null)
                                self.createTransferButton();

                            // updating the value of the hidden field in the transaction form
                            transactionModal.update(IDConto);

                            self.update(trasferimenti); // self visible by closure
                        } else if (req.status === 403) {
                            window.location.href = req.getResponseHeader("Location");
                            window.sessionStorage.removeItem('utente');
                        } else {
                            errorPar.textContent = message;
                            errorPar.classList.add("alert", "alert-danger");
                        }
                    }
                }
            )
        }

        this.update = function(trasferimenti) {
            this.entrateBody.innerHTML = ""; // empty tables' body
            this.usciteBody.innerHTML = "";
            // build updated list
            let self = this;

            let entrate = trasferimenti[0];
            let uscite = trasferimenti[1];

            if(entrate != null){
                for(const [entrata, emailOrdinante] of entrate){
                    let row = document.createElement("tr");

                    let importo = document.createElement("td");
                    importo.textContent = "€ " + entrata.importo;
                    row.appendChild(importo);

                    let ordinante = document.createElement("td");
                    ordinante.textContent = entrata.IDContoSrc + " - " + emailOrdinante;
                    row.appendChild(ordinante);

                    let data = document.createElement("td");
                    data.textContent = entrata.timestamp;
                    row.appendChild(data);

                    let causale = document.createElement("td");
                    causale.textContent = entrata.causale;
                    row.appendChild(causale);

                    self.entrateBody.appendChild(row);
                }

                this.entrateContainer.hidden = false;
            }

            if(uscite != null){
                for(const [uscita, emailDestinatario] of uscite){
                    let row = document.createElement("tr");

                    let importo = document.createElement("td");
                    importo.textContent = "€ " + uscita.importo;
                    row.appendChild(importo);

                    let destinatario = document.createElement("td");
                    destinatario.textContent = uscita.IDContoDst + " - " + emailDestinatario;
                    row.appendChild(destinatario);

                    let data = document.createElement("td");
                    data.textContent = uscita.timestamp;
                    row.appendChild(data);

                    let causale = document.createElement("td");
                    causale.textContent = uscita.causale;
                    row.appendChild(causale);

                    self.usciteBody.appendChild(row);
                }

                this.usciteContainer.hidden = false;
            }

            this.contoContainer.hidden = false;
        }

        this.createTransferButton = function (){
            let transferButton = document.createElement("button");
            transferButton.textContent = "Trasferisci denaro";
            transferButton.classList.add("btn", "btn-primary", "btn-lg", "float-end");
            transferButton.id = "openTransferModalButton";

            transferButton.addEventListener("click",
                () => openModal("transferModal")
            );

            document.getElementById("transferButtonDiv").appendChild(transferButton);
        }

        this.reset = function() {
            this.contoContainer.hidden = true;
            this.entrateContainer.hidden = true;
            this.usciteContainer.hidden = true;
            this.errorPar.innerHTML = "";
            this.errorPar.classList = "";
        }
    }

    function findMatches(typedText){
        makeCall("GET", "FindContacts?typed=" + typedText, null, (req) => {
            if (req.readyState === 4){
                let message = req.responseText;
                let errorPar = document.getElementById("transactionError");

                switch (req.status){
                    case 200:
                        let utentiMatchati = JSON.parse(req.responseText);

                        transactionModal.showMatches(utentiMatchati);

                        break;
                    default:
                        errorPar.textContent = message;
                        errorPar.classList.add("alert", "alert-danger");
                        break;
                }
            }
        });
    }

    function findAccounts(email){
        makeCall("GET", "GetContiContatto?emailContatto=" + email, null, (req) => {
            if(req.readyState === 4){
                let message = req.responseText;

                switch(req.status){
                    case 200:
                        let contiContatto = JSON.parse(req.responseText);

                        transactionModal.showContactAccounts(contiContatto);

                        break;
                    default:
                        alert(message);
                        break;
                }
            }
        });
    }

    function TransactionModal(transferForm, transferEmailDst, transferId, transferButton, closeFormButton, contactList, contactAccount){
        this.transferForm = transferForm;
        this.transferEmailDst = transferEmailDst;
        this.transferId = transferId;
        this.transferButton = transferButton;
        this.closeFormButton = closeFormButton;
        this.contactList = contactList;
        this.contactAccount = contactAccount;

        this.init = function(){
            let self = this;

            // button to fire the transaction
            this.transferButton.addEventListener('click', (e) => {
                e.preventDefault();

                let form = self.transferForm;

                if (form.checkValidity()) {
                    // closes the transfer money form modal
                    closeModal("transferModal");

                    makeCall("POST", 'TransferMoney', form,
                        function(req) {
                            if (req.readyState === XMLHttpRequest.DONE) {
                                let message = req.responseText;

                                switch (req.status) {
                                    case 200:
                                        getTransactionResult(message);

                                        // clean error message
                                        document.getElementById("errorMessage").innerHTML = "";
                                        document.getElementById("errorMessage").className = "";

                                        // reset form
                                        self.reset();

                                        break;
                                    case 400: // bad request
                                        document.getElementById("errorMessage").textContent = message;
                                        document.getElementById("errorMessage").classList.add("alert", "alert-danger")
                                        break;
                                    case 500: // server error
                                        document.getElementById("errorMessage").textContent = message;
                                        document.getElementById("errorMessage").classList.add("alert", "alert-danger")
                                        break;
                                }
                            }
                        }
                    );
                } else {
                    form.reportValidity();
                }
            });

            // email autofill
            this.transferEmailDst.addEventListener("keyup", () => findMatches(this.transferEmailDst.value));
            // fire a keyup to show all contacts
            let event = new Event("keyup");
            this.transferEmailDst.dispatchEvent(event);
            // find user's accounts
            this.transferEmailDst.addEventListener("change", () => {
                let transferEmailDst = this.transferEmailDst.value;
                if(transferEmailDst !== null && transferEmailDst !== "")
                    findAccounts(transferEmailDst)
            });
            this.closeFormButton.addEventListener("click", () => {
                    closeModal("transferModal");
                    this.reset();
                }
            );
        }

        this.update = function(IDConto){
            this.transferId.value = IDConto;
        }

        this.showMatches = function(utenti){
            let self = this;

            self.contactList.innerHTML = "";
            if(utenti.length > 0){
                utenti.forEach(function(utente){
                    let contatto = document.createElement("option");
                    contatto.textContent = utente.email;
                    contatto.value = utente.email;
                    self.contactList.appendChild(contatto);
                })
            }
        }

        this.showContactAccounts = function(idContiContatto) {
            let self = this;

            self.contactAccount.innerHTML = "";
            self.transferEmailDst.setCustomValidity("");

            if(idContiContatto.length === 0){
                self.transferEmailDst.setCustomValidity("Nessun utente trovato");
                self.transferForm.reportValidity();
            }else if(idContiContatto.length === 1){
                let contactAccount = document.createElement("input");
                contactAccount.type = "hidden";
                contactAccount.name = "IDContoDst";
                contactAccount.value = idContiContatto[0];
                self.contactAccount.appendChild(contactAccount);
            }else{
                let contactAccounts = document.createElement("select");
                contactAccounts.id = "contactAccounts";
                contactAccounts.type = "number";
                contactAccounts.name = "IDContoDst";
                contactAccounts.classList.add("form-select");
                contactAccounts.required = true;

                let defaultOption = document.createElement("option");
                defaultOption.innerText = "Conto";
                defaultOption.selected = true;
                defaultOption.value = "";
                contactAccounts.appendChild(defaultOption);

                idContiContatto.forEach(function(idConto){
                    // check that idConto differs from the one being used to make the transaction
                    // (same account transactions lead to a serverside error)
                    if(idConto != self.transferId.value){
                        let idContoOption = document.createElement("option");
                        idContoOption.innerText = idConto;
                        idContoOption.value = idConto;
                        contactAccounts.appendChild(idContoOption);
                    }
                });
                self.contactAccount.appendChild(contactAccounts);
            }
        }

        this.reset = function(){
            this.transferForm.reset();
            this.contactAccount.innerHTML = "";

            // fire a keyup event to update contact list
            let event = new Event("keyup");
            this.transferEmailDst.dispatchEvent(event);
        }
    }

    function getTransactionResult(path){
        makeCall("GET", path, null, (req) => {
            if(req.readyState === XMLHttpRequest.DONE){
                let message = req.responseText;

                if (req.status === 200) {
                    let trasferimento = JSON.parse(req.responseText);

                    // update the background page
                    accountList.show();
                    accountDetails.show(trasferimento.contoSrc.IDConto, trasferimento.contoSrc.saldo);

                    // show the transaction result with a modal
                    prepareModalTrasferimento(trasferimento);
                    openModal("transactionResult");
                } else if (req.status === 403) {
                    window.location.href = req.getResponseHeader("Location");
                    window.sessionStorage.removeItem('utente');
                } else {
                    alert(message);
                }
            }
        });
    }

    function addContact(utenteDaSalvare){
        let IDUtenteDaSalvare = utenteDaSalvare.IDUtente;

        // makeCall - POST without a form
        let req = new XMLHttpRequest();

        req.onreadystatechange = function() {
            if (req.readyState === 4) {
                let message = req.responseText;
                let resultMessage = document.getElementById("transferMessage");

                switch (req.status){
                    case 200: // OK
                        resultMessage.textContent = "Utente aggiunto ai contatti!";
                        resultMessage.classList.add("alert", "alert-success");

                        // fire a keyup event to update the suggested contact list
                        let event = new Event("keyup");
                        document.getElementById("transferEmailDst").dispatchEvent(event);

                        // removes the button
                        document.getElementById("addContactButton").remove();
                        break;
                    default: // errors
                        resultMessage.textContent = message;
                        resultMessage.classList.add("alert", "alert-danger");
                        break;
                }
            }
        };

        req.open("POST", "AddToContacts");
        req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        req.send("IDUtenteDaSalvare=" + IDUtenteDaSalvare);
    }

    function openModal(modalID) {
        document.getElementById(modalID).style.display = "block";
        document.getElementById(modalID).classList.add("show");
    }

    function closeModal(modalID) {
        document.getElementById(modalID).style.display = "none";
        document.getElementById(modalID).classList.remove("show");
    }

    function prepareModalTrasferimento(trasferimento){
        // clears modal's table body
        let modalBody = document.getElementById("modalBody");
        modalBody.innerHTML = "";

        let contoAddebitoRow = document.createElement("tr");
        contoAddebitoRow.id = "infoContoAddebito";
        let data = document.createElement("td");
        data.innerHTML = "<b>Conto Addebito</b>";
        contoAddebitoRow.appendChild(data);
        modalBody.appendChild(contoAddebitoRow);

        let contoAccreditoRow = document.createElement("tr");
        contoAccreditoRow.id = "infoContoAccredito";
        data = document.createElement("td");
        data.innerHTML = "<b>Conto Accredito</b>";
        contoAccreditoRow.appendChild(data);
        modalBody.appendChild(contoAccreditoRow);

        document.getElementById("closeModalButton").addEventListener("click", () => closeModal("transactionResult"));

        document.getElementById("esitoTransazione").textContent = "Transazione eseguita con successo. Importo trasferito: € " + trasferimento.importo;

        // contoAddebitoRow

        let idConto = document.createElement("td");
        idConto.textContent = trasferimento.contoSrc.IDConto;
        contoAddebitoRow.appendChild(idConto);

        let utente = document.createElement("td");
        utente.textContent = sessionStorage.getItem("utente");
        contoAddebitoRow.appendChild(utente);

        let importoPRE = document.createElement("td");
        importoPRE.textContent = (trasferimento.contoSrc.saldo + trasferimento.importo).toFixed(2);
        contoAddebitoRow.appendChild(importoPRE);

        let importoPOST = document.createElement("td");
        importoPOST.textContent = trasferimento.contoSrc.saldo;
        contoAddebitoRow.appendChild(importoPOST);

        // contoAccreditoRow

        idConto = document.createElement("td");
        idConto.textContent = trasferimento.contoDst.IDConto;
        contoAccreditoRow.appendChild(idConto);

        utente = document.createElement("td");
        utente.textContent = trasferimento.utenteDst.email;
        contoAccreditoRow.appendChild(utente);

        importoPRE = document.createElement("td");
        importoPRE.textContent = (trasferimento.contoDst.saldo - trasferimento.importo).toFixed(2);
        contoAccreditoRow.appendChild(importoPRE);

        importoPOST = document.createElement("td");
        importoPOST.textContent = trasferimento.contoDst.saldo;
        contoAccreditoRow.appendChild(importoPOST);

        // if the contact is new, create a button to add it to the contact list
        if(trasferimento.newContact && document.getElementById("addContactButton") === null){
            let addContactButton = document.createElement("button");
            addContactButton.id = "addContactButton";
            addContactButton.textContent = "Aggiungi contatto";
            addContactButton.classList.add("btn", "btn-primary", "mr-auto");
            addContactButton.addEventListener("click", () => {
                addContact(trasferimento.utenteDst);
            });
            let transactionResultModalFooter = document.getElementById("transactionResultModalFooter");
            transactionResultModalFooter.insertBefore(addContactButton, transactionResultModalFooter.firstChild);
        }
    }
}