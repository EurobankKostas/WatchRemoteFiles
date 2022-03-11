package fileeventlistener.com.service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface FileEventListener {

    void triggerPolling() throws JSchException, SftpException, IOException, GeneralSecurityException;


}
