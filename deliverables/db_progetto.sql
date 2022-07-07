CREATE DATABASE  IF NOT EXISTS `moneytransfer_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `moneytransfer_db`;
-- MySQL dump 10.13  Distrib 8.0.28, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: moneytransfer_db
-- ------------------------------------------------------
-- Server version	8.0.28

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `conti`
--

DROP TABLE IF EXISTS `conti`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conti` (
  `IDConto` int unsigned NOT NULL AUTO_INCREMENT,
  `IDUtente` int unsigned NOT NULL,
  `Saldo` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`IDConto`),
  KEY `IDUtente` (`IDUtente`),
  CONSTRAINT `conti_ibfk_1` FOREIGN KEY (`IDUtente`) REFERENCES `utenti` (`IDUtente`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conti`
--

LOCK TABLES `conti` WRITE;
/*!40000 ALTER TABLE `conti` DISABLE KEYS */;
INSERT INTO `conti` VALUES (1,1,1600),(2,1,2000),(3,2,400),(4,3,1000),(5,4,0);
/*!40000 ALTER TABLE `conti` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trasferimenti`
--

DROP TABLE IF EXISTS `trasferimenti`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trasferimenti` (
  `IDContoSrc` int unsigned NOT NULL,
  `IDContoDst` int unsigned NOT NULL,
  `Data` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Importo` decimal(8,2) DEFAULT NULL,
  `Causale` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`IDContoSrc`,`IDContoDst`,`Data`),
  KEY `IDContoDst` (`IDContoDst`),
  CONSTRAINT `trasferimenti_ibfk_1` FOREIGN KEY (`IDContoDst`) REFERENCES `conti` (`IDConto`) ON UPDATE CASCADE,
  CONSTRAINT `trasferimenti_ibfk_2` FOREIGN KEY (`IDContoSrc`) REFERENCES `conti` (`IDConto`) ON UPDATE CASCADE,
  CONSTRAINT `trasferimenti_chk_1` CHECK ((`Importo` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trasferimenti`
--

LOCK TABLES `trasferimenti` WRITE;
/*!40000 ALTER TABLE `trasferimenti` DISABLE KEYS */;
INSERT INTO `trasferimenti` VALUES (1,2,'2022-04-11 18:14:16',200,'ricarica pagina'),(1,2,'2022-04-11 18:18:48',325,''),(1,2,'2022-04-11 18:21:48',20,'ricarica errore'),(1,2,'2022-04-11 18:45:20',55,'55'),(1,3,'2022-04-11 18:12:41',300,'prova'),(2,1,'2022-04-13 10:39:07',200,'Trasferimento interno'),(3,1,'2022-04-11 18:25:23',1300,'prova'),(3,5,'2022-04-13 10:35:21',200,'%&$£'),(5,2,'2022-04-13 10:37:22',600,'Prova quarto - rossi (2)'),(5,3,'2022-04-13 10:30:35',200,'Prova quarto - verdi'),(5,3,'2022-04-13 10:34:08',400,'');
/*!40000 ALTER TABLE `trasferimenti` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `utenti`
--

DROP TABLE IF EXISTS `utenti`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utenti` (
  `IDUtente` int unsigned NOT NULL AUTO_INCREMENT,
  `Nome` varchar(50) NOT NULL,
  `Cognome` varchar(50) NOT NULL,
  `Email` varchar(50) NOT NULL,
  `Password` varchar(255) NOT NULL,
  PRIMARY KEY (`IDUtente`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `utenti`
--

LOCK TABLES `utenti` WRITE;
/*!40000 ALTER TABLE `utenti` DISABLE KEYS */;
INSERT INTO `utenti` VALUES (1,'mario','rossi','mariorossi@mail.com','a'),(2,'luigi','verdi','luigiverdi@mail.com','b'),(3,'Bob','Gialli','bobgialli@mail.com','c'),(4,'Quarto','Quarti','quartoquarti@mail.com','d');
/*!40000 ALTER TABLE `utenti` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'moneytransfer_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-04-15 22:09:07
