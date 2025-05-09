package com.sap.cap.esmapi.utilities.srv.intf;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_SessionAttachment;

public interface IF_SessAttachmentsService
{
    // Initialize the Session Attachments container - Clear Messages too
    public void initialize();

    // Get all Attachments from SAC
    public List<TY_SessionAttachment> getAttachments();

    // Get Names of Attachments
    public List<String> getAttachmentNames();

    // Add Attachent from MultiPart Form Control
    public boolean addAttachment(MultipartFile file) throws EX_ESMAPI;

    // REmove Attachent by File Name
    public boolean removeAttachmentByName(String fileName);

    public boolean checkIFExists(String fileName);

    public void clearSessionMessages();

    // Get All Session messages
    public List<String> getSessionMessages();
}
