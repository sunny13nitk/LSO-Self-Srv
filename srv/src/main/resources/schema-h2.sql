
DROP VIEW IF EXISTS LogsReadService_Logs;
DROP TABLE IF EXISTS db_esmlogs_esmappmsglog;

CREATE TABLE db_esmlogs_esmappmsglog (
  ID NVARCHAR(36) NOT NULL,
  username NVARCHAR(50),
  timestamp TIMESTAMP(7),
  status NVARCHAR(50),
  msgtype NVARCHAR(50),
  objectid NVARCHAR(50),
  message NVARCHAR(1000),
  PRIMARY KEY(ID)
); 

CREATE VIEW LogsReadService_Logs AS SELECT
  esmappmsglog_0.ID,
  esmappmsglog_0.username,
  esmappmsglog_0.timestamp,
  esmappmsglog_0.status,
  esmappmsglog_0.msgtype,
  esmappmsglog_0.objectid,
  esmappmsglog_0.message
FROM db_esmlogs_esmappmsglog AS esmappmsglog_0; 

