-- ----------------------------------------------
-- Metrix Server
-- Netherlands Cancer Institute
-- Bernd van der Veen - 2013
-- For license details please see LICENSE.TXT
-- SQL Script for Metrix Server object table
-- Database Language Type: MySQL
-- Please change the database name if necessary.
-- ----------------------------------------------

DROP DATABASE IF EXISTS `metrix`;
CREATE DATABASE `metrix`;
USE `metrix`;
DROP TABLE IF EXISTS `metrix_objects`;
CREATE TABLE `metrix`.`metrix_objects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `run_id` varchar(512) DEFAULT NULL,
  `object_value` longblob,
  `state` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

