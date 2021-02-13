package org.airsonic.player.security;

import org.airsonic.player.util.HomeRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RESTAuthTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String API_VERSION = "1.15.0";

    @ClassRule
    public static final HomeRule homeRule = new HomeRule();

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mvc;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .dispatchOptions(true)
                .alwaysDo(print())
                .build();
    }

    @Test
    public void testRequestParamAuthSuccess() throws Exception {
        mvc.perform(get("/rest/getArtists")
                .param("v", API_VERSION)
                .param("f", "json")
                .param("c", "test")
                .param("u", USERNAME)
                .param("p", PASSWORD)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"));
    }

    @Test
    public void testRequestParamAuthFailure() throws Exception {
        mvc.perform(get("/rest/getArtists")
                .param("v", API_VERSION)
                .param("c", "test")
                .param("f", "json")
                .param("u", USERNAME)
                .param("p", "incorrectpassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.error.code").value(40));
    }

    @Test
    public void testBasicAuthSuccess() throws Exception {
        mvc.perform(get("/rest/getArtists")
                .param("v", API_VERSION)
                .param("c", "test")
                .param("f", "json")
                .with(httpBasic(USERNAME, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"));
    }

    @Test
    public void testBasicAuthFailure() throws Exception {
        mvc.perform(get("/rest/getArtists")
                .param("v", API_VERSION)
                .param("c", "test")
                .param("f", "json")
                .with(httpBasic(USERNAME, "incorrectpassword"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

}
