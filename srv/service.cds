using {db.esmlogs as logs } from '../db/model/esmlogs'; 

service LogsReadService  
{
    @readonly entity Logs as projection on logs.esmappmsglog;
}


//annotate LogsReadService @(requires :'ESMAPI Administrators') ;
