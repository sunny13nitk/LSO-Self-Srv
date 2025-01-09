package com.sap.cap.esmapi.utilities.srv.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_FlagMsg;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.srv.intf.IF_AttachmentValdationSrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_AttachmentValdationSrv implements IF_AttachmentValdationSrv
{

    private final TY_RLConfig rlConfig; // Autowired - Constructor Injection

    private final MessageSource msgSrc; // Autowired - Cons. Injection

    private final IF_UserSessionSrv userSessionSrv; // Autowired - Constructor Injection

    private final ApplicationEventPublisher applicationEventPublisher; // Autowired - Constructor Injection

    @Override
    public TY_FlagMsg isValidAttachmentByNameAndSize(MultipartFile attachment) throws EX_ESMAPI
    {
        TY_FlagMsg flagMsg = new TY_FlagMsg(false, null);
        if (rlConfig != null && attachment != null)
        {
            if (StringUtils.hasText(rlConfig.getAllowedAttachments())
                    && StringUtils.hasText(attachment.getOriginalFilename()))
            {
                flagMsg = isFileSizeWithinPermissibleLimits(attachment);
                if (flagMsg.isCheck())
                {

                    List<String> allowedAttachmentTypes = Arrays.asList(rlConfig.getAllowedAttachments().split("\\|"));
                    if (CollectionUtils.isNotEmpty(allowedAttachmentTypes))
                    {
                        // Get the Extension Type for Attachment
                        String filename = attachment.getOriginalFilename();
                        String[] fNameSplits = filename.split("\\.");

                        if (fNameSplits != null)
                        {
                            String extensionAttachment = null;
                            if (fNameSplits.length >= 1)
                            {
                                extensionAttachment = fNameSplits[fNameSplits.length - 1];
                            }
                            if (StringUtils.hasText(extensionAttachment))
                            {
                                String extnType = extensionAttachment;
                                Optional<String> extnfoundO = allowedAttachmentTypes.stream()
                                        .filter(a -> a.equalsIgnoreCase(extnType)).findFirst();
                                if (extnfoundO.isPresent())
                                {
                                    // Valid Attachment TYpe
                                    flagMsg.setCheck(true);
                                }
                                else
                                {
                                    if (userSessionSrv != null)
                                    {
                                        flagMsg.setCheck(false);
                                        flagMsg.setMsg(handleInvalidAttachment(filename, extnType));
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        return flagMsg;
    }

    private String handleInvalidAttachment(String filename, String extnType)
    {

        String msg;
        // msg = msgSrc.getMessage("ERR_INVALID_ATT_TYPE", new Object[]
        // { extnType, filename }, Locale.ENGLISH);

        msg = msgSrc.getMessage("ERR_INVALID_ATT_TYPE", new Object[]
        { extnType }, Locale.ENGLISH);

        log.error(msg);

        TY_Message logMsg = new TY_Message(userSessionSrv.getUserDetails4mSession().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ATTACHMENT,
                userSessionSrv.getUserDetails4mSession().getUserId(), msg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) userSessionSrv.getUserDetails4mSession().getUserId(),
                logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        return msg;
    }

    private TY_FlagMsg isFileSizeWithinPermissibleLimits(MultipartFile attachment)
    {

        TY_FlagMsg flagmsg = new TY_FlagMsg(true, null);

        if (!attachment.isEmpty() && rlConfig != null)
        {
            long filesizeinMB = attachment.getSize() / (1024 * 1024);
            if (filesizeinMB > rlConfig.getAllowedSizeAttachmentMB())
            {

                // ERR_ATTACHMENT_SIZE= Attachment size for file - {0} is {1}MB which is greater
                // than permissible limit of {2}MB!

                String msg;
                msg = msgSrc.getMessage("ERR_ATTACHMENT_SIZE", new Object[]
                { attachment.getOriginalFilename(), filesizeinMB, rlConfig.getAllowedSizeAttachmentMB() },
                        Locale.ENGLISH);

                log.error(msg);

                TY_Message logMsg = new TY_Message(userSessionSrv.getUserDetails4mSession().getUserId(),
                        Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ATTACHMENT_SIZE,
                        userSessionSrv.getUserDetails4mSession().getUserId(), msg);
                userSessionSrv.addMessagetoStack(logMsg);

                log.error(msg);

                // Instantiate and Fire the Event : Syncronous processing
                EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
                applicationEventPublisher.publishEvent(logMsgEvent);

                flagmsg.setCheck(false);
                flagmsg.setMsg(msg);
            }
        }

        return flagmsg;
    }

}
