-- DROP SCHEMA IF EXISTS suncorp_test;
-- CREATE SCHEMA `suncorp_test` ;
-- USE suncorp_test;

--
-- Table structure for table `cash_type`
--

DROP TABLE IF EXISTS `cash_type`;
CREATE TABLE `cash_type` (
  `cash_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `cash_desc` varchar(45) DEFAULT NULL,
  `cash_value` int(11) NOT NULL,
  PRIMARY KEY (`cash_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `cash_supply`
--

DROP TABLE IF EXISTS `cash_supply`;
CREATE TABLE `cash_supply` (
  `cash_supply_id` int(11) NOT NULL AUTO_INCREMENT,
  `cash_type_id` int(11) NOT NULL,
  `cash_quantity` int(11) DEFAULT NULL,
  PRIMARY KEY (`cash_supply_id`),
  KEY `cash_type_idx` (`cash_type_id`),
  CONSTRAINT `cash_type` FOREIGN KEY (`cash_type_id`) REFERENCES `cash_type` (`cash_type_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `transaction_log`
--

DROP TABLE IF EXISTS `transaction_log`;
CREATE TABLE `transaction_log` (
  `transaction_log_id` int(11) NOT NULL AUTO_INCREMENT,
  `bsb` varchar(45) DEFAULT NULL,
  `account_no` varchar(45) DEFAULT NULL,
  `account_name` varchar(45) DEFAULT NULL,
  `total_amount` int(11) NOT NULL,
  `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transaction_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `transaction_log_detail`
--

DROP TABLE IF EXISTS `transaction_log_detail`;
CREATE TABLE `transaction_log_detail` (
  `transaction_log_detail_id` int(11) NOT NULL AUTO_INCREMENT,
  `transaction_log_id` int(11) NOT NULL,
  `cash_desc` varchar(45) NOT NULL,
  `cash_value` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  PRIMARY KEY (`transaction_log_detail_id`),
  KEY `transaction_log_idx` (`transaction_log_id`),
  CONSTRAINT `transaction_log` FOREIGN KEY (`transaction_log_id`) REFERENCES `transaction_log` (`transaction_log_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

