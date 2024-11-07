/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public class UserLoginDto {

    @NotNull(message = "Username must not be null")
    @Size(min = 3, max = 256)
    private String username;

    @NotNull(message = "Password must not be null")
    @Size(min = 8, max = 1024)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserLoginDto userLoginDto)) {
            return false;
        }
        return (
            Objects.equals(username, userLoginDto.username) &&
            Objects.equals(password, userLoginDto.password)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        return (
            "UserLoginDto{" +
            "username='" +
            username +
            '\'' +
            ", password='" +
            password +
            '\'' +
            '}'
        );
    }

    public static final class UserLoginDtoBuilder {

        private String username;
        private String password;

        private UserLoginDtoBuilder() {}

        public static UserLoginDtoBuilder anUserLoginDto() {
            return new UserLoginDtoBuilder();
        }

        public UserLoginDtoBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public UserLoginDtoBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserLoginDto build() {
            UserLoginDto userLoginDto = new UserLoginDto();
            userLoginDto.setUsername(username);
            userLoginDto.setPassword(password);
            return userLoginDto;
        }
    }
}
