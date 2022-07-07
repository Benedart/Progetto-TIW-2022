create table utenti(
	IDUtente int unsigned auto_increment,
	Nome varchar(50) not null,
	Cognome varchar(50) not null,
	Email varchar(50) not null unique,
	Password varchar(255) not null,

	primary key(IDUtente)
);

create table rubrica(
    IDUtente int unsigned,
    IDUtenteSalvato int unsigned,

    primary key(IDUtente, IDUtenteSalvato),
    foreign key(IDUtente) references utenti(IDUtente)
                    on update cascade
                    on delete no action,
    foreign key(IDUtenteSalvato) references utenti(IDUtente)
                    on update cascade
                    on delete no action
);

create table conti(
	IDConto int unsigned auto_increment,
	IDUtente int unsigned not null,
	Saldo decimal(10,2),

	primary key(IDConto),
    foreign key(IDUtente) references utenti(IDUtente)
                            on delete no action
                            on update cascade
);

create table trasferimenti(
	IDContoSrc int unsigned not null,
    IDContoDst int unsigned not null,
	Data timestamp default CURRENT_TIMESTAMP,
	Importo decimal(8,2) check(Importo >= 0),
	Causale varchar(100),

	primary key(IDContoSrc, IDContoDst, Data),
	foreign key(IDContoDst) references conti(IDConto)
                                on delete no action
                                on update cascade,
    foreign key(IDContoSrc) references conti(IDConto)
                                on delete no action
                                on update cascade
);