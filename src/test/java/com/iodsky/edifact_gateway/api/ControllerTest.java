package com.iodsky.edifact_gateway.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Controller.class)
class ControllerTest {

    private static final String VALID_EDIFACT = "UNA:+.? 'UNB+UNOA:3+SENDER+RECEIVER+240816:1200+1'UNH+1+ORDERS:D:96A:UN'BGM+220+ORD001'UNT+3+1'UNZ+1+1'";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postParse_validEdifact_returns200() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content(VALID_EDIFACT))
                .andExpect(jsonPath("$.una").value("UNA:+.? '"))
                .andExpect(jsonPath("$.interchange.header.tag").value("UNB"))
                .andExpect(jsonPath("$.interchange.messages[0].header.tag").value("UNH"))
                .andExpect(jsonPath("$.interchange.messages[0].trailer.tag").value("UNT"))
                .andExpect(jsonPath("$.interchange.trailer.tag").value("UNZ"))
                .andExpect(status().isOk());
    }

    @Test
    void postParse_compositeIsNestedArray_simpleIsString() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content(VALID_EDIFACT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interchange.header.elements[0][0]").value("UNOA"))
                .andExpect(jsonPath("$.interchange.header.elements[0][1]").value("3"))
                .andExpect(jsonPath("$.interchange.header.elements[1]").value("SENDER"))
                .andExpect(jsonPath("$.interchange.messages[0].header.elements[1][0]").value("ORDERS"));
    }

    @Test
    void postParse_noUna_returnsNullUna() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content("UNB+UNOA:3+SENDER+RECEIVER+240816:1200+1'UNH+1+ORDERS:D:96A:UN'BGM+220+ORD001'UNT+3+1'UNZ+1+1'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.una").value(nullValue()));
    }

    @Test
    void postParse_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_EDIFACT))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void postParse_emptyBody_returns400_emptyInput() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("EMPTY_INPUT"))
                .andExpect(jsonPath("$.segmentIndex").value(nullValue()))
                .andExpect(jsonPath("$.elementIndex").value(nullValue()));
    }

    @Test
    void postParse_invalidUna_returns400() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content("UNA:+.?"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UNA"));
    }

    @Test
    void postParse_unclosedMessage_returns400() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content("UNB+1'UNH+1+ORDERS:D:96A:UN'BGM+220+ORD001'"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNCLOSED_MESSAGE"));
    }

    @Test
    void postParse_missingUnz_returns400() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content("UNB+1'UNH+1+ORDERS:D:96A:UN'BGM+220+ORD001'UNT+3+1'"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_UNZ"));
    }

    @Test
    void postParse_nestedUnh_returns400_withSegmentIndex() throws Exception {
        mockMvc.perform(post("/parse")
                .contentType(MediaType.TEXT_PLAIN)
                .content("UNB+1'UNH+1'UNH+2'"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNCLOSED_MESSAGE"))
                .andExpect(jsonPath("$.segmentIndex").value(3));
    }

}
