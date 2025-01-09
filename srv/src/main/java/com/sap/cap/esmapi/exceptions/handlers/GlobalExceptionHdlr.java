package com.sap.cap.esmapi.exceptions.handlers;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.exceptions.EX_SessionExpired;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHdlr
{

	@Autowired
	private IF_UserSessionSrv userSessionSrv;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@ExceptionHandler(EX_ESMAPI.class)
	public ModelAndView handleNotFound(Exception ex)
	{
		ModelAndView mv = new ModelAndView();
		mv.setViewName("error");
		log.error("Exception of Type 'EX_ESMAPI' occured with error details as : " + ex.getLocalizedMessage());
		log.error(ex.getStackTrace().toString());
		mv.addObject("formError", ex.getMessage());
		return mv;
	}

	@ExceptionHandler(EX_SessionExpired.class)
	public ModelAndView handleSessionExpired(Exception ex)
	{
		ModelAndView mv = new ModelAndView();
		mv.setViewName("sessionexpire");
		log.error("Exception of Type 'EX_SessionExpired' occured with error details as : " + ex.getLocalizedMessage());
		log.error(ex.getStackTrace().toString());
		return mv;
	}

	@ExceptionHandler(SizeLimitExceededException.class)
	public void handleMaxUploadSizeExceededException(Exception ex)
	{

		// Logging Framework
		TY_Message logMsg = new TY_Message(userSessionSrv.getUserDetails4mSession().getUserId(),
				Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ATTACHMENT_SIZE,
				userSessionSrv.getUserDetails4mSession().getUserId(), ex.getMessage());
		userSessionSrv.addMessagetoStack(logMsg);

		log.error(ex.getLocalizedMessage());

		// Instantiate and Fire the Event : Syncronous processing
		EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
		applicationEventPublisher.publishEvent(logMsgEvent);

	}

}