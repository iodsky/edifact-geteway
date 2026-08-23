package com.iodsky.edifact_gateway.api;

import com.iodsky.edifact_gateway.edifact.Parser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("parse")
public class Controller {

    @PostMapping(consumes = "text/plain")
    public DocumentDto parse(@RequestBody(required = false) String body) {
        return DocumentDto.from(Parser.parse(body));
    }

}
