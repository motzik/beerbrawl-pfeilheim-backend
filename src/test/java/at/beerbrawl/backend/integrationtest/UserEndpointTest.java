/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.UserEndpoint;
import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Testing class for exercising the {@link UserEndpoint} aka. the
 * {@code /api/v1/user} REST endpoint.
 */
@SpringBootTest
@ActiveProfiles({ "test" })
@AutoConfigureMockMvc
public class UserEndpointTest extends TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void tryRegisterNewAccountAndTryAuthenticateWithIt() throws Exception {
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("foo")
            .withPassword("12345678")
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();

        assertThat(response.getHeader("Location")).isNotNull().startsWith("/api/v1/user/");
        assertThat(response.getContentAsString())
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(1)
            .startsWith("Bearer ");

        var body = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(body).isNotNull().hasSizeGreaterThanOrEqualTo(1).startsWith("Bearer ");
    }

    @Test
    public void deleteUser_SuccessfulDeletion_ReturnsNoContent() throws Exception {
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("foo")
            .withPassword("12345678")
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();

        var bearer_token = response.getContentAsString();

        mockMvc
            .perform(
                delete("/api/v1/user/{username}", dto.getUsername())
                    .header("Authorization", bearer_token)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNoContent());
    }

    @Test
    public void deleteUser_UnsuccessfulDeletion_ReturnsForbidden() throws Exception {
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("foo")
            .withPassword("12345678")
            .build();

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();

        var dto2 = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("faa")
            .withPassword("12341234")
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto2))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();

        var wrong_token = response.getContentAsString();
        mockMvc
            .perform(
                delete("/api/v1/user/{username}", dto.getUsername())
                    .header("Authorization", wrong_token)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isForbidden());
    }

    @Test
    public void deletingOtherUserAccountShouldFail() throws Exception {
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("foo")
            .withPassword("12345678")
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();

        assertThat(response.getHeader("Location")).isNotNull().startsWith("/api/v1/user/");

        var authToken = response.getContentAsString();
        assertThat(authToken).isNotNull().hasSizeGreaterThanOrEqualTo(1).startsWith("Bearer ");

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(
                    "/api/v1/user/{username}",
                    TestDataGenerator.TEST_USER
                )
                    .header(HttpHeaders.AUTHORIZATION, authToken)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isForbidden());
    }

    @Test
    public void deletingOwnUserAccountShouldSucceed() throws Exception {
        var authToken = this.loginAndGetAuthorizationToken();

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(
                    "/api/v1/user/{username}",
                    TestDataGenerator.TEST_USER
                )
                    .header(HttpHeaders.AUTHORIZATION, authToken)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNoContent());
    }

    private String loginAndGetAuthorizationToken() throws Exception {
        var loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername(TestDataGenerator.TEST_USER)
            .withPassword(TestDataGenerator.TEST_USER_PASSWORD)
            .build();

        var body = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(loginDto))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(body).isNotNull().hasSizeGreaterThanOrEqualTo(1).startsWith("Bearer ");

        return body;
    }
}
