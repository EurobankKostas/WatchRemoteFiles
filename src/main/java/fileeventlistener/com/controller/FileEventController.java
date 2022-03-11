package fileeventlistener.com.controller;

import com.jcraft.jsch.*;
import fileeventlistener.com.service.FileEventListener;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.Vector;

@RestController
@RequestMapping("/fileEvent/v1")
public class FileEventController {

    @Autowired
    private FileEventListener fileEventListener;

    @PostMapping("/trigger")
    public ResponseEntity<Void> triggerPolling() throws JSchException, SftpException, IOException, GeneralSecurityException {
        fileEventListener.triggerPolling();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/consume")
    public ResponseEntity<Void> consume() throws JSchException, SftpException, IOException, GeneralSecurityException {
        fileEventListener.triggerPolling();
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
