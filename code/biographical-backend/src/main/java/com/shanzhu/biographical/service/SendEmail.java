package com.shanzhu.biographical.service;

import com.shanzhu.biographical.dto.EmailDTO;


public interface SendEmail {
    public void sendMsg(EmailDTO emailDTO);
}
