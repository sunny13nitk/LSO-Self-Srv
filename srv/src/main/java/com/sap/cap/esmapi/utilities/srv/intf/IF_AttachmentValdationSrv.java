package com.sap.cap.esmapi.utilities.srv.intf;

import org.springframework.web.multipart.MultipartFile;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_FlagMsg;

public interface IF_AttachmentValdationSrv
{
    public TY_FlagMsg isValidAttachmentByNameAndSize(MultipartFile attachment) throws EX_ESMAPI;
}
