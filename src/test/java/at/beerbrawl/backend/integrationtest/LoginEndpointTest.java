/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.LoginEndpoint;
import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Testing class for exercising the {@link LoginEndpoint} aka.
 * the {@code /register} REST endpoint.
 */
@SpringBootTest
@ActiveProfiles({ "test" })
@AutoConfigureMockMvc
public class LoginEndpointTest extends TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void loginWithCorrectCredentialsThatShouldSucceed() throws Exception {
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername(TestDataGenerator.TEST_USER)
            .withPassword(TestDataGenerator.TEST_USER_PASSWORD)
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        assertThat(response.getContentAsString())
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(1)
            .startsWith("Bearer ");
    }

    @Test
    public void loginWithIncorrectUsernameShouldYield403Forbidden() throws Exception {
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("foo")
            .withPassword(TestDataGenerator.TEST_USER_PASSWORD)
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse();

        assertThat(response.getContentAsString())
            .isNotNull()
            .isEqualTo("Bad credentials: Username or password incorrect");
    }

    @Test
    public void loginWithIncorrectPasswordShouldYield403Forbidden() throws Exception {
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername(TestDataGenerator.TEST_USER)
            .withPassword("12345678")
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse();

        assertThat(response.getContentAsString())
            .isNotNull()
            .isEqualTo("Bad credentials: Username or password incorrect");
    }
}
