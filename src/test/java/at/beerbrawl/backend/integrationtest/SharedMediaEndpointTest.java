/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.integrationtest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.config.properties.SecurityProperties;
import at.beerbrawl.backend.endpoint.dto.SharedMediaCreateDto;
import at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto;
import at.beerbrawl.backend.endpoint.dto.SharedMediaUpdateStateDto;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.enums.MediaState;
import at.beerbrawl.backend.repository.SharedMediaRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.security.JwtTokenizer;
import at.beerbrawl.backend.util.BeerDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SharedMediaEndpointTest extends TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SharedMediaRepository sharedMediaRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    private static final String SHARED_MEDIA_BASE_URI = "/api/v1/shared-media";

    @Test
    public void successfullyCreateNewSharedMedia() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        // Prepare the data
        SharedMediaCreateDto data = new SharedMediaCreateDto();
        data.setAuthor("Author 1");
        data.setTitle("Title 1");
        data.setTournamentId(tournament.getId());

        // Load a valid image file from resources
        byte[] imageBytes = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("testimage.png")
            .readAllBytes();

        MockMultipartFile imageFile = new MockMultipartFile(
            "image", // name of the file parameter
            "test-image.jpg", // original file name
            MediaType.IMAGE_JPEG_VALUE, // content type
            imageBytes // file content
        );

        MockMultipartFile jsonFile = new MockMultipartFile(
            "sharedMediaCreateDto", // name of the json parameter
            "", // no original file name
            MediaType.APPLICATION_JSON_VALUE, // content type
            objectMapper.writeValueAsBytes(data) // file content
        );

        var mvcResult =
            this.mockMvc.perform(
                    multipart(SHARED_MEDIA_BASE_URI)
                        .file(imageFile)
                        .file(jsonFile)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                )
                .andDo(print())
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            SharedMediaMetadataDto.class
        );

        assertAll(
            () -> assertEquals(HttpStatus.CREATED.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertEquals(dtoRes.author(), data.getAuthor()),
            () -> assertEquals(dtoRes.title(), data.getTitle()),
            () -> assertEquals(dtoRes.state(), MediaState.PENDING)
        );
    }

    private long createSharedMedia(
        Tournament tournament,
        String author,
        String title,
        String imagePath
    ) throws Exception {
        SharedMediaCreateDto data = new SharedMediaCreateDto();
        data.setAuthor(author);
        data.setTitle(title);
        data.setTournamentId(tournament.getId());

        byte[] imageBytes = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(imagePath)
            .readAllBytes();

        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "test-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            imageBytes
        );

        MockMultipartFile jsonFile = new MockMultipartFile(
            "sharedMediaCreateDto",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(data)
        );

        var mvcResult =
            this.mockMvc.perform(
                    multipart(SHARED_MEDIA_BASE_URI)
                        .file(imageFile)
                        .file(jsonFile)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                )
                .andDo(print())
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            SharedMediaMetadataDto.class
        );

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());

        return dtoRes.id();
    }

    @Test
    public void successfullyGetSharedMediaByTournament() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        // Create shared media entries
        createSharedMedia(tournament, "Author 1", "Title 1", "testimage.png");
        createSharedMedia(tournament, "Author 2", "Title 2", "testimage.png");
        createSharedMedia(tournament, "Author 3", "Title 3", "testimage.png");

        // Perform GET request to retrieve shared media metadata by tournament
        var mvcResult =
            this.mockMvc.perform(
                    get(
                        String.format("%s/tournament/%d", SHARED_MEDIA_BASE_URI, tournament.getId())
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();

        // Parse the response
        MockHttpServletResponse response = mvcResult.getResponse();
        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            SharedMediaMetadataDto[].class
        );

        // Assertions
        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertNotNull(dtoRes),
            () ->
                assertEquals(
                    3,
                    dtoRes.length,
                    "Expected to find 3 shared media but found " + dtoRes.length
                ),
            () -> assertEquals("Author 1", dtoRes[0].author()),
            () -> assertEquals("Title 1", dtoRes[0].title()),
            () -> assertEquals("Author 2", dtoRes[1].author()),
            () -> assertEquals("Title 2", dtoRes[1].title()),
            () -> assertEquals("Author 3", dtoRes[2].author()),
            () -> assertEquals("Title 3", dtoRes[2].title())
        );
    }

    @Test
    public void getSharedMediaByTournamentWhenNoneExist() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        var mvcResult =
            this.mockMvc.perform(
                    get(
                        String.format("%s/tournament/%d", SHARED_MEDIA_BASE_URI, tournament.getId())
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            SharedMediaMetadataDto[].class
        );

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertNotNull(dtoRes),
            () -> assertEquals(0, dtoRes.length, "Expected no shared media but found some")
        );
    }

    @Test
    public void unauthorizedGetSharedMediaByTournament() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        this.mockMvc.perform(
                get(String.format("%s/tournament/%d", SHARED_MEDIA_BASE_URI, tournament.getId()))
            )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void successfullyDeleteSharedMedia() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        // Create shared media with a real image
        createSharedMedia(tournament, "Author 1", "Title 1", "testimage.png");

        // Retrieve the shared media to get its ID
        var sharedMediaList = sharedMediaRepository.findAllByTournamentIdWithoutImage(
            tournament.getId()
        );
        assertEquals(1, sharedMediaList.size(), "Expected one shared media to be found");
        var sharedMedia = sharedMediaList.getFirst();

        // Perform DELETE request to delete the shared media
        this.mockMvc.perform(
                delete(String.format("%s/%d", SHARED_MEDIA_BASE_URI, sharedMedia.id()))
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isNoContent())
            .andReturn();

        // Verify the shared media has been deleted
        var deletedSharedMedia = sharedMediaRepository.findById(sharedMedia.id());
        assertTrue(
            deletedSharedMedia.isEmpty(),
            "Expected the shared media to be deleted, but it still exists"
        );
    }

    @Test
    public void deleteSharedMediaUnauthorized() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        // Create shared media with a real image
        var id = createSharedMedia(tournament, "Author 1", "Title 1", "testimage.png");

        this.mockMvc.perform(
                delete(String.format("%s/%d", SHARED_MEDIA_BASE_URI, id)).contentType(
                    MediaType.APPLICATION_JSON
                )
            )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void deleteNonExistingSharedMedia() throws Exception {
        long nonExistingSharedMediaId = -1L;

        var mvcResult =
            this.mockMvc.perform(
                    delete(String.format("%s/%d", SHARED_MEDIA_BASE_URI, nonExistingSharedMediaId))
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(
            HttpStatus.NOT_FOUND.value(),
            response.getStatus(),
            "Expected status 404 for non-existing shared media"
        );
    }

    @Test
    public void successfullyGetSharedMediaImage() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        // Create shared media with a real image
        long sharedMediaId = createSharedMedia(tournament, "Author 1", "Title 1", "testimage.png");

        // Perform GET request to retrieve the image
        var mvcResult =
            this.mockMvc.perform(
                    get(String.format("%s/image/%d", SHARED_MEDIA_BASE_URI, sharedMediaId)).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE))
                .andReturn();

        // Verify the image content
        MockHttpServletResponse response = mvcResult.getResponse();
        byte[] imageBytes = response.getContentAsByteArray();
        assertTrue(imageBytes.length > 0, "Expected to receive image bytes, but received none");
        // Additional verification if necessary, such as checking the image bytes match the original
    }

    @Test
    public void unauthorizedGetSharedMediaImage() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        // Create shared media with a real image
        long sharedMediaId = createSharedMedia(tournament, "Author 1", "Title 1", "testimage.png");

        this.mockMvc.perform(
                get(String.format("%s/image/%d", SHARED_MEDIA_BASE_URI, sharedMediaId))
            )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void setsStateToApproved() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "THIS IS A TEST",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.save(tournament);

        // Create shared media with a real image
        long sharedMediaId = createSharedMedia(tournament, "Author 1", "Title 1", "testimage.png");

        // Act
        this.mockMvc.perform(
                put(SHARED_MEDIA_BASE_URI + "/" + sharedMediaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new SharedMediaUpdateStateDto(MediaState.APPROVED)
                        )
                    )
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
            ).andExpect(status().isOk());

        // Retrieve shared media by tournament and filter by ID
        var mvcResult =
            this.mockMvc.perform(
                    get(SHARED_MEDIA_BASE_URI + "/tournament/" + tournament.getId()).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                ).andReturn();

        var response = mvcResult.getResponse();
        var dtoList = objectMapper.readValue(
            response.getContentAsString(),
            SharedMediaMetadataDto[].class
        );

        var updatedMedia = Arrays.stream(dtoList)
            .filter(dto -> dto.id().equals(sharedMediaId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Shared media not found"));

        assertEquals(MediaState.APPROVED, updatedMedia.state());
    }
}
