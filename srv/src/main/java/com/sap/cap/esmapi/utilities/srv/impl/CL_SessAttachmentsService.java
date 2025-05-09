package com.sap.cap.esmapi.utilities.srv.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.multipart.MultipartFile;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_FlagMsg;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_SessAttContainer;
import com.sap.cap.esmapi.utilities.pojos.TY_SessionAttachment;
import com.sap.cap.esmapi.utilities.srv.intf.IF_AttachmentValdationSrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SessAttachmentsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@SessionScope
@RequiredArgsConstructor
@Slf4j
public class CL_SessAttachmentsService implements IF_SessAttachmentsService
{

    private final MessageSource msgSrc; // Autowired - Cons. Injection

    private final IF_AttachmentValdationSrv attVldSrv; // Autowired - Cons. Injection

    private TY_SessAttContainer SAC; // Session Attachments Container

    @Override
    public void initialize()
    {
        this.SAC = new TY_SessAttContainer(new ArrayList<TY_SessionAttachment>(), new ArrayList<String>());

    }

    @Override
    public boolean addAttachment(MultipartFile file) throws EX_ESMAPI
    {
        boolean uploaded = false;
        if (file != null && attVldSrv != null && !file.isEmpty())
        {

            // Validate the Attachment
            TY_FlagMsg flagMsg = attVldSrv.isValidAttachmentByNameAndSize(file);

            if (!flagMsg.isCheck())
            {

                log.error(flagMsg.getMsg());
                this.SAC.getMessages().add(flagMsg.getMsg());
            }

            else // Attachment Valid - Check for Duplicate
            {
                if (StringUtils.hasText(file.getOriginalFilename()))
                {
                    try
                    {
                        if (file.getBytes() != null)
                        {
                            // check for Duplicate Attachment - Already Exists
                            if (!checkIFExists(file.getOriginalFilename()))
                            {
                                // Add to Session
                                SAC.getAttachments()
                                        .add(new TY_SessionAttachment(file.getOriginalFilename(), file.getBytes()));
                                uploaded = true;
                            }
                            else
                            {
                                // DUPLICATE_ATTACHMENT= Attachment with Filename - {0} already exists. Not able
                                // to upload.
                                if (msgSrc != null)
                                {
                                    String msg = msgSrc.getMessage("DUPLICATE_ATTACHMENT", new Object[]
                                    { file.getOriginalFilename() }, Locale.ENGLISH);
                                    log.error(msg); // System Log
                                    SAC.getMessages().add(msg);
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        // ERR_ATT_BYTES= Error Accessing Binary Data from attachment - {0}, Details -
                        // {1}. Not able to upload.
                        if (msgSrc != null)
                        {
                            String msg = msgSrc.getMessage("ERR_ATT_BYTES", new Object[]
                            { file.getOriginalFilename(), e.getMessage() }, Locale.ENGLISH);
                            log.error(msg); // System Log
                            SAC.getMessages().add(msg);
                        }
                    }
                }

            }
        }

        return uploaded;
    }

    @Override
    public boolean removeAttachmentByName(String fileName)
    {
        boolean removed = false;
        if (StringUtils.hasText(fileName))
        {
            if (SAC != null && StringUtils.hasText(fileName))
            {
                if (CollectionUtils.isNotEmpty(SAC.getAttachments()))
                {
                    Optional<TY_SessionAttachment> sessAttO = SAC.getAttachments().stream()
                            .filter(a -> a.getName().equals(fileName)).findFirst();
                    if (sessAttO.isPresent())
                    {
                        SAC.getAttachments().remove(sessAttO.get());
                        removed = true;
                    }
                }
            }
        }

        return removed;
    }

    @Override
    public List<String> getSessionMessages()
    {
        return SAC.getMessages();
    }

    @Override
    public boolean checkIFExists(String fileName)
    {
        boolean attachmentExists = false;
        if (SAC != null && StringUtils.hasText(fileName))
        {
            if (CollectionUtils.isNotEmpty(SAC.getAttachments()))
            {
                Optional<TY_SessionAttachment> sessAttO = SAC.getAttachments().stream()
                        .filter(a -> a.getName().equals(fileName)).findFirst();
                if (sessAttO.isPresent())
                {
                    attachmentExists = true;
                }
            }
        }

        return attachmentExists;
    }

    @Override
    public void clearSessionMessages()
    {
        if (CollectionUtils.isNotEmpty(getSessionMessages()))
        {
            SAC.getMessages().clear();
        }
    }

    @Override
    public List<TY_SessionAttachment> getAttachments()
    {
        List<TY_SessionAttachment> attachments = null;
        if (this.SAC != null)
        {
            if (CollectionUtils.isNotEmpty(this.SAC.getAttachments()))
            {
                attachments = this.SAC.getAttachments();
            }
        }
        return attachments;
    }

    @Override
    public List<String> getAttachmentNames()
    {
        List<String> fileNames = Collections.emptyList();

        if (SAC != null)
        {
            fileNames = SAC.getAttachments().stream().map(e -> e.getName()).collect(Collectors.toList());
        }

        return fileNames;

    }

}
